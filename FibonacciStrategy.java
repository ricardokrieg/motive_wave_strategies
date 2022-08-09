package ricardo_franco;

import java.util.ArrayList;
import java.util.List;

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.draw.Line;
import com.motivewave.platform.sdk.draw.Marker;
import com.motivewave.platform.sdk.study.*;
import com.motivewave.platform.sdk.order_mgmt.*;


/** Trading using Fibonacci retraction zone, following the trend */
@StudyHeader(
 namespace="com.ricardofranco", 
 id="FIBONACCI_STRATEGY",
 name="Fibonacci Strategy",
 label="Fibonacci Strategy",
 desc="Fibonacci Strategy",
 menu="Ricardo Franco",
 overlay=true,
 studyOverlay=true,
 signals = true,
 strategy = true,
 autoEntry = true,
 manualEntry = false,
 supportsUnrealizedPL = true,
 supportsRealizedPL = true,
 supportsTotalPL = true)
public class FibonacciStrategy extends Study {
	enum Values { MA };
	enum Signals { BUY_STOP, SELL_STOP };
	
	final static String LTF_MARKER = "ltfMarker";
	final static String TTF_MARKER = "ttfMarker";
	final static String LTF_LINE = "ltfLine";
	final static String TTF_LINE = "ttfLine";
	
	GraphicManager graphicManager;
	OrderManager orderManager;
	SwingManager swingManager;
	
	boolean onWave2 = false;
	int wave2Index = 0;
	String currentTrend = null;
	
	double retraction = 0;
	double retraction50 = 0;
	double retraction618 = 0;
	boolean validRetraction = false;
	boolean reachedZone = false;
	boolean invalidatedZone = false;
	
	boolean checkOrders = false;
	Order currentOrder = null;
	float stopPrice = 0;
	float stopLossPrice = 0;
	float takeProfitPrice = 0;
	Enums.OrderAction orderAction = null;
	
	public void initialize(Defaults defaults) {
	    SettingsDescriptor sd = new SettingsDescriptor();
	    setSettingsDescriptor(sd);

	    SettingTab tab = new SettingTab("General");
	    sd.addTab(tab);

	    SettingGroup inputs = new SettingGroup("Inputs");
	    inputs.addRow(new IntegerDescriptor(Inputs.STRENGTH, "Swing Point Strengh", 2, 2, 9999, 1));
	    tab.addGroup(inputs);
	    
	    SettingGroup lines = new SettingGroup("Display");
	    
	    lines.addRow(new PathDescriptor(LTF_LINE, "LTF Line", defaults.getBlue(), 1.0f, null, true, true, true));
	    lines.addRow(new MarkerDescriptor(LTF_MARKER, "LTF Marker", 
	            Enums.MarkerType.CIRCLE, Enums.Size.SMALL, defaults.getBlue(), defaults.getLineColor(), true, true));
	    
	    lines.addRow(new PathDescriptor(TTF_LINE, "TTF Line", defaults.getRed(), 2.0f, null, true, true, true));
	    lines.addRow(new MarkerDescriptor(TTF_MARKER, "TTF Marker", 
	            Enums.MarkerType.CIRCLE, Enums.Size.MEDIUM, defaults.getRed(), defaults.getLineColor(), true, true));
	    
	    tab.addGroup(lines);
	    
	    RuntimeDescriptor desc = new RuntimeDescriptor();
	    setRuntimeDescriptor(desc);

	    desc.exportValue(new ValueDescriptor(Signals.BUY_STOP, Enums.ValueType.BOOLEAN, "Buy Signal", null));
	    desc.exportValue(new ValueDescriptor(Signals.SELL_STOP, Enums.ValueType.BOOLEAN, "Sell Signal", null));

	    // Signals
	    desc.declareSignal(Signals.BUY_STOP, "Buy Signal");
	    desc.declareSignal(Signals.SELL_STOP, "Sell Signal");
	}
	
	@Override
	public void onLoad(Defaults defaults) {
		this.graphicManager = new GraphicManager(
				getSettings().getMarker(LTF_MARKER),
				getSettings().getMarker(TTF_MARKER),
				getSettings().getPath(LTF_LINE),
				getSettings().getPath(TTF_LINE));
		
		this.swingManager = new SwingManager(getSettings().getInteger(Inputs.STRENGTH));
	}
	
	@Override
	public void onActivate(OrderContext ctx) {
		this.orderManager = new OrderManager(ctx, getSettings().getTradeLots());
	}
	
