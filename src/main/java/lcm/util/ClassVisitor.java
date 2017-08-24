// code by lcm
package lcm.util;

public interface ClassVisitor {
  public void classFound(String jarfile, Class<?> cls);
}