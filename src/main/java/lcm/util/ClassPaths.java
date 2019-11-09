// code by jph
package lcm.util;

import java.net.URL;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ClassPaths {
  ;
  /** @param paths
   * @return concatenation of paths to a single class path */
  static String join(String... paths) {
    return Stream.of(paths) //
        .filter(Objects::nonNull) //
        .collect(Collectors.joining(System.getProperty("path.separator")));
  }

  /** @return original classpath used in implementation by lcm */
  public static String getDefault() {
    return join(System.getenv("CLASSPATH"), System.getProperty("java.class.path"));
  }

  /** @return reduced class path used for instance in swisstrolley+ project */
  public static String getResource() {
    URL url = ClassDiscovery.class.getResource("/");
    return join(System.getenv("CLASSPATH"), url.getPath());
  }
}
