// code by lcm
package lcm.spy;

/** Global class allowing multiple charts to know about each other and make
 * intelligent decisions based on that.
 * 
 * Also ensures that we do not try to create two charts based on the same data
 * backend, which can cause conflicts.
 * 
 * @author abarry */
public class ChartData {
  /** constants for setting how much data we keep for each type of graph */
  public static final int SPARKLINECHARTSIZE = 500;
  public static final int SPARKLINECHARTSIZE_DETAILED = 1500;
  // ---
  /** start time of lcm-spy, which all X-axis are based off of */
  private long startuTime;

  /** Constructor for ChartData. Initializes color list and sets the start time
   * of lcm-spy
   * 
   * @param startuTime
   * lcm-spy start time to base each x-axis off of */
  public ChartData(long startuTime) {
    this.startuTime = startuTime;
  }

  /** Get start time in microseconds.
   * 
   * @return start time of lcm-spy in microseconds */
  public long getStartTime() {
    return startuTime;
  }
}
