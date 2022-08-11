package ricardo_franco;

import java.util.ArrayList;
import java.util.List;

import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.Enums;
import com.motivewave.platform.sdk.common.Inputs;
import com.motivewave.platform.sdk.common.SwingPoint;
import com.motivewave.platform.sdk.common.Tick;
import com.motivewave.platform.sdk.common.desc.IntegerDescriptor;
import com.motivewave.platform.sdk.common.desc.MarkerDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.common.desc.SettingGroup;
import com.motivewave.platform.sdk.common.desc.SettingTab;
import com.motivewave.platform.sdk.common.desc.SettingsDescriptor;
import com.motivewave.platform.sdk.common.desc.ValueDescriptor;
import com.motivewave.platform.sdk.draw.Line;
import com.motivewave.platform.sdk.draw.Marker;
import com.motivewave.platform.sdk.order_mgmt.OrderContext;
import com.motivewave.platform.sdk.study.RuntimeDescriptor;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;


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
	TrendManager trendManager;
	
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
		this.trendManager = new TrendManager(this);
	}
	
	@Override
	public void onActivate(OrderContext ctx) {
		this.orderManager = new OrderManager(this, ctx, getSettings().getTradeLots());
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
	
	@Override
	public void onBarClose(DataContext ctx) {
		clear();
		
		DataSeries series = ctx.getDataSeries();
		
		this.swingManager.update(series);
		this.trendManager.update(series);
		
		drawMarkersAndLines();
		
		this.orderManager.update(series, series.getClose());

		super.onBarClose(ctx);
	}
}
