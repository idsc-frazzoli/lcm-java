// code by lcm
package lcm.lcm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lightweight Communications and Marshalling Java implementation */
public class LCM {
  // subscriptions objects is used for synchronization
  private final List<SubscriptionRecord> subscriptions = new ArrayList<>();
  private final List<Provider> providers = new ArrayList<>();
  private final Map<String, List<SubscriptionRecord>> subscriptionsMap = new HashMap<>();
  private boolean closed = false;
  private static LCM singleton;
  private final LCMDataOutputStream encodeBuffer = new LCMDataOutputStream(new byte[1024]);

  /** Create a new LCM object, connecting to one or more URLs. If no URL is
   * specified, the environment variable LCM_DEFAULT_URL is used. If that
   * environment variable is not defined, then the default URL is used. */
  public LCM(String... urls) throws IOException {
    if (urls.length == 0) {
      String env = System.getenv("LCM_DEFAULT_URL");
      if (env == null)
        urls = new String[] { "udpm://239.255.76.67:7667" };
      else
        urls = new String[] { env };
    }
    for (String url : urls) {
      // Allow passing in NULL or the empty string to explicitly indicate
      // the default LCM URL.
      if (null == url || url.equals("")) {
        url = System.getenv("LCM_DEFAULT_URL");
        if (url == null)
          url = "udpm://239.255.76.67:7667";
      }
      URLParser up = new URLParser(url);
      String protocol = up.get("protocol");
      if (protocol.equals("udpm"))
        providers.add(new UDPMulticastProvider(this, up));
      else if (protocol.equals("tcpq"))
        providers.add(new TCPProvider(this, up));
      else if (protocol.equals("file"))
        providers.add(new LogFileProvider(this, up));
      else if (protocol.equals("memq"))
        providers.add(new MemqProvider(this, up));
      else
        System.err.println("LCM: Unknown URL protocol: " + protocol);
    }
  }

  /** Retrieve a default instance of LCM using either the environment variable
   * LCM_DEFAULT_URL or the default. */
  public static LCM getSingleton() {
    if (singleton == null) {
      try {
        singleton = new LCM();
      } catch (Exception exception) {
        System.err.println("LC singleton fail: " + exception);
        throw new RuntimeException();
      }
    }
    return singleton;
  }

  /** Return the number of subscriptions. */
  public int getNumSubscriptions() {
    if (closed)
      throw new IllegalStateException();
    return subscriptions.size();
  }

  /** Publish an LCM-defined type on a channel. If more than one URL was
   * specified, the message will be sent on each. **/
  public synchronized void publish(String channel, LCMEncodable e) {
    if (closed)
      throw new IllegalStateException();
    try {
      encodeBuffer.reset();
      e.encode(encodeBuffer);
      publish(channel, encodeBuffer.getBuffer(), 0, encodeBuffer.size());
    } catch (IOException ex) {
      System.err.println("LC publish fail: " + ex);
    }
  }

  /** Publish raw data on a channel, bypassing the LCM type specification. If
   * more than one URL was specified when the LCM object was created, the
   * message will be sent on each. **/
  public synchronized void publish(String channel, byte[] data, int offset, int length) {
    if (closed)
      throw new IllegalStateException();
    for (Provider p : providers)
      p.publish(channel, data, offset, length);
  }

  /** Subscribe to all channels whose name matches the regular expression. Note
   * that to subscribe to all channels, you must specify ".*", not "*".
   * 
   * @return subscription record that allows to conveniently unsubscribe */
  public SubscriptionRecord subscribe(String regex, LCMSubscriber sub) {
    if (closed)
      throw new IllegalStateException();
    final SubscriptionRecord srec = new SubscriptionRecord(regex, sub);
    synchronized (this) {
      for (Provider provider : providers)
        provider.subscribe(regex);
    }
    synchronized (subscriptions) {
      subscriptions.add(srec);
      for (String channel : subscriptionsMap.keySet())
        if (srec.matches(channel))
          subscriptionsMap.get(channel).add(srec);
    }
    return srec;
  }

  /** function not part of the original LCM API
   * 
   * @param srec */
  public void unsubscribe(SubscriptionRecord srec) {
    if (closed)
      throw new IllegalStateException();
    synchronized (subscriptions) {
      // Find and remove subscriber from list
      boolean removed = subscriptions.remove(srec);
      if (!removed)
        new RuntimeException(srec.regex + " not removed").printStackTrace();
      // Find and remove subscriber from map
      for (String channel : subscriptionsMap.keySet())
        // TODO remove channel as key, if value collection is empty
        subscriptionsMap.get(channel).remove(srec);
    }
  }

  public void unsubscribeAll(Collection<SubscriptionRecord> collection) {
    if (closed)
      throw new IllegalStateException();
    synchronized (subscriptions) {
      // Find and remove subscriber from list
      subscriptions.removeAll(collection);
      // Find and remove subscriber from map
      for (String channel : subscriptionsMap.keySet())
        subscriptionsMap.get(channel).removeAll(collection);
    }
  }

  /** Not for use by end users. Provider back ends call this method when they
   * receive a message. The subscribers that match the channel name are
   * synchronously notified. */
  public void receiveMessage(String channel, byte data[], int offset, int length) {
    if (closed)
      throw new IllegalStateException();
    synchronized (subscriptions) {
      List<SubscriptionRecord> srecs = subscriptionsMap.get(channel);
      if (srecs == null) {
        // must build this list!
        srecs = new ArrayList<>();
        subscriptionsMap.put(channel, srecs);
        for (SubscriptionRecord srec : subscriptions)
          if (srec.matches(channel))
            srecs.add(srec);
      }
      for (SubscriptionRecord srec : srecs)
        srec.lcsub.messageReceived(this, channel, new LCMDataInputStream(data, offset, length));
    }
  }

  /** A convenience function that subscribes to all LCM channels. */
  public synchronized void subscribeAll(LCMSubscriber sub) {
    subscribe(".*", sub);
  }

  /** Call this function to release all resources used by the LCM instance.
   * After calling this function, the LCM instance should consume no
   * resources, and cannot be used to receive or transmit messages. */
  public synchronized void close() {
    // TODO by Jen Check when should close and when should unsubscribe
    if (closed)
      throw new IllegalStateException();
    synchronized (subscriptions) {
      subscriptions.clear();
      subscriptionsMap.clear();
    }
    synchronized (this) {
      providers.forEach(Provider::close);
      providers.clear();
    }
    closed = true;
  }
}
