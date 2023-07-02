package ricardo_franco;

import java.util.ArrayList;
import java.util.List;

import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.SwingPoint;
import com.motivewave.platform.sdk.common.Util;


public class TrendManager {
    FibonacciStrategy study;
    SwingManager swingManager;
    SwingManager ltfSwingManager;

    boolean onWave2;
    int wave2Index;
    String currentTrend;
    String currentTrendForTrading;
    List<SwingPoint> changeOfTrendSwings;

    SwingPoint swing1;
    SwingPoint swing2;
    double currentDiff;

    double currentRetraction;
    double maxRetraction;
    double retraction50;
    double retraction618;
    boolean validRetraction;

    public TrendManager(FibonacciStrategy study, SwingManager swingManager, SwingManager ltfSwingManager) {
        this.study = study;
        this.swingManager = swingManager;
        this.ltfSwingManager = ltfSwingManager;

        this.onWave2 = false;
        this.wave2Index = 0;
        this.currentTrend = null;
        this.currentTrendForTrading = null;
        this.changeOfTrendSwings = new ArrayList<SwingPoint>();

        this.swing1 = null;
        this.swing2 = null;
        this.currentDiff = 0;

        this.currentRetraction = 0;
        this.maxRetraction = 0;
        this.retraction50 = 0;
        this.retraction618 = 0;
        this.validRetraction = false;
    }

    public void update(DataSeries series) {
        this.computeTrend();
        this.computeRetraction(series);
    }

    public void clear() {
        this.changeOfTrendSwings.clear();
    }

    public double priceForRetraction(float percentage, SwingPoint swing1, SwingPoint swing2) {
        if (swing2.isTop()) {
            return swing1.getValue() + this.currentDiff * (1.0f - percentage/100.0f);
        } else {
            return swing1.getValue() - this.currentDiff * (1.0f - percentage/100.0f);
        }
    }

    //----------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------

    protected void computeTrend() {
        currentTrend = null;
        currentTrendForTrading = null;
        onWave2 = false;

        this.swing1 = null;
        this.swing2 = null;

        SwingPoint lastSwingHigh = null;
        SwingPoint lastSwingLow = null;
        SwingPoint highestSwingHigh = null;
        SwingPoint lowestSwingLow = null;
        SwingPoint leadingSwingHigh = null;
        SwingPoint leadingSwingLow = null;

        for (SwingPoint swing : this.swingManager.swings) {
            if (currentTrend == null) {
                if (swing.isTop()) {
                    currentTrend = currentTrendForTrading = "down";
                } else {
                    currentTrend = currentTrendForTrading = "up";
                }
            } else {
                if (currentTrend == "up") {
                    if (swing.isTop()) {
                        if (highestSwingHigh == null || (swing.getValue() > highestSwingHigh.getValue())) {
                            highestSwingHigh = swing;
                            leadingSwingLow = lastSwingLow;
                        }
                    } else {
                        this.onWave2 = false;

                        if (leadingSwingLow != null) {
                            if (swing.getValue() < leadingSwingLow.getValue()) {
                                lowestSwingLow = swing;
                                leadingSwingLow = null;
                                leadingSwingHigh = lastSwingHigh;

                                currentTrend = currentTrendForTrading = "down";
                                this.confirmWave2(leadingSwingHigh, swing);
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
                        this.onWave2 = false;

                        if (leadingSwingHigh != null) {
                            if (swing.getValue() > leadingSwingHigh.getValue()) {
                                highestSwingHigh = swing;
                                leadingSwingHigh = null;
                                leadingSwingLow = lastSwingLow;

                                currentTrend = currentTrendForTrading = "up";
                                this.confirmWave2(leadingSwingLow, swing);
                                this.changeOfTrendSwings.add(swing);
                            }
                        }
                    }
                }

                // trading all swings (remove?)
                // this.checkWave(swing, lastSwingLow, lastSwingHigh);
            }

            if (swing.isTop())
                lastSwingHigh = swing;
            else
                lastSwingLow = swing;
        }

        if (currentTrend != null) {
            for (SwingPoint swing : this.ltfSwingManager.swings) {
                if (swing.getIndex() > lastSwingHigh.getIndex() && swing.getIndex() > lastSwingLow.getIndex()) {
                    if (currentTrend == "up") {
                        if (swing.isBottom()) {
                            if (leadingSwingLow != null) {
                                if (swing.getValue() < leadingSwingLow.getValue()) {
                                    this.currentTrendForTrading = "down";
                                    this.confirmWave2(lastSwingHigh, swing);

                                    // TODO swing2 = swing
                                    // TODO swing1 = lastSwingHigh
                                }
                            }
                        }
                    } else if (currentTrend == "down") {
                        if (swing.isTop()) {
                            if (leadingSwingHigh != null) {
                                if (swing.getValue() > leadingSwingHigh.getValue()) {
                                    this.currentTrendForTrading = "up";
                                    this.confirmWave2(lastSwingLow, swing);

                                    // TODO swing2 = swing
                                    // TODO swing1 = lastSwingLow
                                }
                            }
                        }
                    }
                }

            }
        }

        // this.study.debug("Current Trend: " + currentTrend);
    }

    protected void computeRetraction(DataSeries series) {
        this.validRetraction = false;

        if (!this.onWave2) return;

        if (this.swingManager.swings.size() < 2) {
            this.study.debug("Not enough swing points to compute retraction");
            return;
        }

        // TODO remove
        // SwingPoint swing1 = this.getSwing1();
        // SwingPoint swing2 = this.getSwing2();

        if (swing1 == null || swing2 == null) {
            this.study.debug("Not enough swing points to compute retraction");
            return;
        }

        this.validRetraction = true;

        if (swing2.isTop()) {
            double diff = swing2.getValue() - swing1.getValue();
            this.currentDiff = diff;

            this.retraction50 = this.priceForRetraction(50.0f, swing1, swing2);
            this.retraction618 = this.priceForRetraction(61.8f, swing1, swing2);
            this.currentRetraction = (swing2.getValue() - series.getClose()) * 100.0f / diff;

            double lowest = series.getLow();
            for (int i = series.size()-2; i > swing2.getIndex(); i--) {
                lowest = Util.min(lowest, series.getLow(i));
            }
            this.maxRetraction = (swing2.getValue() - lowest) * 100.0f / diff;
        } else {
            double diff = swing1.getValue() - swing2.getValue();
            this.currentDiff = diff;

            this.retraction50 = this.priceForRetraction(50.0f, swing1, swing2);
            this.retraction618 = this.priceForRetraction(61.8f, swing1, swing2);
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

    protected void confirmWave2(SwingPoint swing1, SwingPoint swing2) {
        this.onWave2 = true;
        this.wave2Index = swing2.getIndex();

        this.swing1 = swing1;
        this.swing2 = swing2;

        // this.study.debug(String.format("Wave 2 confirmed on index #%d", this.wave2Index));
    }

//    protected SwingPoint getSwing1() {
//        return this.swingManager.swings.get(this.swingManager.swings.size() - 2);
//    }
//
//    protected SwingPoint getSwing2() {
//        return this.swingManager.swings.get(this.swingManager.swings.size() - 1);
//    }

    /*
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
    */
}
