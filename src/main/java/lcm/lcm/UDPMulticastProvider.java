// code by lcm
package lcm.lcm;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** LCM provider for the udpm: URL. All messages are broadcast over a
 * pre-arranged UDP multicast address. Subscription operations are a no-op,
 * since all messages are always broadcast.
 *
 * This mechanism is very simple, low-latency, and efficient due to not having
 * to transmit messages more than once when there are multiple subscribers.
 * Since it uses UDP, it is lossy. **/
public class UDPMulticastProvider implements Provider {
  private static final String DEFAULT_NETWORK = "239.255.76.67:7667";
  private static final int DEFAULT_TTL = 0;
  private static final int MAGIC_SHORT = 0x4c433032; // ascii of "LC02"
  private static final int MAGIC_LONG = 0x4c433033; // ascii of "LC03"
  private static final int FRAGMENTATION_THRESHOLD = 64000;
  static {
    System.setProperty("java.net.preferIPv4Stack", "true");
    System.err.println("LCM: Disabling IPV6 support");
  }
  // ---
  private MulticastSocket multicastSocket;
  private ReaderThread readerThread;
  private Map<SocketAddress, FragmentBuffer> fragBufs = new HashMap<>();
  private final LCM lcm;
  private final InetAddress inetAddr;
  private final int inetPort;
  private int msgSeqNumber = 0;

  public UDPMulticastProvider(LCM lcm, URLParser up) throws IOException {
    this.lcm = lcm;
    String addrport[] = up.get("network", DEFAULT_NETWORK).split(":");
    inetAddr = InetAddress.getByName(addrport[0]);
    inetPort = Integer.valueOf(addrport[1]);
    multicastSocket = new MulticastSocket(inetPort);
    multicastSocket.setReuseAddress(true);
    multicastSocket.setLoopbackMode(false); // true *disables* loopback
    int ttl = up.get("ttl", DEFAULT_TTL);
    if (ttl == 0)
      System.err.println("LCM: TTL set to zero, traffic will not leave localhost.");
    else if (ttl > 1)
      System.err.println("LCM: TTL set to > 1... That's almost never correct!");
    else
      System.err.println("LCM: TTL set to 1.");
    multicastSocket.setTimeToLive(up.get("ttl", DEFAULT_TTL));
    try {
      multicastSocket.joinGroup(inetAddr);
    } catch (Exception exception) {
      System.out.println(TroubleShooter.joinGroup());
      System.out.flush();
      throw exception;
    }
  }

  @Override
  public synchronized void publish(String channel, byte data[], int offset, int length) {
    try {
      publishEx(channel, data, offset, length);
    } catch (Exception ex) {
      System.err.println("ex: " + ex);
    }
  }

  @Override
  public synchronized void subscribe(String channel) {
    if (Objects.isNull(readerThread)) {
      readerThread = new ReaderThread();
      readerThread.start();
    }
  }

  @Override
  public void unsubscribe(String channel) {
    // deliberately empty
  }

  @Override
  public synchronized void close() {
    if (Objects.nonNull(readerThread)) {
      readerThread.interrupt();
      try {
        readerThread.join();
      } catch (InterruptedException ex) {
        // ---
      }
    }
    readerThread = null;
    multicastSocket.close();
    multicastSocket = null;
    fragBufs = null;
  }

  void publishEx(String channel, byte data[], int offset, int length) throws Exception {
    byte[] channel_bytes = channel.getBytes("US-ASCII");
    int payload_size = channel_bytes.length + length;
    if (payload_size <= FRAGMENTATION_THRESHOLD) {
      LCMDataOutputStream outs = new LCMDataOutputStream(length + channel.length() + 32);
      outs.writeInt(MAGIC_SHORT);
      outs.writeInt(msgSeqNumber);
      outs.writeStringZ(channel);
      outs.write(data, offset, length);
      multicastSocket.send(new DatagramPacket(outs.getBuffer(), 0, outs.size(), inetAddr, inetPort));
    } else {
      int nfragments = payload_size / FRAGMENTATION_THRESHOLD;
      if (payload_size % FRAGMENTATION_THRESHOLD > 0)
        nfragments++;
      if (nfragments > 65535) {
        System.err.println("LC error: too much data for a single message");
        return;
      }
      // first fragment is special. insert channel before data
      ByteArrayOutputStream bouts = new ByteArrayOutputStream(10 + FRAGMENTATION_THRESHOLD);
      DataOutputStream outs = new DataOutputStream(bouts);
      int fragment_offset = 0;
      int frag_no = 0;
      outs.writeInt(MAGIC_LONG);
      outs.writeInt(msgSeqNumber);
      outs.writeInt(length);
      outs.writeInt(fragment_offset);
      outs.writeShort(frag_no);
      outs.writeShort(nfragments);
      outs.write(channel_bytes, 0, channel_bytes.length);
      outs.writeByte(0);
      int firstfrag_datasize = FRAGMENTATION_THRESHOLD - (channel_bytes.length + 1);
      outs.write(data, offset, firstfrag_datasize);
      byte[] b = bouts.toByteArray();
      multicastSocket.send(new DatagramPacket(b, 0, b.length, inetAddr, inetPort));
      fragment_offset += firstfrag_datasize;
      for (frag_no = 1; frag_no < nfragments; frag_no++) {
        bouts = new ByteArrayOutputStream(10 + FRAGMENTATION_THRESHOLD);
        outs = new DataOutputStream(bouts);
        outs.writeInt(MAGIC_LONG);
        outs.writeInt(msgSeqNumber);
        outs.writeInt(length);
        outs.writeInt(fragment_offset);
        outs.writeShort(frag_no);
        outs.writeShort(nfragments);
        int fraglen = java.lang.Math.min(FRAGMENTATION_THRESHOLD, length - fragment_offset);
        outs.write(data, offset + fragment_offset, fraglen);
        b = bouts.toByteArray();
        multicastSocket.send(new DatagramPacket(b, 0, b.length, inetAddr, inetPort));
        fragment_offset += fraglen;
      }
    }
    ++msgSeqNumber;
  }

