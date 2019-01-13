// code by jph
package lcm.logging;

/** configuration for launch of {@link LogPlayer} */
public class LogPlayerConfig {
  /** path to log file to be opened with the LogPlayer
   * /media/datahaki/media/ethz/gokart/topic/localization/20181211T153939_3/log.lcm */
  public String logFile = null;
  /***************************************************/
  public int speed_numerator = 1;
  public int speed_denominator = 1;
  public String lcmurl = null;
  public boolean startPaused = false;
  public int optind;
  public String channelFilterRegex = null;
  public boolean invertChannelFilter = false;

  /** @return fraction numerator / denominator */
  /* package */ BigFraction speed() {
    return BigFraction.of(speed_numerator, speed_denominator);
  }
}
