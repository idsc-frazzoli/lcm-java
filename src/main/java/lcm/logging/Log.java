// code by lcm
package lcm.logging;

import java.io.IOException;
import java.util.Arrays;

import lcm.lcm.LCMDataOutputStream;
import lcm.lcm.LCMEncodable;
import lcm.util.BufferedRandomAccessFile;

/** A class for reading and writing LCM log files. */
public class Log implements AutoCloseable {
  static final int LOG_MAGIC = 0xEDA1DA01;
  // ---
  private final BufferedRandomAccessFile raf;
  private final String path;
  /* Used to count the number of messages written so far. */
  long numMessagesWritten = 0;

  /** Represents a single received LCM message. */
  public static class Event {
    /** Time of message receipt, represented in microseconds since 00:00:00
     * UTC January 1, 1970. */
    public long utime;
    /** Event number assigned to the message in the log file. */
    public long eventNumber;
    /** Raw data bytes of the message body. */
    public byte[] data;
    /** Channel on which the message was received. */
    public String channel;
  }

  /** Opens a log file for reading or writing.
   *
   * @param path
   * the filename to open
   * @param mode
   * Specifies the access mode, must be one of "r", "rw", "rws", or
   * "rwd". See {@link java.io.RandomAccessFile#RandomAccessFile
   * RandomAccessFile} for more detail. */
  public Log(String path, String mode) throws IOException {
    this.path = path;
    raf = new BufferedRandomAccessFile(path, mode);
  }

  /** Retrieves the path to the log file.
   * 
   * @return the path to the log file */
  public String getPath() {
    return path;
  }

  /** Flush any unwritten data to the underlying file descriptor. **/
  public void flush() throws IOException {
    raf.flush();
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
      int v = raf.readByte() & 0xff; // typically the cause of an exception at EOF
      magic = (magic << 8) | v;
      if (magic != LOG_MAGIC)
        continue;
      event.eventNumber = raf.readLong();
      event.utime = raf.readLong();
      channellen = raf.readInt();
      datalen = raf.readInt();
      if (channellen <= 0 || datalen <= 0 || channellen >= 256 || datalen >= 16 * 1024 * 1024) {
        System.out.printf("Bad log event eventnumber = 0x%08x utime = 0x%08x channellen = 0x%08x datalen=0x%08x\n", event.eventNumber, event.utime, channellen,
            datalen);
        continue;
      }
      break;
    }
    byte bchannel[] = new byte[channellen];
    event.data = new byte[datalen];
    raf.readFully(bchannel);
    event.channel = new String(bchannel);
    raf.readFully(event.data);
    return event;
  }

  public synchronized double getPositionFraction() {
    return raf.getFilePointer() / ((double) raf.length());
  }

  /** Seek to a position in the log file, specified by a fraction.
   *
   * @param frac
   * a number in the range [0, 1) */
  public synchronized void seekPositionFraction(double frac) throws IOException {
    raf.seek((long) (raf.length() * frac));
  }

  /** Writes an event to the log file. The user is responsible for filling in
   * the eventNumber field, which should be sequentially increasing integers
   * starting with 0. */
  public synchronized void write(Event event) throws IOException {
    byte[] channelb = event.channel.getBytes();
    raf.writeInt(LOG_MAGIC);
    raf.writeLong(event.eventNumber);
    raf.writeLong(event.utime);
    raf.writeInt(channelb.length);
    raf.writeInt(event.data.length);
    raf.write(channelb, 0, channelb.length);
    raf.write(event.data, 0, event.data.length);
  }

  /** A convenience method for write. It internally manages the eventNumber
   * field, and so calls to this method should not be mixed with calls to the
   * other write methods. **/
  public synchronized void write(long utime, String channel, LCMEncodable msg) throws IOException {
    Log.Event le = new Log.Event();
    le.utime = utime;
    le.channel = channel;
    LCMDataOutputStream outs = new LCMDataOutputStream();
    msg.encode(outs);
    le.data = outs.toByteArray();
    le.eventNumber = numMessagesWritten;
    write(le);
    ++numMessagesWritten;
  }
  
  public synchronized void write(long utime, String channel, byte[] data) throws IOException {
    Log.Event le = new Log.Event();
    le.utime = utime;
    le.channel = channel;
    le.data = Arrays.copyOf(data, data.length);
    le.eventNumber = numMessagesWritten;
    write(le);
    ++numMessagesWritten;
  }

  /** Closes the log file and releases and system resources used by it. */
  @Override
  public synchronized void close() throws IOException {
    raf.close();
  }

  /** Helper function for log rotation
   * 
   * @return length of logfile in bytes
   * @throws IOException
   * @author Jen Wei */
  public synchronized long length() throws IOException {
    return raf.length();
  }
}
