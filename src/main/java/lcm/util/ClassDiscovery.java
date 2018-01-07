// code by lcm
// adapted by jph
package lcm.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassDiscovery {
  public static void execute(String classpath, ClassVisitor classVisitor) {
    new ClassDiscovery(classpath, classVisitor).findClasses();
  }

  // ---
  private final String classpath;
  private final ClassVisitor classVisitor;

  private ClassDiscovery(String classpath, ClassVisitor classVisitor) {
    this.classpath = classpath;
    this.classVisitor = classVisitor;
  }

  private void visitDirectory(URLClassLoader cldr, String classpath_entry, File dir, String visiting_classpath) {
    if (!dir.canRead())
      return;
    for (File file : dir.listFiles()) {
      if (!file.canRead())
        continue;
      String fname = file.getName();
      if (file.isDirectory()) {
        // found a directory. recursively traverse the directory and
        // search for .class files
        if (fname.contains("."))
          continue;
        // Modified by Jan in order to enable nested packages
        // visitDirectory(visitor, cldr, classpath_entry, f, fname +
        // ".");
        String vc = visiting_classpath.isEmpty() ? fname : visiting_classpath + "." + fname;
        visitDirectory(cldr, classpath_entry, file, vc);
      } else //
      if (file.isFile() && fname.endsWith(".class")) {
        // found a .class file. Construct its full classname and pass
        // it to the class visitor
        // Modified by Jan in order to enable nested packages
        // String cn = visiting_classpath + fname.substring(0,
        // fname.length()-6);
        String cn = visiting_classpath + "." + fname.substring(0, fname.length() - 6);
        try {
          Class<?> cls = cldr.loadClass(cn);
          if (cls != null)
            classVisitor.classFound(classpath_entry, cls);
        } catch (Throwable ex) {
          // ---
        }
      }
    }
  }

  /** Given a colon-delimited list of jar files, iterate over the classes in them. */
  private void findClasses() {
    final String ps = System.getProperty("path.separator");
    String[] items = classpath.split(ps);
    // Create a class loader that has access to the whole class path.
    URLClassLoader cldr;
    try {
      URL[] urls = new URL[items.length];
      for (int i = 0; i < items.length; ++i)
        urls[i] = new File(items[i]).toURI().toURL();
      cldr = new URLClassLoader(urls);
    } catch (IOException ex) {
      System.out.println("ClassDiscoverer ERR: " + ex);
      return;
    }
    for (int i = 0; i < items.length; ++i) {
      String item = items[i];
      if (item.endsWith(".jar")) {
        try (JarFile jf = new JarFile(item)) {
          for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements();) {
            JarEntry je = e.nextElement();
            String n = je.getName();
            // skip private classes?
            // if (n.contains("$"))
            // continue;
            if (n.endsWith(".class")) {
              // convert the path into a class name
              String cn = n.substring(0, n.length() - 6);
              cn = cn.replace('/', '.');
              cn = cn.replace('\\', '.');
              // try loading that class
              try {
                Class<?> cls = cldr.loadClass(cn);
                if (cls == null)
                  continue;
                classVisitor.classFound(item, cls);
              } catch (Throwable ex) {
                // System.out.println("ClassDiscoverer: "+ex);
                // System.out.println(" jar: "+item);
                // System.out.println(" class: "+n);
              }
            }
          }
        } catch (IOException ioe) {
          System.out.println("Error extracting " + items[i]);
        }
      } else {
        File file = new File(item);
        if (!file.isDirectory())
          continue;
        visitDirectory(cldr, item, file, "");
      }
    }
  }
}