	public void drawMarkersAndLines() {
		for (Line line : this.graphicManager.getLTFLines(this.swingManager.swingsLTF)) {
			if (line != null) addFigure(line);
		}
		
		for (Marker marker : this.graphicManager.getLTFMarkers(this.swingManager.swingsLTF)) {
			if (marker != null) addFigure(marker);
		}
		
		List<SwingPoint> swings = new ArrayList<SwingPoint>();
		for (SwingPoint swing : this.swingManager.swingsLTF) {
			if (!this.swingManager.swingsTTFKeys.contains(swing.getIndex())) continue;
			
			swings.add(swing);
		}
		
		for (Marker marker : this.graphicManager.getTTFMarkers(swings)) {
			if (marker != null) addFigure(marker);
		}
		
		for (Line line : this.graphicManager.getTTFLines(swings)) {
			if (line != null) addFigure(line);
		}
	}
	
	public void clear() {
		this.swingManager.clear();
		clearFigures();
	}
	
	public void computeTrend(boolean ltfTrend) {
		currentTrend = null;
		
		SwingPoint lastSwingHigh = null;
		SwingPoint lastSwingLow = null;
		SwingPoint highestSwingHigh = null;
		SwingPoint lowestSwingLow = null;
		SwingPoint leadingSwingHigh = null;
		SwingPoint leadingSwingLow = null;
		
		for (SwingPoint swing : this.swingManager.swingsLTF) {
			if (!ltfTrend) {
				if (!this.swingManager.swingsTTFKeys.contains(swing.getIndex())) continue;
				
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
										this.swingManager.swingsTTFKeys.add(highestSwingHigh.getIndex());
									}
								}
								
								lowestSwingLow = swing;
								leadingSwingLow = null;
								leadingSwingHigh = lastSwingHigh;
								
								currentTrend = "down";
								if (!ltfTrend) {
									onWave2 = true;
									wave2Index = swing.getIndex();
									reachedZone = false;
									invalidatedZone = false;
									//debug(String.format("Wave 2 confirmed on index #%d", wave2Index));
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
										this.swingManager.swingsTTFKeys.add(lowestSwingLow.getIndex());
									}
								}
								
								highestSwingHigh = swing;
								leadingSwingHigh = null;
								leadingSwingLow = lastSwingLow;
								
