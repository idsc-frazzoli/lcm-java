// code by jph
package lcm.spy;

import junit.framework.TestCase;

public class SpyTest extends TestCase {
  public void testSimple() throws Exception {
    Spy spy = new Spy("");
    Thread.sleep(2000);
    spy.jFrame.dispose();
  }
}
