// code by jph
package lcm.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class ClassDump implements ClassVisitor {
  private final BufferedWriter bufferedWriter;
  private int count = 0;

  public ClassDump(BufferedWriter bufferedWriter) {
    this.bufferedWriter = bufferedWriter;
  }

  @Override
  public void classFound(String jarfile, Class<?> cls) {
    ++count;
    try {
      bufferedWriter.write(jarfile + " " + cls.getName());
      bufferedWriter.newLine();
    } catch (Exception exception) {
      // ---
    }
  }

  public static void main(String[] args) {
    File file = UserHome.file("classdump.txt");
    System.out.println("exporting to " + file.getAbsolutePath());
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
      ClassDump classDump = new ClassDump(bufferedWriter);
      ClassDiscoverer.findClasses(classDump);
      System.out.println("found " + classDump.count + " classes");
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }
}
