package ricardo_franco;

import java.util.ArrayList;
import java.util.List;

import com.motivewave.platform.sdk.common.Enums;
import com.motivewave.platform.sdk.common.MarkerInfo;
import com.motivewave.platform.sdk.common.PathInfo;
import com.motivewave.platform.sdk.common.SwingPoint;
import com.motivewave.platform.sdk.draw.Line;
import com.motivewave.platform.sdk.draw.Marker;


public class GraphicManager {
    MarkerInfo ltfMarker;
    MarkerInfo ttfMarker;
    MarkerInfo htfMarker;

    PathInfo ltfLine;
    PathInfo ttfLine;
    PathInfo htfLine;

    MarkerInfo changeOfTrendMarker;

    public GraphicManager(MarkerInfo ltfMarker, MarkerInfo ttfMarker, MarkerInfo htfMarker, PathInfo ltfLine, PathInfo ttfLine, PathInfo htfLine, MarkerInfo changeOfTrendMarker) {
        this.ltfMarker = ltfMarker;
        this.ttfMarker = ttfMarker;
        this.htfMarker = htfMarker;

        this.ltfLine = ltfLine;
        this.ttfLine = ttfLine;
        this.htfLine = htfLine;

        this.changeOfTrendMarker = changeOfTrendMarker;
    }

    public List<Marker> getLTFMarkers(List<SwingPoint> swings) {
        List<Marker> markers = new ArrayList<Marker>();

        for (SwingPoint swing : swings) {
            if (swing.isTop()) {
                markers.add(this.getLTFMarker(swing, Enums.Position.TOP));
            } else {
                markers.add(this.getLTFMarker(swing, Enums.Position.BOTTOM));
            }
        }

        return markers;
    }

    public List<Marker> getTTFMarkers(List<SwingPoint> swings) {
        List<Marker> markers = new ArrayList<Marker>();

        for (SwingPoint swing : swings) {
            if (swing.isTop()) {
                markers.add(this.getTTFMarker(swing, Enums.Position.TOP));
            } else {
                markers.add(this.getTTFMarker(swing, Enums.Position.BOTTOM));
            }
        }

        return markers;
    }

    public List<Marker> getHTFMarkers(List<SwingPoint> swings) {
        List<Marker> markers = new ArrayList<Marker>();

        for (SwingPoint swing : swings) {
            if (swing.isTop()) {
                markers.add(this.getHTFMarker(swing, Enums.Position.TOP));
            } else {
                markers.add(this.getHTFMarker(swing, Enums.Position.BOTTOM));
            }
        }

        return markers;
    }

    public List<Line> getLTFLines(List<SwingPoint> swings) {
        List<Line> lines = new ArrayList<Line>();

        SwingPoint lastSwingHigh = null;
        SwingPoint lastSwingLow = null;

        for (SwingPoint swing : swings) {
            if (swing.isTop()) {
                if (lastSwingLow != null) {
                    lines.add(this.getLTFLine(lastSwingLow, swing));
                }
                lastSwingHigh = swing;
            } else {
                if (lastSwingHigh != null) {
                    lines.add(this.getLTFLine(lastSwingHigh, swing));
                }
                lastSwingLow = swing;
            }
        }

        return lines;
    }

    public List<Line> getTTFLines(List<SwingPoint> swings) {
        List<Line> lines = new ArrayList<Line>();

        SwingPoint lastSwingHigh = null;
        SwingPoint lastSwingLow = null;

        for (SwingPoint swing : swings) {
            if (swing.isTop()) {
                if (lastSwingLow != null) {
                    lines.add(this.getTTFLine(lastSwingLow, swing));
                }
                lastSwingHigh = swing;
            } else {
                if (lastSwingHigh != null) {
                    lines.add(this.getTTFLine(lastSwingHigh, swing));
                }
                lastSwingLow = swing;
            }
        }

        return lines;
    }

    public List<Line> getHTFLines(List<SwingPoint> swings) {
        List<Line> lines = new ArrayList<Line>();

        SwingPoint lastSwingHigh = null;
        SwingPoint lastSwingLow = null;

        for (SwingPoint swing : swings) {
            if (swing.isTop()) {
                if (lastSwingLow != null) {
                    lines.add(this.getHTFLine(lastSwingLow, swing));
                }
                lastSwingHigh = swing;
            } else {
                if (lastSwingHigh != null) {
                    lines.add(this.getHTFLine(lastSwingHigh, swing));
                }
                lastSwingLow = swing;
            }
        }

        return lines;
    }

    public List<Marker> getChangeOfTrendMarkers(List<SwingPoint> swings) {
        List<Marker> markers = new ArrayList<Marker>();

        for (SwingPoint swing : swings) {
            if (swing.isTop()) {
                markers.add(this.getChangeOfTrendMarker(swing, Enums.Position.TOP));
            } else {
                markers.add(this.getChangeOfTrendMarker(swing, Enums.Position.BOTTOM));
            }
        }

        return markers;
    }

    //----------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------

    protected Marker getLTFMarker(SwingPoint swing, Enums.Position position) {
        return this.getMarker(swing, this.ltfMarker, position);
    }

    protected Marker getTTFMarker(SwingPoint swing, Enums.Position position) {
        return this.getMarker(swing, this.ttfMarker, position);
    }

    protected Marker getHTFMarker(SwingPoint swing, Enums.Position position) {
        return this.getMarker(swing, this.htfMarker, position);
    }

    protected Line getLTFLine(SwingPoint swing1, SwingPoint swing2) {
        return this.getLine(swing1, swing2, this.ltfLine);
    }

    protected Line getTTFLine(SwingPoint swing1, SwingPoint swing2) {
        return this.getLine(swing1, swing2, this.ttfLine);
    }

    protected Line getHTFLine(SwingPoint swing1, SwingPoint swing2) {
        return this.getLine(swing1, swing2, this.htfLine);
    }

    protected Marker getChangeOfTrendMarker(SwingPoint swing, Enums.Position position) {
        return this.getMarker(swing, this.changeOfTrendMarker, position);
    }

    protected Marker getMarker(SwingPoint swing, MarkerInfo marker, Enums.Position position) {
        if (marker.isEnabled()) {
            return new Marker(swing.getCoordinate(), position, marker, String.format("Swing #%d", swing.getIndex()));
        }

        return null;
    }

    protected Line getLine(SwingPoint swing1, SwingPoint swing2, PathInfo line) {
        if (line.isEnabled()) {
            return new Line(swing1.getCoordinate(), swing2.getCoordinate(), line);
        }

        return null;
    }
}
