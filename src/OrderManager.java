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
    }

    public void update(DataSeries series, float price) {
        if (this.ctx.getPosition() == 0) {
            this.observe(series);
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

        if (this.trendManager.currentTrend == "up") {
            double entry = this.trendManager.retraction50;
            double sl = entry - series.getInstrument().getPointSize() * (float)slPips;
            double tp = entry + series.getInstrument().getPointSize() * (float)tpPips;

            this.study.debug(String.format("BUY LMT @ %.5f (%d)", entry, this.qty));
            this.study.debug(String.format("SL @ %.5f", sl));
            this.study.debug(String.format("TP @ %.5f", tp));

            this.order = this.ctx.createLimitOrder(OrderAction.BUY, TIF.GTC, this.qty, (float)entry);
        } else if (this.trendManager.currentTrend == "down") {
            double entry = this.trendManager.retraction50;
            double sl = entry + series.getInstrument().getPointSize() * (float)slPips;
            double tp = entry - series.getInstrument().getPointSize() * (float)tpPips;

            this.study.debug(String.format("SELL LMT @ %.5f (%d)", entry, this.qty));
            this.study.debug(String.format("SL @ %.5f", sl));
            this.study.debug(String.format("TP @ %.5f", tp));

            this.order = this.ctx.createLimitOrder(OrderAction.SELL, TIF.GTC, this.qty, (float)entry);
        }
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
