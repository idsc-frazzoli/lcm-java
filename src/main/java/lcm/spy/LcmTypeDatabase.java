// code by lcm
package lcm.spy;

import java.util.HashMap;
import java.util.Map;

/** Searches classpath for objects that implement LCSpyPlugin using reflection. */
public class LcmTypeDatabase {
  private Map<Long, Class<?>> classes = new HashMap<>();

  /* package */ LcmTypeDatabase() {
  }

  /* package */ void put(long fingerprint, Class<?> cls) {
    classes.put(fingerprint, cls);
  }

  /** @return number of discovered lcm types */
  public int size() {
    return classes.size();
  }

  public Class<?> getClassByFingerprint(long fingerprint) {
    return classes.get(fingerprint);
  }
}
