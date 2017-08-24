// code by jph
package lcm.util;

import java.net.URL;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ClassPaths {
  ;
  private static String join(String... paths) {
    return Stream.of(paths).filter(Objects::nonNull) //
        .collect(Collectors.joining(System.getProperty("path.separator")));
  }

  /** original classpath used in implementation by lcm
   * 
   * @return */
  public static String getDefault() {
    return join(System.getenv("CLASSPATH"), System.getProperty("java.class.path"));
  }

  /** used in swisstrolley+ project
   * 
   * @return */
  public static String getResource() {
    URL url = ClassDiscovery.class.getResource("/");
    return join(System.getenv("CLASSPATH"), url.getPath());
  }

  public static void main(String[] args) {
    String string = System.getenv("CLASSPATH");
    System.out.println(string);
    System.out.println(System.getProperty("java.class.path"));
    System.out.println(join("b", null, "asd"));
    System.out.println(getResource());
    System.out.println("DEFAULT=" + getDefault());
  }
}
