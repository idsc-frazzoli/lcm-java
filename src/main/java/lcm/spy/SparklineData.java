// code by lcm
package lcm.spy;

import info.monitorenter.gui.chart.Chart2D;
import lcm.spy.ObjectPanel.Section;

/** Data about an individual sparkline. */
class SparklineData {
  int xmin, xmax;
  int ymin, ymax;
  boolean isHovering;
  /** all sparklines have a chart associated with them, even though
   * we do not use it for display. This allows us to use the
   * data-collection and management features */
  Chart2D chart;
  String name;
  Section section;
  /** we keep track of the drawing iteration number for each line
   * to let us figure out if the line is currently being drawn
   * when the user clicks it. This is needed to fix a bug where the
   * user clicks in a place a line used to be, but is no longer
   * there since the array it was in got shorter. */
  int lastDrawNumber = 0;
}