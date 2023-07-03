package ricardo_franco;

import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.SwingPoint;
import com.motivewave.platform.sdk.common.Util;
import com.motivewave.platform.sdk.common.Enums.OrderAction;
import com.motivewave.platform.sdk.common.Enums.StrategyState;
import com.motivewave.platform.sdk.common.Enums.TIF;
import com.motivewave.platform.sdk.order_mgmt.OrderContext;
import com.motivewave.platform.sdk.order_mgmt.Order;


public class OrderManager {
    enum ExitModeSL { FIXED, PERC_RETRACTION };
    enum ExitModeTP { RRR, PERC_PROJECTION };

    ExitModeSL exitModeSL;
    ExitModeTP exitModeTP;

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

    int fixedSLPips;
    int retractionSL;
    float RRR;
    int projectionTP;

    final float retractionStart = 50.0f - 0.0f;
    final float retractionEnd = 61.8f + 0.0f;
    final float minEntryRetraction = 40.0f;
    final boolean tradingLimitOrders = true;

    public OrderManager(FibonacciStrategy study, SwingManager swingManager, TrendManager trendManager, OrderContext ctx, int tradeLots, int fixedSLPips, int retractionSL, double RRR, int projectionTP) {
//        this.exitModeSL = ExitModeSL.FIXED;
        this.exitModeSL = ExitModeSL.PERC_RETRACTION;
        this.exitModeTP = ExitModeTP.PERC_PROJECTION;

        this.study = study;
        this.swingManager = swingManager;
        this.trendManager = trendManager;

        this.ctx = ctx;
        this.qty = tradeLots * ctx.getInstrument().getDefaultQuantity();

        this.fixedSLPips = fixedSLPips;
        this.retractionSL = retractionSL;
        this.RRR = (float)RRR;
        this.projectionTP = projectionTP;

        this.order = null;
        this.orderSL = null;
        this.orderTP = null;
        this.orderSLEntry = -1;
        this.orderTPEntry = -1;
    }

    public void update(DataSeries series) {
        this.study.debug(String.format("Order? %b", this.order != null));
        this.study.debug(String.format("OrderSL? %b", this.orderSL != null));
        this.study.debug(String.format("OrderTP? %b", this.orderTP != null));

        if (this.ctx.getPosition() == 0) {
            this.observe(series);
        } else {
            if (order == null) {
                this.study.error(String.format("ERROR: Position is open but there's no Order. Closing all."));
                this.cancelAllOrders();
            }
        }
    }

    public void cancelAllOrders() {
//        if (this.order != null) {
//            this.ctx.cancelOrders(this.order);
//            this.order = null;
//        }
//        if (this.orderSL != null) {
//            this.ctx.cancelOrders(this.orderSL);
//            this.orderSL = null;
//            this.orderSLEntry = -1;
//        }
//        if (this.orderTP != null) {
//            this.ctx.cancelOrders(this.orderTP);
//            this.orderTP = null;
//            this.orderTPEntry = -1;
//        }

        this.orderSL = this.orderTP = this.order = null;
        this.orderSLEntry = this.orderTPEntry = -1;
        this.ctx.cancelOrders();
    }

