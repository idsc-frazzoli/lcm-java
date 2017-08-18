package lcm.util;

public interface ClassVisitor {
  @SuppressWarnings("rawtypes")
  public void classFound(String jarfile, Class cls);
}