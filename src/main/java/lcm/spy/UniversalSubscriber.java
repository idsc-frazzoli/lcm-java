package lcm.spy;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import lcm.lcm.LCM;
import lcm.lcm.LCMDataInputStream;
import lcm.lcm.LCMSubscriber;

/** the original name of the class was MySubscriber */
class UniversalSubscriber implements LCMSubscriber {
  private final Spy spy;

  /** @param spy */
  UniversalSubscriber(Spy spy) {
    this.spy = spy;
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void messageReceived(LCM lcm, String channel, LCMDataInputStream dins) {
    Object object = null;
    ChannelData cd = this.spy.channelMap.get(channel);
    int msg_size = 0;
    try {
      msg_size = dins.available();
      long fingerprint = (msg_size >= 8) ? dins.readLong() : -1;
      dins.reset();
      Class cls = this.spy.handlers.getClassByFingerprint(fingerprint);
      if (cd == null) {
        cd = new ChannelData();
        cd.name = channel;
        cd.cls = cls;
        cd.fingerprint = fingerprint;
        cd.row = this.spy.channelList.size();
        synchronized (this.spy.channelList) {
          this.spy.channelMap.put(channel, cd);
          this.spy.channelList.add(cd);
          this.spy._channelTableModel.fireTableDataChanged();
        }
      } else {
        if (cls != null && cd.cls != null && !cd.cls.equals(cls)) {
          System.out.println("WARNING: Class changed for channel " + channel);
          cd.nerrors++;
        }
      }
      long utime = Spy.utime_now();
      long interval = utime - cd.last_utime;
      cd.hz_min_interval = Math.min(cd.hz_min_interval, interval);
      cd.hz_max_interval = Math.max(cd.hz_max_interval, interval);
      cd.hz_bytes += msg_size;
      this.spy.totalBytes += msg_size;
      this.spy.totalBytesRate += msg_size;
      cd.last_utime = utime;
      cd.nreceived++;
      object = cd.cls.getConstructor(DataInput.class).newInstance(dins);
      cd.last = object;
      if (cd.viewer != null)
        cd.viewer.setObject(object, cd.last_utime);
    } catch (NullPointerException ex) {
      cd.nerrors++;
    } catch (IOException ex) {
      cd.nerrors++;
      System.out.println("Spy.messageReceived ex: " + ex);
    } catch (NoSuchMethodException ex) {
      cd.nerrors++;
      System.out.println("Spy.messageReceived ex: " + ex);
    } catch (InstantiationException ex) {
      cd.nerrors++;
      System.out.println("Spy.messageReceived ex: " + ex);
    } catch (IllegalAccessException ex) {
      cd.nerrors++;
      System.out.println("Spy.messageReceived ex: " + ex);
    } catch (InvocationTargetException ex) {
      cd.nerrors++;
      // these are almost always spurious
      // System.out.println("ex: "+ex+"..."+ex.getTargetException());
    }
  }
}