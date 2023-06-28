package ricardo_franco;

import java.util.ArrayList;
import java.util.List;

import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.SwingPoint;
import com.motivewave.platform.sdk.common.Util;


public class TrendManager {
    FibonacciStrategy study;
    SwingManager swingManager;

    boolean onWave2;
    int wave2Index;
    String currentTrend;
    List<SwingPoint> changeOfTrendSwings;

    SwingPoint currentSwing1;
    SwingPoint currentSwing2;
    double currentDiff;

    double currentRetraction;
    double maxRetraction;
    double retraction50;
    double retraction618;
    boolean validRetraction;
    boolean reachedZone;
    boolean invalidatedZone;

    public TrendManager(FibonacciStrategy study, SwingManager swingManager) {
        this.study = study;
        this.swingManager = swingManager;

        this.onWave2 = false;
        this.wave2Index = 0;
        this.currentTrend = null;
        this.changeOfTrendSwings = new ArrayList<SwingPoint>();

        this.currentSwing1 = null;
        this.currentSwing2 = null;
        this.currentDiff = 0;

        this.currentRetraction = 0;
        this.maxRetraction = 0;
        this.retraction50 = 0;
        this.retraction618 = 0;
        this.validRetraction = false;
        this.reachedZone = false;
        this.invalidatedZone = false;
    }

    public void update(DataSeries series) {
        this.computeTrend();
        this.computeRetraction(series);
    }

    public void clear() {
        this.changeOfTrendSwings.clear();
    }

    //----------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------

    protected void computeTrend() {
        currentTrend = null;
        onWave2 = false;

        SwingPoint lastSwingHigh = null;
        SwingPoint lastSwingLow = null;
        SwingPoint highestSwingHigh = null;
        SwingPoint lowestSwingLow = null;
        SwingPoint leadingSwingHigh = null;
        SwingPoint leadingSwingLow = null;

        for (SwingPoint swing : this.swingManager.swings) {
            if (currentTrend == null) {
                if (swing.isTop()) {
                    currentTrend = "down";
                } else {
                    currentTrend = "up";
                }
            } else {
                if (currentTrend == "up") {
                    if (swing.isTop()) {
                        if (highestSwingHigh == null || (swing.getValue() > highestSwingHigh.getValue())) {
                            highestSwingHigh = swing;
                            leadingSwingLow = lastSwingLow;
                        }
                    } else {
                        if (leadingSwingLow != null) {
                            if (swing.getValue() < leadingSwingLow.getValue()) {
                                lowestSwingLow = swing;
                                leadingSwingLow = null;
                                leadingSwingHigh = lastSwingHigh;

                                currentTrend = "down";
                                this.confirmWave2(swing);
                                this.changeOfTrendSwings.add(swing);
                            }
                        }
                    }
                } else if (currentTrend == "down") {
                    if (swing.isBottom()) {
                        if (lowestSwingLow == null || (swing.getValue() < lowestSwingLow.getValue())) {
                            lowestSwingLow = swing;
                            leadingSwingHigh = lastSwingHigh;
                        }
                    } else {
                        if (leadingSwingHigh != null) {
                            if (swing.getValue() > leadingSwingHigh.getValue()) {
                                highestSwingHigh = swing;
                                leadingSwingHigh = null;
                                leadingSwingLow = lastSwingLow;

                                currentTrend = "up";
                                this.confirmWave2(swing);
                                this.changeOfTrendSwings.add(swing);
                            }
                        }
                    }
                }

                // TODO trading all swings
                this.checkWave(swing, lastSwingLow, lastSwingHigh);
            }

            if (swing.isTop())
                lastSwingHigh = swing;
            else
                lastSwingLow = swing;
        }

        this.study.debug("Current Trend: " + currentTrend);
    }

    protected void computeRetraction(DataSeries series) {
        this.validRetraction = false;

        if (!this.onWave2) return;

        if (this.swingManager.swings.size() < 2) {
            this.study.debug("Not enough swing points to compute retraction");
            return;
        }

        SwingPoint swing1 = this.swingManager.swings.get(this.swingManager.swings.size() - 2);
        SwingPoint swing2 = this.swingManager.swings.get(this.swingManager.swings.size() - 1);

        if (swing1 == null || swing2 == null) {
            this.study.debug("Not enough swing points to compute retraction");
            return;
        }

        this.validRetraction = true;
        this.currentSwing1 = swing1;
        this.currentSwing2 = swing2;

        if (swing2.isTop()) {
            double diff = swing2.getValue() - swing1.getValue();
            this.currentDiff = diff;

            this.retraction50 = diff * 0.5f + swing1.getValue();
            this.retraction618 = diff * (1.0f - 0.618f) + swing1.getValue();
            this.currentRetraction = (swing2.getValue() - series.getClose()) * 100.0f / diff;

            double lowest = series.getLow();
            for (int i = series.size()-2; i > swing2.getIndex(); i--) {
                lowest = Util.min(lowest, series.getLow(i));
            }
            this.maxRetraction = (swing2.getValue() - lowest) * 100.0f / diff;
        } else {
            double diff = swing1.getValue() - swing2.getValue();
            this.currentDiff = diff;

            this.retraction50 = diff * 0.5f + swing2.getValue();
            this.retraction618 = diff * (1.0f - 0.618f) + swing2.getValue();
            this.currentRetraction = (series.getClose() - swing2.getValue()) * 100.0f / diff;

            double highest = series.getHigh();
            for (int i = series.size()-2; i > swing2.getIndex(); i--) {
                highest = Util.max(highest, series.getHigh(i));
            }
            this.maxRetraction = (highest - swing2.getValue()) * 100.0f / diff;
        }

        this.study.debug(String.format("Swing 1: %.5f", swing1.getValue()));
        this.study.debug(String.format("Swing 2: %.5f", swing2.getValue()));
        this.study.debug(String.format("Retraction: %.2f%%", this.currentRetraction));
        this.study.debug(String.format("Max Retraction: %.2f%%", this.maxRetraction));
        this.study.debug(String.format("Retraction 50%%: %.5f", this.retraction50));
        this.study.debug(String.format("Retraction 61.8%%: %.5f", this.retraction618));
    }

    protected void confirmWave2(SwingPoint swing) {
        this.onWave2 = true;
        this.wave2Index = swing.getIndex();
        this.reachedZone = false;
        this.invalidatedZone = false;

//        this.study.debug(String.format("Wave 2 confirmed on index #%d", this.wave2Index));
    }

    protected void checkWave(SwingPoint swing, SwingPoint lastSwingLow, SwingPoint lastSwingHigh) {
        if (swing.isTop()) {
            if (lastSwingHigh == null) return;

            if (swing.getValue() > lastSwingHigh.getValue()) {
                this.confirmWave2(swing);
            }
        } else {
            if (lastSwingLow == null) return;

            if (swing.getValue() < lastSwingLow.getValue()) {
                this.confirmWave2(swing);
            }
        }
    }
}
