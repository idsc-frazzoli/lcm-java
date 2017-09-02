// code by jph
package lcm.util;

import junit.framework.TestCase;

public class ClassPathsTest extends TestCase {
  public void testSimple() {
    assertEquals(ClassPaths.join("b", null, "asd"), "b:asd");
  }
}
