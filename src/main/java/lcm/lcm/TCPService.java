// code by lcm
package lcm.lcm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

// TODO class does not have a conventional close/stop function!
public class TCPService {
  private final ServerSocket serverSocket;
  private final AcceptThread acceptThread;
  private final List<ClientThread> clients = new ArrayList<>();
  private final ReadWriteLock clients_lock = new ReentrantReadWriteLock();
  private int bytesCount = 0;

  public TCPService(int port) throws IOException {
    serverSocket = new ServerSocket(port);
    // sock.setReuseAddress(true);
    // sock.setLoopbackMode(false); // true *disables* loopback
    acceptThread = new AcceptThread();
    acceptThread.start();
    long inittime = System.currentTimeMillis();
    long starttime = System.currentTimeMillis();
    while (!Thread.interrupted()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        break;
      }
      long endtime = System.currentTimeMillis();
      double dt = (endtime - starttime) / 1000.0;
      starttime = endtime;
      System.out.printf("%10.3f : %10.1f kB/s, %d clients\n", (endtime - inittime) / 1000.0, bytesCount / 1024.0 / dt, clients.size());
      bytesCount = 0;
    }
    // interrupt signal received
    closeResources();
  }

  private void closeResources() throws IOException {
    acceptThread.interrupt();
    serverSocket.close();
    synchronized (clients) {
      for (ClientThread clientThread : clients) {
        clientThread.closeResources();
      }
    }
  }

  public void relay(byte channel[], byte data[]) {
    // synchronously send to all clients.
    String chanstr = new String(channel);
    try {
      clients_lock.readLock().lock();
      for (ClientThread client : clients) {
        client.send(chanstr, channel, data);
      }
    } finally {
      clients_lock.readLock().unlock();
    }
  }

  class AcceptThread extends Thread {
    @Override
    public void run() {
      while (!Thread.interrupted()) {
        try {
          Socket clientSock = serverSocket.accept();
          ClientThread client = new ClientThread(clientSock);
          client.start();
          try {
            clients_lock.writeLock().lock();
            clients.add(client);
          } finally {
            clients_lock.writeLock().unlock();
          }
        } catch (IOException ex) {
          // ---
        }
      }
    }
  }

  class ClientThread extends Thread {
    Socket socket;
    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;

    class SubscriptionRecord {
      String regex;
      Pattern pattern;

      SubscriptionRecord(String regex) {
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
      }
    }

    private final List<SubscriptionRecord> subscriptions = new ArrayList<>();
    private final ReadWriteLock subscriptions_lock = new ReentrantReadWriteLock();

    public ClientThread(Socket socket) throws IOException {
      this.socket = socket;
      dataInputStream = new DataInputStream(socket.getInputStream());
      dataOutputStream = new DataOutputStream(socket.getOutputStream());
      dataOutputStream.writeInt(TCPProvider.MAGIC_SERVER);
      dataOutputStream.writeInt(TCPProvider.VERSION);
    }

    @Override
    public void run() {
      ///////////////////////
      // read messages until something bad happens.
      try {
        while (true) {
          int type = dataInputStream.readInt();
          if (type == TCPProvider.MESSAGE_TYPE_PUBLISH) {
            int channellen = dataInputStream.readInt();
            byte channel[] = new byte[channellen];
            dataInputStream.readFully(channel);
            int datalen = dataInputStream.readInt();
            byte data[] = new byte[datalen];
            dataInputStream.readFully(data);
            TCPService.this.relay(channel, data);
            bytesCount += channellen + datalen + 8;
          } else if (type == TCPProvider.MESSAGE_TYPE_SUBSCRIBE) {
            int channellen = dataInputStream.readInt();
            byte channel[] = new byte[channellen];
            dataInputStream.readFully(channel);
            try {
              subscriptions_lock.writeLock().lock();
              subscriptions.add(new SubscriptionRecord(new String(channel)));
            } finally {
              subscriptions_lock.writeLock().unlock();
            }
          } else if (type == TCPProvider.MESSAGE_TYPE_UNSUBSCRIBE) {
            int channellen = dataInputStream.readInt();
            byte channel[] = new byte[channellen];
            dataInputStream.readFully(channel);
            String re = new String(channel);
            try {
              subscriptions_lock.writeLock().lock();
              for (int i = 0, n = subscriptions.size(); i < n; i++) {
                if (subscriptions.get(i).regex.equals(re)) {
                  subscriptions.remove(i);
                  break;
                }
              }
            } finally {
              subscriptions_lock.writeLock().unlock();
            }
          }
        }
      } catch (IOException ex) {
        // ---
      }
      ///////////////////////
      // Something bad happened, close this connection.
      try {
        closeResources();
      } catch (IOException ex) {
        // ---
      }
      try {
        clients_lock.writeLock().lock();
        clients.remove(this);
      } finally {
        clients_lock.writeLock().unlock();
      }
    }

    public void closeResources() throws IOException {
      socket.close();
    }

    public void send(String chanstr, byte channel[], byte data[]) {
      try {
        subscriptions_lock.readLock().lock();
        for (SubscriptionRecord sr : subscriptions) {
          if (sr.pattern.matcher(chanstr).matches()) {
            synchronized (dataOutputStream) {
              dataOutputStream.writeInt(TCPProvider.MESSAGE_TYPE_PUBLISH);
              dataOutputStream.writeInt(channel.length);
              dataOutputStream.write(channel);
              dataOutputStream.writeInt(data.length);
              dataOutputStream.write(data);
              dataOutputStream.flush();
              return;
            }
          }
        }
      } catch (IOException ex) {
        // ---
      } finally {
        subscriptions_lock.readLock().unlock();
      }
    }
  }
}
