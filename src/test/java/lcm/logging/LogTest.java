// code by jph
package lcm.logging;

import junit.framework.TestCase;

public class LogTest extends TestCase {
  public void testSimple() {
    assertEquals(Log.LOG_MAGIC, 0xEDA1DA01);
  }
}
