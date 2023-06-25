package ricardo_franco;

import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.SwingPoint;
import com.motivewave.platform.sdk.common.Util;
import com.motivewave.platform.sdk.common.Enums.OrderAction;
import com.motivewave.platform.sdk.order_mgmt.OrderContext;


public class OrderManager {
    FibonacciStrategy study;

    OrderContext ctx;
    int qty;
    OrderObject currentOrder;

    int slPips;
    int tpPips;

    final float retractionStart = 50.0f - 0.0f;
    final float retractionEnd = 61.8f + 0.0f;
    final float minEntryRetraction = 40.0f;
    final boolean tradingLimitOrders = true;

    public OrderManager(FibonacciStrategy study, OrderContext ctx, int tradeLots, int slPips, int tpPips) {
        this.study = study;

        this.ctx = ctx;
        this.qty = tradeLots * ctx.getInstrument().getDefaultQuantity();

        this.slPips = slPips;
        this.tpPips = tpPips;

        this.currentOrder = null;
    }

    public void update(DataSeries series, float price) {
        if (this.ctx.getPosition() == 0) {
            this.observe(series);
        }

        if (this.currentOrder == null) return;

        if (this.currentOrder.running) {
            if (this.currentOrder.isStopLossPrice(price) || this.currentOrder.isTakeProfitPrice(price)) {
                this.placeOrderAtMarket(true);
            }

            if (this.tradingLimitOrders) {
                this.currentOrder.trailStop(price);
            }
        } else {
            if (this.currentOrder.isEntryPrice(price)) {
                this.placeOrderAtMarket(false);
            }
        }
    }

    public void placeOrder(float price, OrderAction orderAction, float sl, float tp) {
        this.currentOrder = new OrderObject(orderAction, price, sl, tp);
    }

    public void placeBuyOrder(float price, float sl, float tp) {
        this.currentOrder = new OrderObject(OrderAction.BUY, price, sl, tp);
    }

    public void placeSellOrder(float price, float sl, float tp) {
        this.currentOrder = new OrderObject(OrderAction.SELL, price, sl, tp);
    }

    //----------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------