    public void onOrderFilled(Order filledOrder) {
        if (this.order != null && filledOrder.getOrderId() == this.order.getOrderId()) {
            if (this.orderSLEntry != -1 && this.orderTPEntry != -1) {
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
        } else if (this.orderSL != null && filledOrder.getOrderId() == this.orderSL.getOrderId()) {
            this.cancelAllOrders();
        } else if (this.orderTP != null && filledOrder.getOrderId() == this.orderTP.getOrderId()) {
            this.cancelAllOrders();
        }
    }

    public void onOrderCancelled(Order cancelledOrder) {
        if (this.order != null && cancelledOrder.getOrderId() == this.order.getOrderId()) {
            this.cancelAllOrders();
            this.ctx.closeAtMarket();
        } else if (this.orderSL != null && cancelledOrder.getOrderId() == this.orderSL.getOrderId()) {
            this.orderSL = null;
            this.orderSLEntry = -1;
        } else if (this.orderTP != null && cancelledOrder.getOrderId() == this.orderTP.getOrderId()) {
            this.orderTP = null;
            this.orderTPEntry =  -1;
        }
    }

    public void onOrderRejected(Order rejectedOrder) {
        if (this.order != null && rejectedOrder.getOrderId() == this.order.getOrderId()) {
            this.cancelAllOrders();
            this.ctx.closeAtMarket();
        } else if (this.orderSL != null && rejectedOrder.getOrderId() == this.orderSL.getOrderId()) {
            this.cancelAllOrders();
            this.ctx.closeAtMarket();
        } else if (this.orderTP != null && rejectedOrder.getOrderId() == this.orderTP.getOrderId()) {
            this.cancelAllOrders();
            this.ctx.closeAtMarket();
        }
    }

    //----------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------

    protected void observe(DataSeries series) {
        if (!this.trendManager.onWave2 || !this.trendManager.validRetraction) {
            if (this.order != null) {
                this.study.debug(String.format("Cancel pending order: %s", this.order.getOrderId()));
                this.cancelAllOrders();
            }

            return;
        }

        if (this.trendManager.currentRetraction <= 0) {
            if (this.order != null) {
                this.study.debug(String.format("Retraction < 0, cancel pending order: %s", this.order.getOrderId()));
                this.cancelAllOrders();
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

        if (this.study.getState() != StrategyState.ACTIVE) {
            this.study.debug("Skip trade because state is not ACTIVE");
            return;
        }

        if (this.trendManager.currentTrendForTrading == "up") {
            float entry = (float)this.trendManager.retraction50;
            float sl = this.calculateSL(series, entry, OrderAction.BUY);
            float tp = this.calculateTP(series, entry, OrderAction.BUY);

            this.study.debug(String.format("BUY LMT @ %.5f (%d)", entry, this.qty));
            this.study.debug(String.format("SL @ %.5f", sl));
            this.study.debug(String.format("TP @ %.5f", tp));

            this.order = this.ctx.createLimitOrder(OrderAction.BUY, TIF.GTC, this.qty, entry);
            this.orderSLEntry = sl;
            this.orderTPEntry = tp;
        } else if (this.trendManager.currentTrendForTrading == "down") {
            float entry = (float)this.trendManager.retraction50;
            float sl = this.calculateSL(series, entry, OrderAction.SELL);
            float tp = this.calculateTP(series, entry, OrderAction.SELL);

            this.study.debug(String.format("SELL LMT @ %.5f (%d)", entry, this.qty));
            this.study.debug(String.format("SL @ %.5f", sl));
            this.study.debug(String.format("TP @ %.5f", tp));

            this.order = this.ctx.createLimitOrder(OrderAction.SELL, TIF.GTC, this.qty, entry);
            this.orderSLEntry = sl;
            this.orderTPEntry = tp;
        }
    }

    protected float calculateSL(DataSeries series, float entry, OrderAction orderAction) {
        if (this.exitModeSL == ExitModeSL.FIXED) {
            if (orderAction == OrderAction.BUY) {
                return entry - calculateSLPips(series, entry, orderAction);
            } else {
                return entry + calculateSLPips(series, entry, orderAction);
            }
        } else if (this.exitModeSL == ExitModeSL.PERC_RETRACTION) {
            return (float)this.trendManager.priceForRetraction((float)this.retractionSL, this.trendManager.swing1, this.trendManager.swing2);
        }

        return -1.0f;
    }

    protected float calculateSLPips(DataSeries series, float entry, OrderAction orderAction) {
        if (this.exitModeSL == ExitModeSL.FIXED) {
            return (float)series.getInstrument().getPointSize() * (float)this.fixedSLPips;
        } else if (this.exitModeSL == ExitModeSL.PERC_RETRACTION) {
            if (orderAction == OrderAction.BUY) {
                return entry - this.calculateSL(series, entry, orderAction);
            } else {
                return this.calculateSL(series, entry, orderAction) - entry;
            }
        }

        return 0;
    }

    protected float calculateTP(DataSeries series, float entry, OrderAction orderAction) {
        if (this.exitModeTP == ExitModeTP.RRR) {
            if (orderAction == OrderAction.BUY) {
                return entry + this.calculateTPPips(series, entry, orderAction);
            } else {
                return entry - this.calculateTPPips(series, entry, orderAction);
            }
        } else if (this.exitModeTP == ExitModeTP.PERC_PROJECTION) {
            return (float)this.trendManager.priceForProjection((float)this.projectionTP, this.trendManager.swing1, this.trendManager.swing2, entry);
        }

        return -1.0f;
    }

    protected float calculateTPPips(DataSeries series, float entry, OrderAction orderAction) {
        if (this.exitModeTP == ExitModeTP.RRR) {
            return this.calculateSLPips(series, entry, orderAction) * this.RRR;
        } else if (this.exitModeTP == ExitModeTP.PERC_PROJECTION) {
            if (orderAction == OrderAction.BUY) {
                return this.calculateTP(series, entry, orderAction) - entry;
            } else {
                return  entry - this.calculateTP(series, entry, orderAction);
            }
        }

        return 0;
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
