// code by jph
package lcm.util;

/** list every class in class path */
enum ClassDiscoveryDemo {
  ;
  public static void main(String args[]) {
    ClassVisitor classVisitor = new ClassVisitor() {
      @Override
      public void classFound(String jarfile, Class<?> cls) {
        System.out.printf("%-30s %s\n", jarfile, cls);
      }
    };
    ClassDiscovery.execute(ClassPaths.getDefault(), classVisitor);
  }
}
