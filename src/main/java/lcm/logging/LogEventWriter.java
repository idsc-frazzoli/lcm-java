// code by jph
package lcm.logging;

import java.io.File;
import java.io.IOException;

import lcm.logging.Log.Event;

public class LogEventWriter {
  private final Log log;
  private long count = -1;

  public LogEventWriter(File file) throws IOException {
    log = new Log(file.getPath(), "rw");
  }

  public void write(Event event) throws IOException {
    Event le = new Event();
    le.utime = event.utime;
    le.data = event.data;
    le.channel = event.channel;
    le.eventNumber = ++count;
    log.write(le);
  }

  public void close() throws IOException {
    log.close();
  }
}
