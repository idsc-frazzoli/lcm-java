// code by lcm
package lcm.spy;

import lcm.spy.ObjectPanel.Section;

/** Data about an individual sparkline. */
class SparklineData {
  int xmin, xmax;
  int ymin, ymax;
  boolean isHovering;
  String name;
  Section section;
  /** we keep track of the drawing iteration number for each line
   * to let us figure out if the line is currently being drawn
   * when the user clicks it. This is needed to fix a bug where the
   * user clicks in a place a line used to be, but is no longer
   * there since the array it was in got shorter. */
  int lastDrawNumber = 0;
}