// code by jph
package lcm.logging;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

public class LogPlayerTest extends TestCase {
  public void testSimple() throws Exception {
    URL url = LogPlayerTest.class.getResource("/log/lcmlog-2017-08-24.04");
    File file = new File(url.getFile());
    assertTrue(file.exists());
    // ---
    LogPlayerConfig logPlayerConfig = new LogPlayerConfig();
    logPlayerConfig.logFile = url.getFile();
    logPlayerConfig.speed_numerator = 1;
    logPlayerConfig.speed_denominator = 64;
    // ---
    LogPlayer logPlayer = LogPlayer.create(logPlayerConfig);
    logPlayer.jFrame.setBounds(100, 100, 600, 400);
    logPlayer.standalone();
    Thread.sleep(2000);
    logPlayer.jFrame.dispose();
  }
}
