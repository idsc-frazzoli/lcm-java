// code by lcm
package lcm.logging;

interface JScrubberListener {
  void scrubberMovedByUser(JScrubber js, double x);

  void scrubberPassedRepeat(JScrubber js, double from_pos, double to_pos);

  void scrubberExportRegion(JScrubber js, double p0, double p1);
}
