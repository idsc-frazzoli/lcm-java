// code by jph
package lcm.util;

enum ClassPathsDemo {
  ;
  public static void main(String[] args) {
    String string = System.getenv("CLASSPATH");
    System.out.println(string);
    System.out.println(System.getProperty("java.class.path"));
    System.out.println("REDUCED=" + ClassPaths.getResource());
    System.out.println("DEFAULT=" + ClassPaths.getDefault());
  }
}
