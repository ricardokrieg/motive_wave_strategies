package ricardo_franco;

import com.motivewave.platform.sdk.common.Enums.OrderAction;


public class OrderObject {
	
	OrderAction orderAction;
	float price;
	boolean running;

	public OrderObject(OrderAction orderAction, float price) {
		this.orderAction = orderAction;
		this.price = price;
		
		this.running = false;
	}
	
	public void execute() {
		this.running = !this.running;
	}
	
	public boolean isEntryPrice(float price) {
		return (this.isBuy() && price >= this.price) || (this.isSell() && price <= this.price); 
	}
	
	public boolean isStopLossPrice(float price) {
		return false;
	}
	
	public boolean isTakeProfitPrice(float price) {
		return false;
	}
	
	public boolean isBuy() {
		return this.orderAction == OrderAction.BUY;
	}
	
	public boolean isSell() {
		return this.orderAction == OrderAction.SELL;
	}
}
