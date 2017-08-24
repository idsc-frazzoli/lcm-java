// code by jph
package lcm.spy;

import junit.framework.TestCase;
import lcm.util.ClassPaths;

public class LcmTypeDatabaseTest extends TestCase {
  public void testSimple() {
    LcmTypeDatabase db = LcmTypeDatabaseBuilder.create(ClassPaths.getDefault());
    assertTrue(0 < db.size());
  }
}
