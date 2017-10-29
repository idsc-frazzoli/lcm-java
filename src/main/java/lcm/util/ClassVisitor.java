// code by lcm
package lcm.util;

public interface ClassVisitor {
  void classFound(String jarfile, Class<?> cls);
}