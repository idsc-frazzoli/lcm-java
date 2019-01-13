package lcm.logging;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lcm.util.BufferedRandomAccessFile;

/** Class for reading compressed input stream sequentially. Similar to
 * {@link BufferedRandomAccessFile} for {@link Log}, but for
 * {@link ZipLogEventReader}.
 * 
 * @author anritter */
public class BufferedZipInputStream {
  private final ZipInputStream zipInputStream;

  public BufferedZipInputStream(String path) throws IOException {
    zipInputStream = new ZipInputStream(new FileInputStream(path));
    ZipEntry zipEntry = null;
    while ((zipEntry = zipInputStream.getNextEntry()) != null)
      // entry name must be equal to file name of path
      if (path.endsWith(zipEntry.getName() + ".zip"))
        break;
  }

  public void close() throws IOException {
    zipInputStream.close();
  }

  public final int read() throws IOException {
    int byteRead = zipInputStream.read();
    if (byteRead == -1)
      throw new EOFException("EOF");
    return byteRead & 0xff;
  }

  public boolean readBoolean() throws IOException {
    return read() != 0;
  }

  public byte readByte() throws IOException {
    return (byte) read();
  }

  public short readShort() throws IOException {
    short v = 0;
    v |= (read() << 8);
    v |= (read());
    return v;
  }

  public void readFully(byte[] b, int offset, int length) throws IOException {
    int numRead = zipInputStream.read(b, offset, length);
    if (numRead == -1)
      throw new EOFException("EOF");
    if (numRead != length) // happens. Read again remaining bytes.
      readFully(b, offset + numRead, length - numRead);
  }

  public void readFully(byte[] b) throws IOException {
    readFully(b, 0, b.length);
  }

  public int readInt() throws IOException {
    int v = 0;
    v |= (read() << 24);
    v |= (read() << 16);
    v |= (read() << 8);
    v |= (read());
    return v;
  }

  public long readLong() throws IOException {
    long v = 0;
    v |= (((long) read()) << 56);
    v |= (((long) read()) << 48);
    v |= (((long) read()) << 40);
    v |= (((long) read()) << 32);
    v |= (((long) read()) << 24);
    v |= (((long) read()) << 16);
    v |= (((long) read()) << 8);
    v |= read();
    return v;
  }

  public float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());
  }

  public double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());
  }
}
