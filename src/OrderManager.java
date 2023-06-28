package ricardo_franco;

import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.SwingPoint;
import com.motivewave.platform.sdk.common.Util;
import com.motivewave.platform.sdk.common.Enums.OrderAction;
import com.motivewave.platform.sdk.common.Enums.TIF;
import com.motivewave.platform.sdk.order_mgmt.OrderContext;
import com.motivewave.platform.sdk.order_mgmt.Order;


public class OrderManager {
    FibonacciStrategy study;
    SwingManager swingManager;
    TrendManager trendManager;

    OrderContext ctx;
    int qty;
    Order order;
    Order orderSL;
    Order orderTP;
    float orderSLEntry;
    float orderTPEntry;

    int slPips;
    int tpPips;

    final float retractionStart = 50.0f - 0.0f;
    final float retractionEnd = 61.8f + 0.0f;
    final float minEntryRetraction = 40.0f;
    final boolean tradingLimitOrders = true;

    public OrderManager(FibonacciStrategy study, SwingManager swingManager, TrendManager trendManager, OrderContext ctx, int tradeLots, int slPips, int tpPips) {
        this.study = study;
        this.swingManager = swingManager;
        this.trendManager = trendManager;

        this.ctx = ctx;
        this.qty = tradeLots * ctx.getInstrument().getDefaultQuantity();

        this.slPips = slPips;
        this.tpPips = tpPips;

        this.order = null;
        this.orderSL = null;
        this.orderTP = null;
        this.orderSLEntry = -1;
        this.orderTPEntry = -1;
    }

    public void update(DataSeries series, float price) {
        if (this.ctx.getPosition() == 0) {
            this.observe(series);
        } else {
            this.manageOrders(series);
        }
    }

    //----------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------

    protected void observe(DataSeries series) {
        if (!this.trendManager.onWave2) return;
        if (!this.trendManager.validRetraction) return;

        if (this.trendManager.currentRetraction <= 0) {
            if (this.order != null) {
                this.study.debug(String.format("Cancel pending order: %s", this.order.getOrderId()));
                this.ctx.cancelOrders();

                this.order = null;
            } else {
                return;
            }
        }

        if (this.order != null) {
            return;
        }

        if (this.trendManager.maxRetraction > 50.0f) {
            this.study.debug("Skip trade because retraction was bigger than 50%");
            return;
        }

        if (this.trendManager.currentTrend == "up") {
            float entry = (float)this.trendManager.retraction50;
            float sl = entry - (float)series.getInstrument().getPointSize() * (float)this.slPips;
            float tp = entry + (float)series.getInstrument().getPointSize() * (float)this.tpPips;

            this.study.debug(String.format("BUY LMT @ %.5f (%d)", entry, this.qty));
            this.study.debug(String.format("SL @ %.5f", sl));
            this.study.debug(String.format("TP @ %.5f", tp));

            this.order = this.ctx.createLimitOrder(OrderAction.BUY, TIF.GTC, this.qty, entry);
            this.orderSLEntry = sl;
            this.orderTPEntry = tp;
        } else if (this.trendManager.currentTrend == "down") {
            float entry = (float)this.trendManager.retraction50;
            float sl = entry + (float)series.getInstrument().getPointSize() * (float)this.slPips;
            float tp = entry - (float)series.getInstrument().getPointSize() * (float)this.tpPips;

            this.study.debug(String.format("SELL LMT @ %.5f (%d)", entry, this.qty));
            this.study.debug(String.format("SL @ %.5f", sl));
            this.study.debug(String.format("TP @ %.5f", tp));

            this.order = this.ctx.createLimitOrder(OrderAction.SELL, TIF.GTC, this.qty, entry);
            this.orderSLEntry = sl;
            this.orderTPEntry = tp;
        }
    }

    protected void manageOrders(DataSeries series) {
        if (this.order != null) {
            if (this.order.exists()) {
                if (this.order.isFilled()) {
                    if (this.orderSLEntry != -1 || this.orderTPEntry != -1) {
                        OrderAction orderAction;

                        if (this.order.isBuy()) {
                            orderAction = OrderAction.SELL;
                        } else {
                            orderAction = OrderAction.BUY;
                        }

                        this.orderSL = this.ctx.createStopOrder(orderAction, TIF.GTC, this.qty, this.orderSLEntry);
                        this.orderTP = this.ctx.createLimitOrder(orderAction, TIF.GTC, this.qty, this.orderTPEntry);

                        this.orderSLEntry = this.orderTPEntry = -1;
                    }
                }
            } else {
                this.order = null;
            }
        }

        if (this.orderSL != null && this.orderSL.exists() && this.orderSL.isFilled()) {
            this.orderSL = this.orderTP = this.order = null;

            this.ctx.cancelOrders();
        }
        if (this.orderTP != null && this.orderTP.exists() && this.orderTP.isFilled()) {
            this.orderSL = this.orderTP = this.order = null;

            this.ctx.cancelOrders();
        }
    }

    /*
    protected double getMinSLDistance(DataSeries series) {
        double pointSize = series.getInstrument().getPointSize();
        this.study.debug(String.format("Tick Size: %.5f", series.getInstrument().getTickSize()));
        this.study.debug(String.format("Point Size: %.5f", pointSize));
        this.study.debug(String.format("Spread: %.5f", series.getInstrument().getSpread()));

        return Util.max(pointSize * 10, pointSize * series.getInstrument().getSpread() * 5.0f);
    }
    */
}
