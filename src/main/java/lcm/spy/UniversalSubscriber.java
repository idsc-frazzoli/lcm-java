// code by lcm
package lcm.spy;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import lcm.lcm.LCM;
import lcm.lcm.LCMDataInputStream;
import lcm.lcm.LCMSubscriber;

/** the original name of the class was MySubscriber */
/* package */ class UniversalSubscriber implements LCMSubscriber {
  private final Spy spy;

  /** @param spy */
  UniversalSubscriber(Spy spy) {
    this.spy = spy;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void messageReceived(LCM lcm, String channel, LCMDataInputStream dins) {
    Object object = null;
    ChannelData channelData = spy.channelMap.get(channel);
    int msg_size = 0;
    try {
      msg_size = dins.available();
      long fingerprint = (msg_size >= 8) ? dins.readLong() : -1;
      dins.reset();
      Class<?> cls = spy.lcmTypeDatabase.getClassByFingerprint(fingerprint);
      if (channelData == null) {
        channelData = new ChannelData();
        channelData.name = channel;
        channelData.cls = cls;
        channelData.fingerprint = fingerprint;
        channelData.row = spy.channelList.size();
        synchronized (spy.channelList) {
          spy.channelMap.put(channel, channelData);
          spy.channelList.add(channelData);
          spy._channelTableModel.fireTableDataChanged();
        }
      } else {
        if (cls != null && channelData.cls != null && !channelData.cls.equals(cls)) {
          System.out.println("WARNING: Class changed for channel " + channel);
          channelData.nerrors++;
        }
      }
      long utime = Spy.utime_now();
      long interval = utime - channelData.last_utime;
      channelData.hz_min_interval = Math.min(channelData.hz_min_interval, interval);
      channelData.hz_max_interval = Math.max(channelData.hz_max_interval, interval);
      channelData.hz_bytes += msg_size;
      spy.totalBytes += msg_size;
      spy.totalBytesRate += msg_size;
      channelData.last_utime = utime;
      channelData.nreceived++;
      object = channelData.cls.getConstructor(DataInput.class).newInstance(dins);
      channelData.last = object;
      if (channelData.viewer != null)
        channelData.viewer.setObject(object, channelData.last_utime);
    } catch (NullPointerException ex) {
      channelData.nerrors++;
    } catch (IOException ex) {
      channelData.nerrors++;
      System.out.println("Spy.messageReceived ex: " + ex);
    } catch (NoSuchMethodException ex) {
      channelData.nerrors++;
      System.out.println("Spy.messageReceived ex: " + ex);
    } catch (InstantiationException ex) {
      channelData.nerrors++;
      System.out.println("Spy.messageReceived ex: " + ex);
    } catch (IllegalAccessException ex) {
      channelData.nerrors++;
      System.out.println("Spy.messageReceived ex: " + ex);
    } catch (InvocationTargetException ex) {
      channelData.nerrors++;
      // these are almost always spurious
      // System.out.println("ex: "+ex+"..."+ex.getTargetException());
    }
  }
}