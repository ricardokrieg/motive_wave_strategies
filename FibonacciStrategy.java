package study_examples;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.draw.*;
import com.motivewave.platform.sdk.study.*;
import com.motivewave.platform.sdk.order_mgmt.*;


/** Trading using Fibonacci retracement zone, following the trend */
@StudyHeader(
 namespace="com.ricardofranco", 
 id="FIBONACCI_STRATEGY",
 name="Fibonacci Strategy",
 label="Fibonacci Strategy",
 desc="Fibonacci Strategy",
 menu="Ricardo",
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
	
	List<SwingPoint> swingsLTF = new ArrayList<SwingPoint>();
	List<Integer> swingsTTFKeys = new ArrayList<Integer>();
	
	boolean onWave2 = false;
	int wave2Index = 0;
	String currentTrend = null;
	
	double retraction = 0;
	double retraction50 = 0;
	double retraction618 = 0;
	boolean validRetraction = false;
	boolean reachedZone = false;
	boolean invalidatedZone = false;
	
	Order currentOrder = null;
	float stopPrice = 0;
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
	    
	    lines.addRow(new PathDescriptor(Inputs.PATH, "LTF Line", defaults.getBlue(), 1.0f, 
                null, true, true, true));
	    lines.addRow(new MarkerDescriptor(Inputs.UP_MARKER, "LTF Swing High Marker", 
	            Enums.MarkerType.TRIANGLE, Enums.Size.VERY_SMALL, defaults.getGreen(), defaults.getLineColor(), true, true));
	    lines.addRow(new MarkerDescriptor(Inputs.DOWN_MARKER, "LTF Swing Low Marker", 
	            Enums.MarkerType.TRIANGLE, Enums.Size.VERY_SMALL, defaults.getRed(), defaults.getLineColor(), true, true));
	    
	    lines.addRow(new PathDescriptor("TTFLine", "TTF Line", defaults.getRed(), 2.0f, 
                null, true, true, true));
	    lines.addRow(new MarkerDescriptor("TTFSHMarker", "TTF Swing High Marker", 
	            Enums.MarkerType.CIRCLE, Enums.Size.LARGE, defaults.getGreen(), defaults.getLineColor(), true, true));
	    lines.addRow(new MarkerDescriptor("TTFSLMarker", "TTF Swing Low Marker", 
	            Enums.MarkerType.CIRCLE, Enums.Size.LARGE, defaults.getRed(), defaults.getLineColor(), true, true));
	    
	    tab.addGroup(lines);
	    
	    RuntimeDescriptor desc = new RuntimeDescriptor();
	    setRuntimeDescriptor(desc);

	    desc.exportValue(new ValueDescriptor(Signals.BUY_STOP, Enums.ValueType.BOOLEAN, "Buy Signal", null));
	    desc.exportValue(new ValueDescriptor(Signals.SELL_STOP, Enums.ValueType.BOOLEAN, "Sell Signal", null));

	    // Signals
	    desc.declareSignal(Signals.BUY_STOP, "Buy Signal");
	    desc.declareSignal(Signals.SELL_STOP, "Sell Signal");
	}
	
	public static <T> List<T> castList(Class<? extends T> clazz, Collection<?> c) {
	    List<T> r = new ArrayList<T>(c.size());
	    for(Object o: c)
	      r.add(clazz.cast(o));
	    return r;
	}
	
	public List<SwingPoint> computeSwings(DataSeries series, boolean top, String markerName, Enums.Position position, String msg) {
		return castList(SwingPoint.class, series.calcSwingPoints(top, getSettings().getInteger(Inputs.STRENGTH)));
	}
	
	public void mergeSwings(List<SwingPoint> swings1, List<SwingPoint> swings2) {
		Map<Integer, SwingPoint> mergedSwings = new HashMap<Integer, SwingPoint>();
		
		for (SwingPoint swing : swings1) {
			mergedSwings.put(swing.getIndex(), swing);
		}
		
		for (SwingPoint swing : swings2) {
			mergedSwings.put(swing.getIndex(), swing);
		}
		
		SortedSet<Integer> keys = new TreeSet<Integer>(mergedSwings.keySet());
		for (Integer key : keys) {
			SwingPoint swing = mergedSwings.get(key);
			
			swingsLTF.add(swing);
		}
	}
	
	public void deleteNeighborSwings() {
		List<Integer> keysToKeep = new ArrayList<Integer>();
		List<Integer> keysToIgnore = new ArrayList<Integer>();
		
		for (SwingPoint swing : swingsLTF) {
			if (keysToIgnore.contains(swing.getIndex())) continue;
			
			double strength = swing.getAvgStrength();
			Integer strengthKey = swing.getIndex();
			
			for (SwingPoint swingToCompare : swingsLTF) {
				int keyToCompare = swingToCompare.getIndex();
				if (keyToCompare > swing.getIndex()) {
					
					if (swingToCompare.isTop() == swing.isTop()) {
						if (swingToCompare.getAvgStrength() > strength) {
							strength = swingToCompare.getAvgStrength();
							strengthKey = keyToCompare;
						} else {
							keysToIgnore.add(keyToCompare);
						}
					} else {
						break;
					}
				}
			}
			
			keysToKeep.add(strengthKey);
		}
		
		List<SwingPoint> tempSwingsLTF = new ArrayList<SwingPoint>();
		
		for (SwingPoint swing : swingsLTF) {
			int key = swing.getIndex();
			if (keysToKeep.contains(key)) {
				tempSwingsLTF.add(swing);
			}
		}
		
		swingsLTF.clear();
		swingsLTF.addAll(tempSwingsLTF);
	}
	
	public void drawMarker(SwingPoint swing, String markerName, Enums.Position position) {
		MarkerInfo marker = getSettings().getMarker(markerName);
		if (marker.isEnabled())
			addFigure(
					new Marker(swing.getCoordinate(), position, marker, String.format("Swing #%d", swing.getIndex())));
	}
	
	public void drawLine(SwingPoint swing1, SwingPoint swing2, String lineName) {
		PathInfo line = getSettings().getPath(lineName);
		if (line.isEnabled())
			addFigure(new Line(swing1.getCoordinate(), swing2.getCoordinate(), line));
	}
	
	public void drawLTFMarkersAndLines() {
		SwingPoint lastSwingHigh = null;
		SwingPoint lastSwingLow = null;
		
		for (SwingPoint swing : swingsLTF) {
			if (swing.isTop()) {
				drawMarker(swing, Inputs.UP_MARKER, Enums.Position.TOP);
				
				if (lastSwingLow != null) drawLine(lastSwingLow, swing, Inputs.PATH);
				lastSwingHigh = swing;
			} else {
				drawMarker(swing, Inputs.DOWN_MARKER, Enums.Position.BOTTOM);
				
				if (lastSwingHigh != null) drawLine(lastSwingHigh, swing, Inputs.PATH);
				lastSwingLow = swing;
			}
		}
	}
	
	public void drawTTFMarkersAndLines() {
		SwingPoint lastSwingHigh = null;
		SwingPoint lastSwingLow = null;
		
		for (SwingPoint swing : swingsLTF) {
			if (!swingsTTFKeys.contains(swing.getIndex())) continue;
			
			if (swing.isTop()) {
				drawMarker(swing, "TTFSHMarker", Enums.Position.TOP);
				
				if (lastSwingLow != null) drawLine(lastSwingLow, swing, "TTFLine");
				lastSwingHigh = swing;
			} else {
				drawMarker(swing, "TTFSLMarker", Enums.Position.BOTTOM);
				
				if (lastSwingHigh != null) drawLine(lastSwingHigh, swing, "TTFLine");
				lastSwingLow = swing;
			}
		}
	}
	
	public void clear() {
		swingsLTF.clear();
		swingsTTFKeys.clear();
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
		
		for (SwingPoint swing : swingsLTF) {
			if (!ltfTrend) {
				if (!swingsTTFKeys.contains(swing.getIndex())) continue;
				
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
										swingsTTFKeys.add(highestSwingHigh.getIndex());
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
										swingsTTFKeys.add(lowestSwingLow.getIndex());
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
		
		if (swingsTTFKeys.size() < 2) {
			debug("Not enough swing points to compute correction");
			return;
		}
		
		int swing1Index = swingsTTFKeys.get(swingsTTFKeys.size() - 2);
		int swing2Index = swingsTTFKeys.get(swingsTTFKeys.size() - 1);
		
		SwingPoint swing1 = null;
		SwingPoint swing2 = null;
		
		for (SwingPoint swing : swingsLTF) {
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
			
			SwingPoint lastSwing = null;
			
			for (SwingPoint swing : swingsLTF) {
				if (currentTrend == "up") {
					if (swing.isTop()) lastSwing = swing;
				} else if (currentTrend == "down") {
					if (swing.isBottom()) lastSwing = swing;
				}
			}
			
			if (lastSwing != null) {
				if (currentTrend == "up") {
					debug(String.format("BUY @ %.5f", lastSwing.getValue()));
					stopPrice = (float)lastSwing.getValue();
					orderAction = Enums.OrderAction.BUY;
					//ctx.signal(lastSwing.getIndex(), Signals.BUY_STOP, "BUY", series.getClose(lastSwing.getIndex()));
				} else if (currentTrend == "down") {
					debug(String.format("SELL @ %.5f", lastSwing.getValue()));
					stopPrice = (float)lastSwing.getValue();
					orderAction = Enums.OrderAction.SELL;
					ctx.signal(lastSwing.getIndex(), Signals.SELL_STOP, "SELL", series.getClose(lastSwing.getIndex()));
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
		debug(String.format("Tick Size: %.5f", tickSize));
		debug(String.format("Spread: %.5f", spread));
		
		if (orderAction == Enums.OrderAction.BUY) {
			stopPrice += (tickSize + spread);
		} else if (orderAction == Enums.OrderAction.SELL) {
			stopPrice -= (tickSize + spread);
		}
		
		debug(String.format("Stop Price After: %.5f", stopPrice));
		
		ctx.cancelOrders();
		
		currentOrder = ctx.createStopOrder(orderAction, Enums.TIF.GTC, lots, stopPrice);
	}
	
	@Override
	public void onBarClose(DataContext ctx) {
		clear();
		
		DataSeries series = ctx.getDataSeries();
		
		List<SwingPoint> swingsHigh = computeSwings(series, true, Inputs.UP_MARKER, Enums.Position.TOP, "Swing High");
		List<SwingPoint> swingsLow = computeSwings(series, false, Inputs.DOWN_MARKER, Enums.Position.BOTTOM, "Swing Low");
		
		mergeSwings(swingsHigh, swingsLow);
		
		deleteNeighborSwings();
		
		drawLTFMarkersAndLines();
		computeTrend(true);
		
		drawTTFMarkersAndLines();
		computeTrend(false);
		
		computeRetraction(series);
		prepareTrade(ctx, series);

		super.onBarClose(ctx);
	}
	
	@Override
	public void onBarUpdate(OrderContext ctx) {
		if (invalidatedZone && currentOrder != null) {
			ctx.cancelOrders();
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
		
		super.onBarUpdate(ctx);
	}
}
