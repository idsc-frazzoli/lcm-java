// code by lcm
package lcm.util;

@FunctionalInterface
public interface ClassVisitor {
  /** @param jarfile
   * @param cls */
  void classFound(String jarfile, Class<?> cls);
}