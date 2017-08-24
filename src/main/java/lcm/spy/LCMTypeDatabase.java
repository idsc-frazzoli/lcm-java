// code by lcm
package lcm.spy;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import lcm.util.ClassDiscoverer;
import lcm.util.ClassVisitor;

/** Searches classpath for objects that implement LCSpyPlugin using reflection. **/
public class LCMTypeDatabase implements ClassVisitor {
  public static LCMTypeDatabase create() {
    LCMTypeDatabase lcmTypeDatabase = new LCMTypeDatabase();
    ClassDiscoverer.findClasses(lcmTypeDatabase);
    System.out.println("Found " + lcmTypeDatabase.size() + " LCM types");
    return lcmTypeDatabase;
  }

  @SuppressWarnings("rawtypes")
  private Map<Long, Class> classes = new HashMap<>();

  private LCMTypeDatabase() {
  }

  /** @return number of discovered lcm types */
  public int size() {
    return classes.size();
  }

  @Override
  public void classFound(String jar, Class<?> cls) {
    try {
      Field[] fields = cls.getFields();
      for (Field f : fields) {
        if (f.getName().equals("LCM_FINGERPRINT")) {
          // it's a static member, we don't need an instance
          long fingerprint = f.getLong(null);
          classes.put(fingerprint, cls);
          // System.out.printf("%016x : %s\n", fingerprint, cls);
          break;
        }
      }
    } catch (IllegalAccessException ex) {
      System.out.println("Bad LCM Type? " + ex);
    }
  }

  @SuppressWarnings("rawtypes")
  public Class getClassByFingerprint(long fingerprint) {
    return classes.get(fingerprint);
  }
}
