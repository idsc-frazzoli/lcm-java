// code by lcm
// adapted by jph
package lcm.logging;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;

import lcm.util.LcmStaticHelper;

public class LogPlayer {
  public static LogPlayer create(LogPlayerConfig cfg) throws IOException {
    return new LogPlayer(cfg);
  }
  // ---

  public final LogPlayerComponent logPlayerComponent;
  public final JFrame jFrame = new JFrame("LogPlayer");

  private LogPlayer(LogPlayerConfig cfg) throws IOException {
    logPlayerComponent = new LogPlayerComponent(cfg.lcmurl, cfg.speed);
    jFrame.setLayout(new BorderLayout());
    jFrame.add(logPlayerComponent, BorderLayout.CENTER);
    jFrame.pack();
    jFrame.setSize(jFrame.getWidth(), 300);
    jFrame.setVisible(true);
    jFrame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        try {
          logPlayerComponent.savePreferences();
        } catch (IOException ex) {
          System.out.println("Couldn't save preferences: " + ex);
        }
        // FIXME interrupt all threads and make a clean exit!
        System.exit(0);
      }
    });
    if (cfg.channelFilterRegex != null)
      logPlayerComponent.setChannelFilter(cfg.channelFilterRegex);
    if (cfg.invertChannelFilter)
      logPlayerComponent.invertChannelFilter();
    if (cfg.logFile != null)
      logPlayerComponent.setLog(cfg.logFile, !cfg.startPaused);
    else
      logPlayerComponent.openDialog();
  }

  public static void main(String args[]) throws IOException {
    LcmStaticHelper.checkJre();
    LogPlayerConfig cfg = new LogPlayerConfig();
    for (cfg.optind = 0; cfg.optind < args.length; cfg.optind++) {
      String c = args[cfg.optind];
      if (c.equals("-h") || c.equals("--help")) {
        StaticHelper.usage();
      } else if (c.equals("-l") || c.equals("--lcm-url") || c.startsWith("--lcm-url=")) {
        String optarg = null;
        if (c.startsWith("--lcm-url=")) {
          optarg = c.split("=")[1];
        } else if (cfg.optind < args.length) {
          cfg.optind++;
          optarg = args[cfg.optind];
        }
        if (null == optarg) {
          StaticHelper.usage();
        } else {
          cfg.lcmurl = optarg;
        }
      } else if (c.equals("-p") || c.equals("--paused")) {
        cfg.startPaused = true;
      } else if (c.equals("-f") || c.equals("--filter") || c.startsWith("--filter=")) {
        String optarg = null;
        if (c.startsWith("--filter=")) {
          optarg = c.split("=")[1];
        } else if (cfg.optind < args.length) {
          cfg.optind++;
          optarg = args[cfg.optind];
        }
        if (null == optarg) {
          StaticHelper.usage();
        } else {
          cfg.channelFilterRegex = optarg;
        }
      } else if (c.equals("-v") || c.equals("--invert-filter")) {
        cfg.invertChannelFilter = true;
      } else if (c.startsWith("-")) {
        StaticHelper.usage();
      } else if (cfg.logFile != null) // there should only be 1 non-flag argument
        StaticHelper.usage();
      else {
        cfg.logFile = c;
      }
    }
    create(cfg);
  }
}