    protected void observe(DataSeries series) {
        if (!this.study.trendManager.onWave2) return;
        if (!this.study.trendManager.validRetraction) return;

        if (this.study.trendManager.maxRetraction > retractionStart) this.study.trendManager.reachedZone = true;
        if (this.study.trendManager.maxRetraction > retractionEnd) this.study.trendManager.invalidatedZone = true;

        this.study.debug(String.format("Reached Zone? %b", this.study.trendManager.reachedZone));
        this.study.debug(String.format("Invalidated Zone? %b", this.study.trendManager.invalidatedZone));

        if (this.study.trendManager.currentRetraction < 0 || !this.study.trendManager.reachedZone || this.study.trendManager.invalidatedZone) {
            if (this.currentOrder != null) {
                this.study.debug("Cancel pending orders");
                if (this.currentOrder.running) {
                    this.study.debug("Cant cancel: Order is running");
                } else {
                    this.currentOrder = null;
                    return;
                }
            } else {
                return;
            }
        }

        this.study.debug("Trade is valid. We can create the pending Order now.");

        if (this.tradingLimitOrders) {
            if (this.study.trendManager.currentTrend == "up") {
                double entry = series.getClose();
                double sl = entry - series.getInstrument().getPointSize() * (float)slPips;
                double tp = entry + series.getInstrument().getPointSize() * (float)tpPips;

                this.study.debug(String.format("BUY @ %.5f", entry));
                this.study.debug(String.format("SL @ %.5f", sl));
                this.study.debug(String.format("TP @ %.5f", tp));

                this.placeBuyOrder((float)entry, (float)sl, (float)tp);
            } else if (this.study.trendManager.currentTrend == "down") {
                double entry = series.getClose();
                double sl = entry + series.getInstrument().getPointSize() * (float)slPips;
                double tp = entry - series.getInstrument().getPointSize() * (float)tpPips;

                this.study.debug(String.format("SELL @ %.5f", entry));
                this.study.debug(String.format("SL @ %.5f", sl));
                this.study.debug(String.format("TP @ %.5f", tp));

                this.placeSellOrder((float)entry, (float)sl, (float)tp);
            }

            return;
        }

        SwingPoint lastSwingHigh = null;
        SwingPoint lastSwingLow = null;

        for (SwingPoint swing : this.study.swingManager.swingsLTF) {
            if (swing.isTop()) lastSwingHigh = swing;
            if (swing.isBottom()) lastSwingLow = swing;
        }

        if (this.study.trendManager.currentTrend == "up") {
            if (lastSwingHigh != null) {
                double entry = this.getEntry(series, lastSwingHigh, true);
                double sl = this.getEntry(series, lastSwingLow, false);
                double tp = (2.0f * entry) - sl;

                double retraction = (this.study.trendManager.currentSwing2.getValue() - entry) * 100.0f / this.study.trendManager.currentDiff;
                if (retraction < this.minEntryRetraction) {
                    this.study.debug(String.format("Retraction too low: %.2f%%", retraction));
                    return;
                }

                double SLDistance = entry - sl;
                double minSLDistance = this.getMinSLDistance(series);
                if (SLDistance < minSLDistance) {
                    this.study.debug(String.format("Entry: %.5f / SL: %.5f", entry, sl));
                    this.study.debug(String.format("SL too close: %.5f (min is %.5f)", SLDistance, minSLDistance));
                    return;
                }

                this.study.debug(String.format("BUY @ %.5f", entry));
                this.study.debug(String.format("SL @ %.5f", sl));
                this.study.debug(String.format("TP @ %.5f", tp));

                this.placeBuyOrder((float)entry, (float)sl, (float)tp);
            }
        } else if (this.study.trendManager.currentTrend == "down") {
            if (lastSwingLow != null) {
                double entry = this.getEntry(series, lastSwingLow, false);
                double sl = this.getEntry(series, lastSwingHigh, true);
                double tp = (2.0f * entry) - sl;

                double retraction = (entry - this.study.trendManager.currentSwing2.getValue()) * 100.0f / this.study.trendManager.currentDiff;
                if (retraction < this.minEntryRetraction) {
                    this.study.debug(String.format("Retraction too low: %.2f%%", retraction));
                    return;
                }

                double SLDistance = sl - entry;
                double minSLDistance = this.getMinSLDistance(series);
                if (SLDistance < minSLDistance) {
                    this.study.debug(String.format("Entry: %.5f / SL: %.5f", entry, sl));
                    this.study.debug(String.format("SL too close: %.5f (min is %.5f)", SLDistance, minSLDistance));
                    return;
                }

                this.study.debug(String.format("SELL @ %.5f", entry));
                this.study.debug(String.format("SL @ %.5f", sl));
                this.study.debug(String.format("TP @ %.5f", tp));

                this.placeSellOrder((float)entry, (float)sl, (float)tp);
            }
        }
    }

    protected void placeOrderAtMarket(boolean exit) {
        if (exit) {
            this.study.debug("Exit Order filled");

            if (this.currentOrder.isBuy()) {
                this.sellAtMarket();
            } else if (this.currentOrder.isSell()) {
                this.buyAtMarket();
            }

            this.currentOrder = null;
        } else {
            this.study.debug("Entry Order filled");

            if (this.currentOrder.isBuy()) {
                this.buyAtMarket();
            } else if (this.currentOrder.isSell()) {
                this.sellAtMarket();
            }
        }
    }

    protected void buyAtMarket() {
        this.ctx.buy(this.qty);
        this.currentOrder.execute();
    }

    protected void sellAtMarket() {
        this.ctx.sell(this.qty);
        this.currentOrder.execute();
    }

    protected double getEntry(DataSeries series, SwingPoint swing, boolean isBuy) {
        double pointSize = series.getInstrument().getPointSize();
        double spread = series.getInstrument().getSpread();
        //double diff = (pointSize * spread) + pointSize;
        double diff = pointSize * spread;

        if (isBuy)
            return swing.getValue() + diff;
        else
            return swing.getValue() - diff;
    }

    protected double getMinSLDistance(DataSeries series) {
        double pointSize = series.getInstrument().getPointSize();
        this.study.debug(String.format("Tick Size: %.5f", series.getInstrument().getTickSize()));
        this.study.debug(String.format("Point Size: %.5f", pointSize));
        this.study.debug(String.format("Spread: %.5f", series.getInstrument().getSpread()));

        return Util.max(pointSize * 10, pointSize * series.getInstrument().getSpread() * 5.0f);
    }
}
