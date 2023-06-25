package ricardo_franco;

import com.motivewave.platform.sdk.common.Enums.OrderAction;
import com.motivewave.platform.sdk.common.Util;


public class OrderObject {
	
	OrderAction orderAction;
	float price;
	float sl;
	float tp;
	float SLDistance;
	boolean running;

	public OrderObject(OrderAction orderAction, float price, float sl, float tp) {
		this.orderAction = orderAction;
		this.price = price;
		this.sl = sl;
		this.tp = tp;
		
		if (this.isBuy()) {
			this.SLDistance = this.price - this.sl;			
		} else {
			this.SLDistance = this.sl - this.price;
		}
		
		this.running = false;
	}
	
	public void execute() {
		this.running = !this.running;
	}
	
	public boolean isEntryPrice(float price) {
		return (this.isBuy() && price >= this.price) || (this.isSell() && price <= this.price); 
	}
	
	public boolean isStopLossPrice(float price) {
		return (this.isBuy() && price <= this.sl) || (this.isSell() && price >= this.sl);
	}
	
	public boolean isTakeProfitPrice(float price) {
		return (this.isBuy() && price >= this.tp) || (this.isSell() && price <= this.tp);
	}
	
	public boolean isBuy() {
		return this.orderAction == OrderAction.BUY;
	}
	
	public boolean isSell() {
		return this.orderAction == OrderAction.SELL;
	}
	
	public void trailStop(float price) {
		if (this.isBuy()) {
			float newSL = price - this.SLDistance;
			this.sl = Util.maxFloat(this.sl, newSL);			
		} else {
			float newSL = price + this.SLDistance;
			this.sl = Util.minFloat(this.sl, newSL);
		}
	}
}
