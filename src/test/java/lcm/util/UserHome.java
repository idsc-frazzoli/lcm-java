// code by jph
package lcm.util;

import java.io.File;

enum UserHome {
  ;
  public static File file(String filename) {
    return new File(System.getProperty("user.home"), filename);
  }

  public static File Pictures(String filename) {
    File directory = file("Pictures");
    directory.mkdir();
    return new File(directory, filename);
  }
}
