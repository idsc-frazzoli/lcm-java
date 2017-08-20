// code by jph
package lcm.spy;

import junit.framework.TestCase;

public class LCMTypeDatabaseTest extends TestCase {
  public void testSimple() {
    LCMTypeDatabase db = LCMTypeDatabase.create();
    assertTrue(0 < db.size());
  }
}
