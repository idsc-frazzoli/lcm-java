package lcm.spy;

import java.lang.reflect.Field;
import java.util.HashMap;

import lcm.util.ClassDiscoverer;

/** Searches classpath for objects that implement LCSpyPlugin using reflection. **/
public class LCMTypeDatabase {
  @SuppressWarnings("rawtypes")
  HashMap<Long, Class> classes = new HashMap<Long, Class>();

  public LCMTypeDatabase() {
    ClassDiscoverer.findClasses(new MyClassVisitor());
    System.out.println("Found " + classes.size() + " LCM types");
  }

  class MyClassVisitor implements ClassDiscoverer.ClassVisitor {
    @SuppressWarnings("rawtypes")
    public void classFound(String jar, Class cls) {
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
  }

  @SuppressWarnings("rawtypes")
  public Class getClassByFingerprint(long fingerprint) {
    return classes.get(fingerprint);
  }
}
