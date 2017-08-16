package lcm.spy;

import java.util.LinkedList;
import java.util.List;

/** Global class allowing multiple charts to know about each other and make
 * intelligent decisions based on that.
 * 
 * Also ensures that we do not try to create two charts based on the same data
 * backend, which can cause conflicts.
 * 
 * @author abarry */
public class ChartData {
  private long startuTime; // start time of lcm-spy, which all X-axis are
                           // based off of
  // list of all charts displayed
  private List<ZoomableChartScrollWheel> charts = new LinkedList<>();
  // constants for setting how much data we keep for each type of graph
  public final int sparklineChartSize = 500;
  public final int detailedSparklineChartSize = 1500;

  /** Constructor for ChartData. Initializes color list and sets the start time
   * of lcm-spy
   * 
   * @param startuTime
   * lcm-spy start time to base each x-axis off of */
  public ChartData(long startuTime) {
    this.startuTime = startuTime;
  }

  /** Returns all charts being displayed
   * 
   * @return all chrats being displayed */
  public List<ZoomableChartScrollWheel> getCharts() {
    return charts;
  }

  /** Get start time in microseconds.
   * 
   * @return start time of lcm-spy in microseconds */
  public long getStartTime() {
    return startuTime;
  }
}
