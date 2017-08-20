// code by lcm
package lcm.lcm;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;

/** Will not throw EOF. **/
public final class LCMDataInputStream implements DataInput {
  byte buf[];
  int pos = 0; // current index into buf.
  int len; // number of valid bytes in buffer
  int startpos; // index of first valid byte
  int endpos; // index of byte after last valid byte

  public LCMDataInputStream(byte buf[]) {
    this.buf = buf;
    this.endpos = buf.length + 1;
  }

  public LCMDataInputStream(byte buf[], int offset, int len) {
    this.buf = buf;
    this.pos = offset;
    this.startpos = offset;
    this.endpos = offset + len + 1;
  }

  void needInput(int need) throws EOFException {
    if (pos + need >= endpos)
      throw new EOFException("LCMDataInputStream needed " + need + " bytes, only " + available() + " available.");
  }

  public int available() {
    return endpos - pos - 1;
  }

  public void close() {
  }

  public void reset() {
    pos = startpos;
  }

  @Override
  public boolean readBoolean() throws IOException {
    needInput(1);
    return (buf[pos++] != 0);
  }

  @Override
  public byte readByte() throws IOException {
    needInput(1);
    return buf[pos++];
  }

  @Override
  public int readUnsignedByte() throws IOException {
    needInput(1);
    return buf[pos++] & 0xff;
  }

  @Override
  public char readChar() throws IOException {
    return (char) readShort();
  }

  @Override
  public short readShort() throws IOException {
    needInput(2);
    return (short) (((buf[pos++] & 0xff) << 8) | ((buf[pos++] & 0xff) << 0));
  }

  @Override
  public int readUnsignedShort() throws IOException {
    needInput(2);
    return ((buf[pos++] & 0xff) << 8) | ((buf[pos++] & 0xff) << 0);
  }

  @Override
  public int readInt() throws IOException {
    needInput(4);
    return ((buf[pos++] & 0xff) << 24) | ((buf[pos++] & 0xff) << 16) | ((buf[pos++] & 0xff) << 8) | ((buf[pos++] & 0xff) << 0);
  }

  @Override
  public long readLong() throws IOException {
    needInput(8);
    return ((buf[pos++] & 0xffL) << 56) | ((buf[pos++] & 0xffL) << 48) | ((buf[pos++] & 0xffL) << 40) | ((buf[pos++] & 0xffL) << 32)
        | ((buf[pos++] & 0xffL) << 24) | ((buf[pos++] & 0xffL) << 16) | ((buf[pos++] & 0xffL) << 8) | ((buf[pos++] & 0xffL) << 0);
  }

  @Override
  public float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());
  }

  @Override
  public void readFully(byte b[]) throws IOException {
    needInput(b.length);
    System.arraycopy(buf, pos, b, 0, b.length);
    pos += b.length;
  }

  @Override
  public void readFully(byte b[], int off, int len) throws IOException {
    needInput(len);
    System.arraycopy(buf, pos, b, off, len);
    pos += len;
  }

  /** Writes chars as one byte per char, filling high byte with zero. **/
  public void readFullyBytesAsChars(char c[]) throws IOException {
    needInput(c.length);
    for (int i = 0; i < c.length; i++)
      c[i] = (char) (buf[pos++] & 0xff);
  }

  @Override
  public double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());
  }

  @Override
  public String readLine() throws IOException {
    StringBuffer sb = new StringBuffer();
    while (true) {
      needInput(1);
      byte v = buf[pos++];
      if (v == 0)
        break;
      sb.append((char) v);
    }
    return sb.toString();
  }

  /** Read a string of 8-bit characters terminated by a zero. The zero is
   * consumed. **/
  public String readStringZ() throws IOException {
    StringBuffer sb = new StringBuffer();
    while (true) {
      int v = buf[pos++] & 0xff;
      if (v == 0)
        break;
      sb.append((char) v);
    }
    return sb.toString();
  }

  @Override
  public String readUTF() throws IOException {
    assert (false);
    return null;
  }

  @Override
  public int skipBytes(int n) {
    pos += n;
    return n;
  }

  /** Returns the internal buffer representation. **/
  public byte[] getBuffer() {
    return buf;
  }

  /** Returns the current position in the internal buffer representation. **/
  public int getBufferOffset() {
    return pos;
  }
}
