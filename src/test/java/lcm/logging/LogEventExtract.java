// code by jph
package lcm.logging;

import java.io.File;

import lcm.logging.Log.Event;

// TODO make this a proper class
enum LogEventExtract {
  ;
  public static void main(String[] args) throws Exception {
    File file = new File("/media/datahaki/media/ethz/lcmlog", "20171207T134930_59f9bc78.lcm.00");
    File dst = new File("/media/datahaki/media/ethz/lcmlog", "20171207T134930_59f9bc78.lcm.00_part1");
    Log log = new Log(file.toString(), "r");
    LogEventWriter logWriter = new LogEventWriter(dst);
    try {
      while (true) {
        Event event = log.readNext();
        if (2236384 < event.eventNumber && event.eventNumber < 2897446) {
          logWriter.write(event);
        }
      }
    } catch (Exception exception) {
      System.err.println(exception.getMessage());
      // ---
    }
    logWriter.close();
  }
}
