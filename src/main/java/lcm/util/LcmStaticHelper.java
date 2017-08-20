// code by lcm
// extracted by jph
package lcm.util;

public enum LcmStaticHelper {
  ;
  /** check if the JRE is supplied by gcj, and warn the user if it is */
  public static void checkJre() {
    if (System.getProperty("java.vendor").indexOf("Free Software Foundation") >= 0) {
      System.err.println("WARNING: Detected gcj. lcm-spy is not known to work well with gcj.");
      System.err.println("         The Sun JRE is recommended.");
    }
  }
}
