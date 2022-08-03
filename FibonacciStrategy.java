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
 strategy=false)
public class FibonacciStrategy extends Study {
	enum Values { MA };
	
	List<SwingPoint> swingsLTF = new ArrayList<SwingPoint>();
	List<Integer> swingsTTFKeys = new ArrayList<Integer>();
	
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
	
	public void drawLine(SwingPoint swing1, SwingPoint swing2, String lineName, int waveCount) {
		PathInfo line = getSettings().getPath(lineName);
		if (line.isEnabled()) {
			if (waveCount == 1 || waveCount == 2 || waveCount == 3) {
				line = new PathInfo(line.getColor(), line.getWidth() * 2,
						line.getDash(), line.isEnabled(), line.isContinuous(),
						line.isShowAsBars(), line.getBarCenter(), line.getFixedWidth());
			}
			
			addFigure(
					new Line(swing1.getCoordinate(), swing2.getCoordinate(), line));
		}
	}
	
	public void drawLTFMarkersAndLines() {
		SwingPoint lastSwingHigh = null;
		SwingPoint lastSwingLow = null;
		
		for (SwingPoint swing : swingsLTF) {
			if (swing.isTop()) {
				drawMarker(swing, Inputs.UP_MARKER, Enums.Position.TOP);
				
				if (lastSwingLow != null) drawLine(lastSwingLow, swing, Inputs.PATH, 0);
				lastSwingHigh = swing;
			} else {
				drawMarker(swing, Inputs.DOWN_MARKER, Enums.Position.BOTTOM);
				
				if (lastSwingHigh != null) drawLine(lastSwingHigh, swing, Inputs.PATH, 0);
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
				
				if (lastSwingLow != null) drawLine(lastSwingLow, swing, "TTFLine", 1);
				lastSwingHigh = swing;
			} else {
				drawMarker(swing, "TTFSLMarker", Enums.Position.BOTTOM);
				
				if (lastSwingHigh != null) drawLine(lastSwingHigh, swing, "TTFLine", 0);
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
		String currentTrend = null;
		SwingPoint lastSwingHigh = null;
		SwingPoint lastSwingLow = null;
		SwingPoint highestSwingHigh = null;
		SwingPoint lowestSwingLow = null;
		SwingPoint leadingSwingHigh = null;
		SwingPoint leadingSwingLow = null;
		
		for (SwingPoint swing : swingsLTF) {
			if (!ltfTrend) {
				if (!swingsTTFKeys.contains(swing.getIndex())) continue;
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
									if (highestSwingHigh != null)
										swingsTTFKeys.add(highestSwingHigh.getIndex());									
								}
								
								lowestSwingLow = swing;
								leadingSwingLow = null;
								leadingSwingHigh = lastSwingHigh;
								
								currentTrend = "down";
								if (!ltfTrend) {
									debug(String.format("Change to DOWN trend #%d", swing.getIndex()));
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
									if (lowestSwingLow != null)
										swingsTTFKeys.add(lowestSwingLow.getIndex());
								}
								
								highestSwingHigh = swing;
								leadingSwingHigh = null;
								leadingSwingLow = lastSwingLow;
								
								currentTrend = "up";
								if (!ltfTrend) {
									debug(String.format("Change to UP trend #%d", swing.getIndex()));
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
	
	@Override
	protected void calculate(int index, DataContext ctx) {
		DataSeries series = ctx.getDataSeries();

		if (!series.isBarComplete(index)) return;
		series.setComplete(index);
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
		
		debug("TTF Keys");
		for (int key : swingsTTFKeys) {
			debug(String.format("%d", key));
		}
	}
}
