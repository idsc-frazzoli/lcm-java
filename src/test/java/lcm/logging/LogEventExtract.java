// code by jph
package lcm.logging;

import java.io.File;

import lcm.logging.Log.Event;

// TODO make this a proper class
enum LogEventExtract {
  ;
  public static void main(String[] args) throws Exception {
    File src = new File("/media/datahaki/backup/gokartlogs/20171207", "20171207T105632_59f9bc78.lcm.00");
    File dst = new File("/home/datahaki", "20171207T105632_59f9bc78.lcm.00_part1");
    int lo = 7481997;
    int hi = 8090113;
    // ---
    Log log = new Log(src.toString(), "r");
    LogEventWriter logWriter = new LogEventWriter(dst);
    try {
      while (true) {
        Event event = log.readNext();
        if (lo < event.eventNumber && event.eventNumber < hi) {
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
