// code by jph
package lcm.logging;

/** configuration for launch of {@link LogPlayer} */
public class LogPlayerConfig {
  public String lcmurl = null;
  public String logFile = null;
  public boolean startPaused = false;
  public int optind;
  public String channelFilterRegex = null;
  public boolean invertChannelFilter = false;
}
