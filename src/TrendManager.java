package ricardo_franco;

import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.SwingPoint;
import com.motivewave.platform.sdk.common.Util;


public class TrendManager {
    FibonacciStrategy study;

    boolean onWave2;
    int wave2Index;
    String currentTrend;

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

    public TrendManager(FibonacciStrategy study) {
        this.study = study;

        this.onWave2 = false;
        this.wave2Index = 0;
        this.currentTrend = null;

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
        this.computeTrend(true);
        this.computeTrend(false);

        this.computeRetraction(series);
    }

    //----------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------

    protected void computeTrend(boolean ltfTrend) {
        currentTrend = null;

        SwingPoint lastSwingHigh = null;
        SwingPoint lastSwingLow = null;
        SwingPoint highestSwingHigh = null;
        SwingPoint lowestSwingLow = null;
        SwingPoint leadingSwingHigh = null;
        SwingPoint leadingSwingLow = null;

        for (SwingPoint swing : this.study.swingManager.swingsLTF) {
            if (!ltfTrend) {
                if (!this.study.swingManager.swingsTTFKeys.contains(swing.getIndex())) continue;

                onWave2 = false;
            }

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
                                if (ltfTrend) {
                                    if (highestSwingHigh != null) {
                                        this.study.swingManager.swingsTTFKeys.add(highestSwingHigh.getIndex());
                                    }
                                }

                                lowestSwingLow = swing;
                                leadingSwingLow = null;
                                leadingSwingHigh = lastSwingHigh;

                                currentTrend = "down";
                                if (!ltfTrend) {
                                    this.confirmWave2(swing);
                                }
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
                                if (ltfTrend) {
                                    if (lowestSwingLow != null) {
                                        this.study.swingManager.swingsTTFKeys.add(lowestSwingLow.getIndex());
                                    }
                                }

                                highestSwingHigh = swing;
                                leadingSwingHigh = null;
                                leadingSwingLow = lastSwingLow;

                                currentTrend = "up";
                                if (!ltfTrend) {
                                    this.confirmWave2(swing);
                                }
                            }
                        }
                    }
                }

                // TODO trading all swings
                this.checkWave(ltfTrend, swing, lastSwingLow, lastSwingHigh);
            }

            if (swing.isTop())
                lastSwingHigh = swing;
            else
                lastSwingLow = swing;
        }
    }

    protected void computeRetraction(DataSeries series) {
        this.validRetraction = false;

        if (!this.onWave2) return;

        if (this.study.swingManager.swingsTTFKeys.size() < 2) {
            this.study.debug("Not enough swing points to compute retraction");
            return;
        }

        int swing1Index = this.study.swingManager.swingsTTFKeys.get(this.study.swingManager.swingsTTFKeys.size() - 2);
        int swing2Index = this.study.swingManager.swingsTTFKeys.get(this.study.swingManager.swingsTTFKeys.size() - 1);

        SwingPoint swing1 = null;
        SwingPoint swing2 = null;

        for (SwingPoint swing : this.study.swingManager.swingsLTF) {
            if (swing.getIndex() == swing1Index) swing1 = swing;
            if (swing.getIndex() == swing2Index) swing2 = swing;

            if (swing1 != null & swing2 != null) break;
        }

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
            this.retraction618 = diff * 0.618f + swing1.getValue();
            this.currentRetraction = (swing2.getValue() - series.getClose()) * 100.0f / diff;

            double lowestSwing = 9999.0f;
            for (SwingPoint swing : this.study.swingManager.swingsLTF) {
                if (swing.getIndex() <= swing2Index) continue;

                lowestSwing = Util.min(lowestSwing, swing.getValue());
            }
            this.maxRetraction = (swing2.getValue() - lowestSwing) * 100.0f / diff;
        } else {
            double diff = swing1.getValue() - swing2.getValue();
            this.currentDiff = diff;

            this.retraction50 = diff * 0.5f + swing2.getValue();
            this.retraction618 = diff * 0.618f + swing2.getValue();
            this.currentRetraction = (series.getClose() - swing2.getValue()) * 100.0f / diff;

            double highestSwing = 0;
            for (SwingPoint swing : this.study.swingManager.swingsLTF) {
                if (swing.getIndex() <= swing2Index) continue;

                highestSwing = Util.max(highestSwing, swing.getValue());
            }
            this.maxRetraction = (highestSwing - swing2.getValue()) * 100.0f / diff;
        }

        //this.study.debug(String.format("Swing 2 is Top? %b", swing2.isTop()));
        //this.study.debug(String.format("Close: %.5f", series.getClose()));
        this.study.debug(String.format("Swing 1: %.5f", swing1.getValue()));
        this.study.debug(String.format("Swing 2: %.5f", swing2.getValue()));
        this.study.debug(String.format("Retraction: %.2f%%", this.currentRetraction));
        this.study.debug(String.format("Max Retraction: %.2f%%", this.maxRetraction));
        //this.study.debug(String.format("Retraction 50%%: %.5f", this.retraction50));
        //this.study.debug(String.format("Retraction 61.8%%: %.5f", this.retraction618));
    }

    protected void confirmWave2(SwingPoint swing) {
        this.onWave2 = true;
        this.wave2Index = swing.getIndex();
        this.reachedZone = false;
        this.invalidatedZone = false;

        // this.study.debug(String.format("Wave 2 confirmed on index #%d", this.wave2Index));
    }

    protected void checkWave(boolean ltfTrend, SwingPoint swing, SwingPoint lastSwingLow, SwingPoint lastSwingHigh) {
        if (ltfTrend) return;

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