								currentTrend = "up";
								if (!ltfTrend) {
									onWave2 = true;
									wave2Index = swing.getIndex();
									reachedZone = false;
									invalidatedZone = false;
									//debug(String.format("Wave 2 confirmed on index #%d", wave2Index));
								}
							}
						}
					}
				}
			}
			
			if (swing.isTop())
				lastSwingHigh = swing;
			else
				lastSwingLow = swing;
		}
	}
	
	public void computeRetraction(DataSeries series) {
		validRetraction = false;
		
		//debug(String.format("On Wave 2? %b", onWave2));
		if (!onWave2) return;
		
		if (this.swingManager.swingsTTFKeys.size() < 2) {
			debug("Not enough swing points to compute correction");
			return;
		}
		
		int swing1Index = this.swingManager.swingsTTFKeys.get(this.swingManager.swingsTTFKeys.size() - 2);
		int swing2Index = this.swingManager.swingsTTFKeys.get(this.swingManager.swingsTTFKeys.size() - 1);
		
		SwingPoint swing1 = null;
		SwingPoint swing2 = null;
		
		for (SwingPoint swing : this.swingManager.swingsLTF) {
			if (swing.getIndex() == swing1Index) swing1 = swing;
			if (swing.getIndex() == swing2Index) swing2 = swing;
			
			if (swing1 != null & swing2 != null) break;
		}
		
		if (swing1 == null || swing2 == null) {
			debug("Not enough swing points to compute correction");
			return;
		}
		
		validRetraction = true;
		
		if (swing2.isTop()) {
			double diff = swing2.getValue() - swing1.getValue();
			
			retraction50 = diff * 0.5f + swing1.getValue();
			retraction618 = diff * 0.618f + swing1.getValue();
			retraction = (swing2.getValue() - series.getClose()) * 100.0f / diff;
		} else {
			double diff = swing1.getValue() - swing2.getValue();
			
			retraction50 = diff * 0.5f + swing2.getValue();
			retraction618 = diff * 0.618f + swing2.getValue();
			retraction = (series.getClose() - swing2.getValue()) * 100.0f / diff;
		}
		
		/*debug(String.format("Swing 2 is Top? %b", swing2.isTop()));
		debug(String.format("Close: %.5f", series.getClose()));
		debug(String.format("Swing 1: %.5f", swing1.getValue()));
		debug(String.format("Swing 2: %.5f", swing2.getValue()));
		debug(String.format("Retraction: %.2f%%", retraction));
		debug(String.format("Retraction 50%%: %.5f", retraction50));
		debug(String.format("Retraction 61.8%%: %.5f", retraction618));*/
	}
	
	public void prepareTrade(DataContext ctx, DataSeries series) {
		if (!onWave2) return;
		if (!validRetraction) return;
		
		if (retraction > 40.0f) reachedZone = true;
		if (retraction > 66.8f) invalidatedZone = true;
		
		if (reachedZone && !invalidatedZone) {
			debug("Trade is valid. We can create the Stop order now.");
			
			SwingPoint lastSwingHigh = null;
			SwingPoint lastSwingLow = null;
			
			for (SwingPoint swing : this.swingManager.swingsLTF) {
				if (swing.isTop()) lastSwingHigh = swing;
				if (swing.isBottom()) lastSwingLow = swing;
			}
			
			if (currentTrend == "up") {
				if (lastSwingHigh != null) {
					debug(String.format("BUY @ %.5f", lastSwingHigh.getValue()));
					stopPrice = (float)lastSwingHigh.getValue();
					stopLossPrice = (float)lastSwingLow.getValue();
					//takeProfitPrice
					orderAction = Enums.OrderAction.BUY;
					//ctx.signal(lastSwingLow.getIndex(), Signals.BUY_STOP, "BUY", series.getClose(lastSwingLow.getIndex()));
				}
			} else if (currentTrend == "down") {
				if (lastSwingLow != null) {	
					debug(String.format("SELL @ %.5f", lastSwingLow.getValue()));
					stopPrice = (float)lastSwingLow.getValue();
					stopLossPrice = (float)lastSwingHigh.getValue();
					orderAction = Enums.OrderAction.SELL;
					//ctx.signal(lastSwingHigh.getIndex(), Signals.SELL_STOP, "SELL", series.getClose(lastSwingHigh.getIndex()));
					//debug(String.format("Sending signal %.5f #%d %.5f", stopPrice, lastSwing.getIndex(), series.getClose(lastSwing.getIndex())));
				}
			}
		}
	}

	public void openTrade(OrderContext ctx, Enums.OrderAction orderAction) {
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
	}
	
	@Override
	public void onBarClose(DataContext ctx) {
		clear();
		
		DataSeries series = ctx.getDataSeries();
		
		this.swingManager.update(series);
		
		computeTrend(true);
		computeTrend(false);
		
		drawMarkersAndLines();
		
		computeRetraction(series);
		prepareTrade(ctx, series);

		super.onBarClose(ctx);
	}
	
	@Override
	public void onBarUpdate(OrderContext ctx) {
		// FIXME just faking an order
		DataSeries series = ctx.getDataContext().getDataSeries();
		if (series.getClose() < 1.18140f) {
			if (currentOrder == null) {
				//currentOrder = ctx.createStopOrder(Enums.OrderAction.BUY, Enums.TIF.GTC, 100, 1.18200f);
				//ctx.createLimitOrder(Enums.OrderAction.SELL, Enums.TIF.GTC, 100, 1.18260f);
			}
		}
		
		if (invalidatedZone && currentOrder != null && ctx.getPosition() == 0) {
			//ctx.cancelOrders();
		}
		
		if (orderAction == Enums.OrderAction.BUY) {
			debug("BUY signal");
		} else if (orderAction == Enums.OrderAction.SELL) {
			debug("SELL signal");
		} else {
			return;
		}
		
		openTrade(ctx, orderAction);
		
		orderAction = null;
		stopPrice = 0;
		
		checkOrders = true;
		
		super.onBarUpdate(ctx);
	}
	
	@Override
	protected void calculate(int index, DataContext ctx) {
		DataSeries series = ctx.getDataSeries();
		
		if (!series.isBarComplete(index)) return;
		
		if (checkOrders) {
		}
		checkOrders = false;
		
		if (series.getClose() < 1.18140f) {
			ctx.signal(index, Signals.SELL_STOP, "Sell Now!", series.getClose(index));
			/*if (currentOrder == null) {
				//currentOrder = ctx.createStopOrder(Enums.OrderAction.BUY, Enums.TIF.GTC, 100, 1.18200f);
				//ctx.createLimitOrder(Enums.OrderAction.SELL, Enums.TIF.GTC, 100, 1.18260f);
				debug("HALO 4");
				ctx.signal(series.getEndIndex() - 1, Signals.SELL_STOP, "Sell Now!", series.getClose());
			}*/
		}

		series.setComplete(index);
	}
	
	@Override
	public void onSignal(OrderContext ctx, Object signal) {
		debug("SIGNAL!!!!!");
		
		//this.orderManager.placeOrder();
		
		if (currentOrder == null) {
			debug("PLACING ORDER");
			currentOrder = ctx.createStopOrder(Enums.OrderAction.BUY, Enums.TIF.GTC, 100, 1.18200f);
			ctx.createLimitOrder(Enums.OrderAction.SELL, Enums.TIF.GTC, 100, 1.18260f);
			ctx.buy(100);
		}
	}
}
