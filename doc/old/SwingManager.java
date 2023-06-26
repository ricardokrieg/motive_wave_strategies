package ricardo_franco;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.SwingPoint;


public class SwingManager {
	FibonacciStrategy study;

	int strength;
	
	List<SwingPoint> swingsLTF;
	List<Integer> swingsTTFKeys;
	List<Integer> swingsHTFKeys;
	
	public SwingManager(FibonacciStrategy study, int strength) {
		this.study = study;

		this.strength = strength;
		
		this.swingsLTF = new ArrayList<SwingPoint>();
		this.swingsTTFKeys = new ArrayList<Integer>();
        this.swingsHTFKeys = new ArrayList<Integer>();
	}
	
	public void update(DataSeries series) {
		List<SwingPoint> swingsHigh = this.computeSwings(series, true);
		List<SwingPoint> swingsLow = this.computeSwings(series, false);
		
		this.swingsLTF = mergeSwings(swingsHigh, swingsLow);
		this.swingsLTF = deleteNeighborSwings(this.swingsLTF);
	}
	
	public void clear() {
		this.swingsLTF.clear();
		this.swingsTTFKeys.clear();
        this.swingsHTFKeys.clear();
	}
	
	//----------------------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------------------
	
	protected List<SwingPoint> computeSwings(DataSeries series, boolean top) {
		return castList(SwingPoint.class, series.calcSwingPoints(top, this.strength));
	}
	
	protected List<SwingPoint> mergeSwings(List<SwingPoint> swings1, List<SwingPoint> swings2) {
		Map<Integer, SwingPoint> mergedSwings = new HashMap<Integer, SwingPoint>();
		List<SwingPoint> tempSwings = new ArrayList<SwingPoint>();
		
		for (SwingPoint swing : swings1) {
			mergedSwings.put(swing.getIndex(), swing);
		}
		
		for (SwingPoint swing : swings2) {
			mergedSwings.put(swing.getIndex(), swing);
		}
		
		SortedSet<Integer> keys = new TreeSet<Integer>(mergedSwings.keySet());
		for (Integer key : keys) {
			SwingPoint swing = mergedSwings.get(key);
			
			tempSwings.add(swing);
		}
		
		return tempSwings;
	}
	
	protected List<SwingPoint> deleteNeighborSwings(List<SwingPoint> swings) {
		List<Integer> keysToKeep = new ArrayList<Integer>();
		List<Integer> keysToIgnore = new ArrayList<Integer>();
		
		for (SwingPoint swing : swings) {
			if (keysToIgnore.contains(swing.getIndex())) continue;
			
			double strength = swing.getAvgStrength();
			Integer strengthKey = swing.getIndex();
			
			for (SwingPoint swingToCompare : swings) {
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
		
		for (SwingPoint swing : swings) {
			int key = swing.getIndex();
			if (keysToKeep.contains(key)) {
				tempSwingsLTF.add(swing);
			}
		}
		
		return tempSwingsLTF;
	}
	
	protected static <T> List<T> castList(Class<? extends T> clazz, Collection<?> c) {
	    List<T> r = new ArrayList<T>(c.size());
	    for(Object o: c)
	      r.add(clazz.cast(o));
	    return r;
	}
}
