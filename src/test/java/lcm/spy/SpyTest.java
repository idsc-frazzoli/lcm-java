// code by jph
package lcm.spy;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;
import lcm.logging.LogPlayer;
import lcm.logging.LogPlayerConfig;
import lcm.logging.LogPlayerTest;

public class SpyTest extends TestCase {
  public void testSimple() throws Exception {
    Spy spy = new Spy("");
    URL url = LogPlayerTest.class.getResource("/log/lcmlog-2017-08-24.04");
    File file = new File(url.getFile());
    assertTrue(file.exists());
    // ---
    LogPlayerConfig logPlayerConfig = new LogPlayerConfig();
    logPlayerConfig.logFile = url.getFile();
    logPlayerConfig.speed_numerator = 1;
    logPlayerConfig.speed_denominator = 3;
    // ---
    LogPlayer logPlayer = LogPlayer.create(logPlayerConfig);
    logPlayer.jFrame.setBounds(100, 100, 600, 400);
    logPlayer.standalone();
    Thread.sleep(3000);
    logPlayer.jFrame.dispose();
    spy.jFrame.dispose();
  }
}
