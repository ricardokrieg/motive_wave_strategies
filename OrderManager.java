package ricardo_franco;

import com.motivewave.platform.sdk.common.Enums.OrderAction;
import com.motivewave.platform.sdk.order_mgmt.OrderContext;

public class OrderManager {
	OrderContext ctx;
	int qty;
	OrderObject currentOrder;
	
	public OrderManager(OrderContext ctx, int tradeLots) {
		this.ctx = ctx;
		this.qty = tradeLots * ctx.getInstrument().getDefaultQuantity();
		
		this.currentOrder = null;
	}
	
	public void update(float price) {
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
	
	public void placeOrder(float price, OrderAction orderAction) {
		this.currentOrder = new OrderObject(orderAction, price);
	}
	
	public void placeBuyOrder(float price) {
		this.currentOrder = new OrderObject(OrderAction.BUY, price);
	}
	
	public void placeSellOrder(float price) {
		this.currentOrder = new OrderObject(OrderAction.SELL, price);
	}
	
	//----------------------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------------------
	
	protected void placeOrderAtMarket(boolean exit) {
		if (exit) {
			if (this.currentOrder.isBuy()) {
				this.sellAtMarket();
			} else if (this.currentOrder.isSell()) {
				this.buyAtMarket();
			}
		} else {
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
}
