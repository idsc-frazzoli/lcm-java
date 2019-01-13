// code by lcm
// adapted by jph
package lcm.logging;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import lcm.util.LcmStaticHelper;

public class LogPlayer {
  public static LogPlayer create(LogPlayerConfig cfg) throws IOException {
    return new LogPlayer(cfg);
  }
  // ---

  public final LogPlayerComponent logPlayerComponent;
  public final JFrame jFrame = new JFrame("LogPlayer");

  private LogPlayer(LogPlayerConfig logPlayerConfig) throws IOException {
    logPlayerComponent = new LogPlayerComponent(logPlayerConfig.lcmurl, logPlayerConfig.speed());
    jFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    jFrame.setLayout(new BorderLayout());
    jFrame.add(logPlayerComponent, BorderLayout.CENTER);
    jFrame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent windowEvent) {
        logPlayerComponent.stopThreads();
      }
    });
    if (logPlayerConfig.channelFilterRegex != null)
      logPlayerComponent.setChannelFilter(logPlayerConfig.channelFilterRegex);
    if (logPlayerConfig.invertChannelFilter)
      logPlayerComponent.invertChannelFilter();
    if (logPlayerConfig.logFile != null)
      logPlayerComponent.setLog(logPlayerConfig.logFile, !logPlayerConfig.startPaused);
    else
      logPlayerComponent.openDialog();
  }

  public void standalone() {
    jFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    jFrame.setVisible(true);
  }

  public static void main(String args[]) throws IOException {
    LcmStaticHelper.checkJre();
    LogPlayerConfig logPlayerConfig = new LogPlayerConfig();
    for (logPlayerConfig.optind = 0; logPlayerConfig.optind < args.length; logPlayerConfig.optind++) {
      String c = args[logPlayerConfig.optind];
      if (c.equals("-h") || c.equals("--help")) {
        StaticHelper.usage();
      } else //
      if (c.equals("-l") || c.equals("--lcm-url") || c.startsWith("--lcm-url=")) {
        String optarg = null;
        if (c.startsWith("--lcm-url=")) {
          optarg = c.split("=")[1];
        } else //
        if (logPlayerConfig.optind < args.length) {
          logPlayerConfig.optind++;
          optarg = args[logPlayerConfig.optind];
        }
        if (null == optarg) {
          StaticHelper.usage();
        } else {
          logPlayerConfig.lcmurl = optarg;
        }
      } else //
      if (c.equals("-p") || c.equals("--paused")) {
        logPlayerConfig.startPaused = true;
      } else //
      if (c.equals("-f") || c.equals("--filter") || c.startsWith("--filter=")) {
        String optarg = null;
        if (c.startsWith("--filter=")) {
          optarg = c.split("=")[1];
        } else //
        if (logPlayerConfig.optind < args.length) {
          logPlayerConfig.optind++;
          optarg = args[logPlayerConfig.optind];
        }
        if (null == optarg) {
          StaticHelper.usage();
        } else {
          logPlayerConfig.channelFilterRegex = optarg;
        }
      } else //
      if (c.equals("-v") || c.equals("--invert-filter")) {
        logPlayerConfig.invertChannelFilter = true;
      } else //
      if (c.startsWith("-")) {
        StaticHelper.usage();
      } else //
      if (logPlayerConfig.logFile != null) // there should only be 1 non-flag argument
        StaticHelper.usage();
      else {
        logPlayerConfig.logFile = c;
      }
    }
    create(logPlayerConfig);
  }
}
