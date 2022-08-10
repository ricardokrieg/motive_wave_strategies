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
	
	final float retractionStart = 50.0f - 10.0f;
	final float retractionEnd = 61.8f + 10.0f;
	
	public OrderManager(FibonacciStrategy study, OrderContext ctx, int tradeLots) {
		this.study = study;
		
		this.ctx = ctx;
		this.qty = tradeLots * ctx.getInstrument().getDefaultQuantity();
		
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
			if (this.currentOrder != null) this.study.debug("Cancel pending orders");
			this.currentOrder = null;
			return;
		}
		
		this.study.debug("Trade is valid. We can create the Stop Order now.");
		
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
				
				//stopPrice = (float)lastSwingHigh.getValue();
				//stopLossPrice = (float)lastSwingLow.getValue();
				//takeProfitPrice
				//orderAction = Enums.OrderAction.BUY;
				//ctx.signal(lastSwingLow.getIndex(), Signals.BUY_STOP, "BUY", series.getClose(lastSwingLow.getIndex()));
			}
		} else if (this.study.trendManager.currentTrend == "down") {
			if (lastSwingLow != null) {
				double entry = this.getEntry(series, lastSwingLow, false);
				double sl = this.getEntry(series, lastSwingHigh, true);
				double tp = (2.0f * entry) - sl;
				
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
				
				//stopPrice = (float)lastSwingLow.getValue();
				//stopLossPrice = (float)lastSwingHigh.getValue();
				//orderAction = Enums.OrderAction.SELL;
				//ctx.signal(lastSwingHigh.getIndex(), Signals.SELL_STOP, "SELL", series.getClose(lastSwingHigh.getIndex()));
				//debug(String.format("Sending signal %.5f #%d %.5f", stopPrice, lastSwing.getIndex(), series.getClose(lastSwing.getIndex())));
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
		double diff = (pointSize * spread) + pointSize;
		
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
	
	/*public void openTrade(OrderContext ctx, Enums.OrderAction orderAction) {
		if (ctx.getPosition() != 0) {
			debug("Position is not zero");
			return;
		}
		
		if (stopPrice <= 0) {
			debug("stopPrice is invalid");
			return;
		}
		
		Instrument instr = ctx.getInstrument();
		// TODO the code is running twice, so I specify the half of the value I want
		int lots = 200 / 2;
		double tickSize = instr.getTickSize();
		float spread = (instr.getSpread() / 2.0f) * (float)tickSize;
		
		debug("Openning Order");
		debug(String.format("Lot Size: %d", lots));
		debug(String.format("Stop Price Before: %.5f", stopPrice));
		debug(String.format("Stop Loss Price: %.5f", stopLossPrice));
		debug(String.format("Tick Size: %.5f", tickSize));
		debug(String.format("Spread: %.5f", spread));
		
		if (orderAction == Enums.OrderAction.BUY) {
			stopPrice += (tickSize + spread);
		} else if (orderAction == Enums.OrderAction.SELL) {
			stopPrice -= (tickSize + spread);
		}
		
		debug(String.format("Stop Price After: %.5f", stopPrice));
		
		//ctx.cancelOrders();
		//if (currentOrder != null) return; // TODO remove this
		//currentOrder = ctx.createStopOrder(orderAction, Enums.TIF.GTC, lots, stopPrice);
		//ctx.createStopOrder(Enums.OrderAction.BUY, Enums.TIF.GTC, lots, stopPrice + (10.0f * (float)tickSize));
		//ctx.createLimitOrder(Enums.OrderAction.BUY, Enums.TIF.GTC, lots, stopPrice - (10.0f * (float)tickSize));
	}*/
}