  class FragmentBuffer {
    final SocketAddress socketAddress;
    final String channel;
    final int msgSeqNumber;
    final int data_size;
    int fragments_remaining;
    final byte[] data;
    final boolean frag_received[];

    FragmentBuffer( //
        SocketAddress socketAddress, String channel, //
        int msgSeqNumber, int data_size, int fragments_remaining) {
      this.socketAddress = socketAddress;
      this.channel = channel;
      this.msgSeqNumber = msgSeqNumber;
      this.data_size = data_size;
      this.fragments_remaining = fragments_remaining;
      this.data = new byte[data_size];
      this.frag_received = new boolean[fragments_remaining];
    }
  }

  class ReaderThread extends Thread {
    ReaderThread() {
      setDaemon(true);
      setName("LCM-UDP"); // Added by Jen
    }

    @Override
    public void run() {
      DatagramPacket datagramPacket = new DatagramPacket(new byte[65536], 65536);
      while (!isInterrupted()) {
        try {
          multicastSocket.receive(datagramPacket);
          handlePacket(datagramPacket);
        } catch (IOException ex) {
          System.err.println("ex: " + ex);
          continue;
        }
      }
    }

    @Override
    public void interrupt() {
      super.interrupt();
      multicastSocket.close();
    }

    void handleShortMessage(DatagramPacket datagramPacket, LCMDataInputStream ins) throws IOException {
      @SuppressWarnings("unused")
      int msgSeqNumber = ins.readInt();
      String channel = ins.readStringZ();
      lcm.receiveMessage(channel, ins.getBuffer(), ins.getBufferOffset(), ins.available());
    }

    void handleFragment(DatagramPacket datagramPacket, LCMDataInputStream ins) throws IOException {
      int msgSeqNumber = ins.readInt();
      int msg_size = ins.readInt(); // & 0xffffffff;
      int fragment_offset = ins.readInt(); // & 0xffffffff;
      int fragment_id = ins.readShort() & 0xffff;
      int fragments_in_msg = ins.readShort() & 0xffff;
      // read entire packet payload
      byte payload[] = new byte[ins.available()];
      ins.readFully(payload);
      // if (0 < ins.available())
      // System.err.println("Unread data! " + ins.available());
      int data_start = 0;
      int frag_size = payload.length;
      SocketAddress socketAddress = datagramPacket.getSocketAddress();
      FragmentBuffer fragmentBuffer = fragBufs.get(socketAddress);
      // TODO arrangement of conditions not nice
      if (fragmentBuffer != null && ((fragmentBuffer.msgSeqNumber != msgSeqNumber) || (fragmentBuffer.data_size != msg_size))) {
        fragBufs.remove(fragmentBuffer.socketAddress);
        fragmentBuffer = null;
      }
      if (Objects.isNull(fragmentBuffer))
        if (0 == fragment_id) {
          // extract channel name
          int channel_len = 0;
          for (; channel_len < payload.length; ++channel_len)
            if (0 == payload[channel_len])
              break;
          data_start = channel_len + 1;
          frag_size -= channel_len + 1;
          String channel = new String(payload, 0, channel_len, "US-ASCII");
          fragmentBuffer = new FragmentBuffer(socketAddress, channel, msgSeqNumber, msg_size, fragments_in_msg);
          fragBufs.put(fragmentBuffer.socketAddress, fragmentBuffer);
        } else
          return;
      // ---
      if (fragmentBuffer.data_size < fragment_offset + frag_size) {
        System.err.println("LC: dropping invalid fragment");
        fragBufs.remove(fragmentBuffer.socketAddress);
        return;
      }
      if (!fragmentBuffer.frag_received[fragment_id]) {
        fragmentBuffer.frag_received[fragment_id] = true;
        System.arraycopy(payload, data_start, fragmentBuffer.data, fragment_offset, frag_size);
        --fragmentBuffer.fragments_remaining;
      }
      if (0 == fragmentBuffer.fragments_remaining) {
        lcm.receiveMessage(fragmentBuffer.channel, fragmentBuffer.data, 0, fragmentBuffer.data_size);
        fragBufs.remove(fragmentBuffer.socketAddress);
      }
    }

    void handlePacket(DatagramPacket datagramPacket) throws IOException {
      LCMDataInputStream ins = new LCMDataInputStream(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength());
      int magic = ins.readInt();
      if (magic == MAGIC_SHORT) {
        handleShortMessage(datagramPacket, ins);
      } else if (magic == MAGIC_LONG) {
        handleFragment(datagramPacket, ins);
      } else {
        System.err.println("bad magic: " + Integer.toHexString(magic));
      }
    }
  }
}
