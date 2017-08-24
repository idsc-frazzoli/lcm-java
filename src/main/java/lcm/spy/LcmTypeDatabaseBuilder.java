// code adapted by jph
package lcm.spy;

import java.lang.reflect.Field;

import lcm.util.ClassDiscovery;
import lcm.util.ClassVisitor;

public class LcmTypeDatabaseBuilder implements ClassVisitor {
  ;
  public static LcmTypeDatabase create(String classpath) {
    LcmTypeDatabaseBuilder instance = new LcmTypeDatabaseBuilder();
    ClassDiscovery.execute(classpath, instance);
    LcmTypeDatabase lcmTypeDatabase = instance.lcmTypeDatabase;
    if (lcmTypeDatabase.size() == 0) {
      System.err.println("no lcm types found in:");
      System.out.println(classpath);
    } else
      System.out.println("Found " + lcmTypeDatabase.size() + " LCM types");
    return lcmTypeDatabase;
  }
  // ---

  private final LcmTypeDatabase lcmTypeDatabase = new LcmTypeDatabase();

  private LcmTypeDatabaseBuilder() {
  }

  @Override
  public void classFound(String jar, Class<?> cls) {
    try {
      Field[] fields = cls.getFields();
      for (Field field : fields) {
        if (field.getName().equals("LCM_FINGERPRINT")) {
          // it's a static member, we don't need an instance
          long fingerprint = field.getLong(null);
          lcmTypeDatabase.put(fingerprint, cls);
          // System.out.printf("%016x : %s\n", fingerprint, cls);
          break;
        }
      }
    } catch (IllegalAccessException ex) {
      System.out.println("Bad LCM Type? " + ex);
    }
  }
}
