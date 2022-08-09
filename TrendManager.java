package ricardo_franco;

import java.util.List;

import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.SwingPoint;

public class TrendManager {
	boolean onWave2;
	int wave2Index;
	String currentTrend;
	
	double retraction;
	double retraction50;
	double retraction618;
	boolean validRetraction;
	boolean reachedZone;
	boolean invalidatedZone;
	
	public TrendManager() {
		this.onWave2 = false;
		this.wave2Index = 0;
		this.currentTrend = null;
		
		this.retraction = 0;
		this.retraction50 = 0;
		this.retraction618 = 0;
		this.validRetraction = false;
		this.reachedZone = false;
		this.invalidatedZone = false;
	}
	
	public void update(List<SwingPoint> swingsLTF, List<Integer> swingsTTFKeys, DataSeries series) {
		this.computeTrend(swingsLTF, swingsTTFKeys, true);
		this.computeTrend(swingsLTF, swingsTTFKeys, false);
		
		this.computeRetraction(swingsLTF, swingsTTFKeys, series);
	}
	
	//----------------------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------------------
	
	protected void computeTrend(List<SwingPoint> swingsLTF, List<Integer> swingsTTFKeys, boolean ltfTrend) {
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
	
	protected void computeRetraction(List<SwingPoint> swingsLTF, List<Integer> swingsTTFKeys, DataSeries series) {
		validRetraction = false;
		
		//debug(String.format("On Wave 2? %b", onWave2));
		if (!onWave2) return;
		
		if (swingsTTFKeys.size() < 2) {
			//debug("Not enough swing points to compute correction");
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
			//debug("Not enough swing points to compute correction");
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
}
