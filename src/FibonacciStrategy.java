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
import com.motivewave.platform.sdk.common.desc.DoubleDescriptor;
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
import com.motivewave.platform.sdk.order_mgmt.Order;
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
	
	final static String FIXED_SL_PIPS = "FixedSLPips";
	final static String RRR = "RiskRewardRatio";

    final static String LTF_STRENGTH = "ltfStrength";
    final static String TTF_STRENGTH = "ttfStrength";
    final static String HTF_STRENGTH = "htfStrength";

    final static String LTF_MARKER = "ltfMarker";
    final static String TTF_MARKER = "ttfMarker";
    final static String HTF_MARKER = "htfMarker";

    final static String LTF_LINE = "ltfLine";
    final static String TTF_LINE = "ttfLine";
    final static String HTF_LINE = "htfLine";

    final static String CHANGE_OF_TREND_MARKER = "changeOfTrendMarker";

    SwingManager ltfSwingManager;
    SwingManager ttfSwingManager;
    SwingManager htfSwingManager;

	TrendManager trendManager;
	GraphicManager graphicManager;
	OrderManager orderManager;


	public void initialize(Defaults defaults) {
	    SettingsDescriptor sd = new SettingsDescriptor();
	    setSettingsDescriptor(sd);

	    SettingTab tab = new SettingTab("General");
	    sd.addTab(tab);

	    SettingGroup inputs = new SettingGroup("Inputs");
	    inputs.addRow(new IntegerDescriptor(LTF_STRENGTH, "LTF Swing Point Strength", 2, 2, 9999, 1));
        inputs.addRow(new IntegerDescriptor(TTF_STRENGTH, "TTF Swing Point Strength", 10, 2, 9999, 1));
        inputs.addRow(new IntegerDescriptor(HTF_STRENGTH, "HTF Swing Point Strength", 50, 2, 9999, 1));
	    inputs.addRow(new IntegerDescriptor(FIXED_SL_PIPS, "Fixed Stop Loss Pips", 10, 5, 100, 1));
	    inputs.addRow(new DoubleDescriptor(RRR, "Risk Reward Ratio", 1, 0.5, 10, 0.5));
	    tab.addGroup(inputs);
	    
	    SettingGroup lines = new SettingGroup("Display");
	    
	    lines.addRow(new PathDescriptor(LTF_LINE, "LTF Line", defaults.getBlue(), 1.0f, null, true, true, true));
	    lines.addRow(new MarkerDescriptor(LTF_MARKER, "LTF Marker", 
	            Enums.MarkerType.CIRCLE, Enums.Size.SMALL, defaults.getBlue(), defaults.getLineColor(), true, true));
	    
	    lines.addRow(new PathDescriptor(TTF_LINE, "TTF Line", defaults.getRed(), 2.0f, null, true, true, true));
	    lines.addRow(new MarkerDescriptor(TTF_MARKER, "TTF Marker", 
	            Enums.MarkerType.CIRCLE, Enums.Size.MEDIUM, defaults.getRed(), defaults.getLineColor(), true, true));

		lines.addRow(new PathDescriptor(HTF_LINE, "HTF Line", defaults.getGrey(), 4.0f, null, true, true, true));
		lines.addRow(new MarkerDescriptor(HTF_MARKER, "HTF Marker",
				Enums.MarkerType.CIRCLE, Enums.Size.LARGE, defaults.getGrey(), defaults.getLineColor(), true, true));

        lines.addRow(new MarkerDescriptor(CHANGE_OF_TREND_MARKER, "Change Of Trend Marker",
                Enums.MarkerType.DIAMOND, Enums.Size.LARGE, defaults.getYellow(), defaults.getLineColor(), true, true));
	    
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
				getSettings().getMarker(HTF_MARKER),
				getSettings().getPath(LTF_LINE),
				getSettings().getPath(TTF_LINE),
				getSettings().getPath(HTF_LINE),
                getSettings().getMarker(CHANGE_OF_TREND_MARKER));

		this.ltfSwingManager = new SwingManager(this, getSettings().getInteger(LTF_STRENGTH));
        this.ttfSwingManager = new SwingManager(this, getSettings().getInteger(TTF_STRENGTH));
        this.htfSwingManager = new SwingManager(this, getSettings().getInteger(HTF_STRENGTH));

        this.trendManager = new TrendManager(this, ttfSwingManager, ltfSwingManager);
	}
	
	@Override
	public void onActivate(OrderContext ctx) {
		this.orderManager = new OrderManager(
				this, ttfSwingManager, trendManager, ctx,
				getSettings().getTradeLots(),
				getSettings().getInteger(FIXED_SL_PIPS), getSettings().getDouble(RRR)
		);
	}
	
	public void drawMarkersAndLines() {
        // HTF
        for (Line line : this.graphicManager.getHTFLines(this.htfSwingManager.swings)) {
            if (line != null) addFigure(line);
        }
        for (Marker marker : this.graphicManager.getHTFMarkers(this.htfSwingManager.swings)) {
            if (marker != null) addFigure(marker);
        }

		// TTF
        for (Line line : this.graphicManager.getTTFLines(this.ttfSwingManager.swings)) {
            if (line != null) addFigure(line);
        }
        for (Marker marker : this.graphicManager.getTTFMarkers(this.ttfSwingManager.swings)) {
            if (marker != null) addFigure(marker);
        }

        // LTF
        for (Line line : this.graphicManager.getLTFLines(this.ltfSwingManager.swings)) {
            if (line != null) addFigure(line);
        }
        for (Marker marker : this.graphicManager.getLTFMarkers(this.ltfSwingManager.swings)) {
            if (marker != null) addFigure(marker);
        }

        // TTF Changes of Trend
        for (Marker marker : this.graphicManager.getChangeOfTrendMarkers(this.trendManager.changeOfTrendSwings)) {
            if (marker != null) addFigure(marker);
        }
	}
	
	public void clear() {
		this.ltfSwingManager.clear();
        this.ttfSwingManager.clear();
        this.htfSwingManager.clear();
        this.trendManager.clear();
		clearFigures();
	}
	
	@Override
	public void onBarClose(DataContext ctx) {
		clear();
		
		DataSeries series = ctx.getDataSeries();
		
		this.ltfSwingManager.update(series);
        this.ttfSwingManager.update(series);
        this.htfSwingManager.update(series);
		this.trendManager.update(series);
		
		drawMarkersAndLines();
		
		this.orderManager.update(series);

		super.onBarClose(ctx);
	}

	@Override
	public void onOrderFilled(OrderContext ctx, Order order) {
		this.orderManager.onOrderFilled(order);
	}

	@Override
	public void onOrderCancelled(OrderContext ctx, Order order) {
		this.orderManager.onOrderCancelled(order);
	}

	@Override
	public void onOrderRejected(OrderContext ctx, Order order) {
		this.orderManager.onOrderCancelled(order);
	}
}
