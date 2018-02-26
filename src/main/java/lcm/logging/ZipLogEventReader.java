package lcm.logging;

import java.io.IOException;

import lcm.logging.Log.Event;

/** Class for reading compressed LCM log files. Use similar to {@link Log}.
 * 
 * @author anritter */
public class ZipLogEventReader {
  static final int LOG_MAGIC = 0xEDA1DA01;
  // ---
  private BufferedZipInputStream zis;
  private final String path;

  /** Opens a compressed log file for reading.
   *
   * @param path
   * the filename to open */
  public ZipLogEventReader(String path) throws IOException {
    this.path = path;
    zis = new BufferedZipInputStream(path);
  }

  /** Retrieves the path to the log file.
   * 
   * @return the path to the log file */
  public String getPath() {
    return path;
  }

  /** Reads the next event in the log file
   *
   * @throws java.io.EOFException
   * if the end of the file has been reached. */
  public synchronized Event readNext() throws IOException {
    int magic = 0;
    Event event = new Event();
    int channellen = 0, datalen = 0;
    while (true) {
      int v = zis.read() & 0xff; // typically the cause of an
                                 // exception at EOF
      magic = (magic << 8) | v;
      if (magic != LOG_MAGIC)
        continue;
      event.eventNumber = zis.readLong();
      event.utime = zis.readLong();
      channellen = zis.readInt();
      datalen = zis.readInt();
      if (channellen <= 0 || datalen <= 0 || channellen >= 256 || datalen >= 16 * 1024 * 1024) {
        System.out.printf("Bad log event eventnumber = 0x%08x utime = 0x%08x channellen = 0x%08x datalen=0x%08x\n", event.eventNumber, event.utime, channellen,
            datalen);
        continue;
      }
      break;
    }
    byte bchannel[] = new byte[channellen];
    event.data = new byte[datalen];
    zis.readFully(bchannel);
    event.channel = new String(bchannel);
    zis.readFully(event.data);
    return event;
  }

  /** Closes the log file and releases and system resources used by it. */
  public synchronized void close() throws IOException {
    zis.close();
  }
}
