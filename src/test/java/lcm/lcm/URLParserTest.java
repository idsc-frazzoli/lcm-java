// code by jph
package lcm.lcm;

import junit.framework.TestCase;

public class URLParserTest extends TestCase {
  public void testSimple() {
    String[] args = new String[] { "http://blub.html" };
    URLParser u = null;
    if (args.length < 1) {
      String env = System.getenv("LCM_DEFAULT_URL");
      if (null != env)
        u = new URLParser(env);
      else {
        System.err.println("Must specify URL");
        System.exit(1);
      }
    } else {
      u = new URLParser(args[0]);
    }
    assertEquals(u.get("protocol"), "http");
    assertEquals(u.get("network"), "blub.html");
  }
}
