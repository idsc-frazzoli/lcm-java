// code by lcm
// modifications by jph
package lcm.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

/** implementation is standalone */
public class BufferedRandomAccessFile {
  private static final int BUFFER_SIZE = 32768; // must be power of two!
  // ---
  private final RandomAccessFile randomAccessFile;
  /** buffer needs to be written back to disk? (If true, reads MUST use buffer.) */
  private boolean bufferDirty = false;
  private byte[] buffer = new byte[BUFFER_SIZE];
  /** what file offset does this buffer start at? */
  private long bufferOffset = -1;
  /** how many bytes of the buffer are valid? ( < BUFFER_SIZE near end of file) */
  private int bufferLength = -1;
  /** current file position in the buffer [0, BUFFER_SIZE-1] */
  private int bufferPosition = -1;
  /** length of the file */
  private long fileLength;

  /** Invariant: the current file position = bufferOffset + bufferPosition.
   * This position is always stored inside the buffer, or this position is the
   * byte after the current buffer (in which case the next read will re-fill
   * the buffer. */
  public BufferedRandomAccessFile(String path, String mode) throws IOException {
    randomAccessFile = new RandomAccessFile(path, mode);
    fileLength = randomAccessFile.length();
    bufferSeek(0);
  }

  public void close() throws IOException {
    flushBuffer();
    randomAccessFile.close();
  }

  public long getFilePointer() {
    return bufferOffset + bufferPosition;
  }

  public long length() {
    return fileLength;
  }

  private static long min(long a, long b) {
    return a < b ? a : b;
  }

  public void seek(long pos) throws IOException {
    bufferSeek(pos);
  }

  public void flush() throws IOException {
    flushBuffer();
  }

  /** Writes the buffer if it contains any dirty data */
  void flushBuffer() throws IOException {
    if (!bufferDirty)
      return;
    randomAccessFile.seek(bufferOffset);
    randomAccessFile.write(buffer, 0, bufferLength);
    bufferDirty = false;
  }

  /** Performs a seek and fills the buffer accordingly. **/
  void bufferSeek(long seekOffset) throws IOException {
    flushBuffer();
    long newOffset = seekOffset - (seekOffset & (BUFFER_SIZE - 1L));
    if (newOffset == bufferOffset) {
      bufferPosition = (int) (seekOffset - bufferOffset);
      return;
    }
    bufferOffset = newOffset;
    bufferLength = (int) min(BUFFER_SIZE, fileLength - bufferOffset);
    if (bufferLength < 0)
      bufferLength = 0;
    bufferPosition = (int) (seekOffset - bufferOffset);
    // we always ask for an amount that should be exactly available.
    randomAccessFile.seek(bufferOffset);
    randomAccessFile.readFully(buffer, 0, bufferLength);
    // System.out.printf("%08x %08x %08x %08x\n", seekOffset, bufferOffset,
    // bufferPosition, bufferLength);
  }

  public final int read() throws IOException {
    if (bufferOffset + bufferPosition >= fileLength)
      throw new EOFException("EOF");
    if (bufferPosition >= bufferLength)
      bufferSeek(bufferOffset + bufferPosition);
    return buffer[bufferPosition++] & 0xff;
  }

  public boolean hasMore() {
    return bufferPosition + bufferOffset < fileLength;
  }

  public byte peek() throws IOException {
    if (bufferPosition < bufferLength)
      return buffer[bufferPosition];
    randomAccessFile.seek(bufferOffset + bufferPosition);
    return randomAccessFile.readByte();
  }

  public void write(int v) throws IOException {
    write((byte) (v & 0xff));
  }

  public void writeBoolean(boolean b) throws IOException {
    write((byte) (b ? 1 : 0));
  }

  public boolean readBoolean() throws IOException {
    return read() != 0;
  }

  public void writeShort(short v) throws IOException {
    write((byte) (v >> 8));
    write((byte) (v & 0xff));
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
    while (length > 0) {
      int bufferAvailable = bufferLength - bufferPosition;
      int thiscopy = Math.min(bufferAvailable, length);
      if (thiscopy == 0) {
        flushBuffer();
        if (bufferOffset + bufferPosition >= fileLength)
          throw new EOFException("EOF");
        bufferSeek(bufferOffset + bufferLength);
        continue;
      }
      System.arraycopy(buffer, bufferPosition, b, offset, thiscopy);
      bufferPosition += thiscopy;
      offset += thiscopy;
      length -= thiscopy;
    }
  }

  public void readFully(byte[] b) throws IOException {
    readFully(b, 0, b.length);
  }

  public void writeInt(long v) throws IOException {
    write((byte) (v >> 24));
    write((byte) (v >> 16));
    write((byte) (v >> 8));
    write((byte) (v & 0xff));
  }

  public int readInt() throws IOException {
    int v = 0;
    v |= (read() << 24);
    v |= (read() << 16);
    v |= (read() << 8);
    v |= (read());
    return v;
  }

  public void writeLong(long v) throws IOException {
    write((byte) (v >> 56));
    write((byte) (v >> 48));
    write((byte) (v >> 40));
    write((byte) (v >> 32));
    write((byte) (v >> 24));
    write((byte) (v >> 16));
    write((byte) (v >> 8));
    write((byte) (v & 0xff));
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

  public void writeFloat(float f) throws IOException {
    writeInt(Float.floatToIntBits(f));
  }

  public float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());
  }

  public void writeDouble(double f) throws IOException {
    writeLong(Double.doubleToLongBits(f));
  }

  public double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());
  }

  public void write(byte src[], int offset, int writelen) throws IOException {
    for (int i = offset; i < offset + writelen; ++i)
      write(src[i]);
  }

  public void write(byte v) throws IOException {
    bufferDirty = true;
    // they're doing a write within our current buffer.
    if (bufferPosition < bufferLength) {
      buffer[bufferPosition++] = v;
      return;
    }
    // they're increasing the size of the file, but it still fits inside our buffer
    if (bufferLength < BUFFER_SIZE) {
      buffer[bufferPosition++] = v;
      bufferLength++;
      fileLength++;
      return;
    }
    // they're doing a write, but we're out of buffer.
    flushBuffer();
    bufferSeek(bufferOffset + bufferPosition);
    write(v);
  }
}
