// code by lcm
// extracted by jph
package lcm.lcm;

import java.io.IOException;

enum LcmDemo {
  ;
  ////////////////////////////////////////////////////////////////
  /** Minimalist test code. **/
  public static void main(String args[]) {
    LCM lcm;
    try {
      lcm = new LCM();
    } catch (IOException ex) {
      System.err.println("ex: " + ex);
      return;
    }
    lcm.subscribeAll(new SimpleSubscriber());
    while (true) {
      try {
        Thread.sleep(1000);
        lcm.publish("TEST", "foobar");
      } catch (Exception ex) {
        System.err.println("ex: " + ex);
      }
    }
  }

  static class SimpleSubscriber implements LCMSubscriber {
    @Override
    public void messageReceived(LCM lcm, String channel, LCMDataInputStream dins) {
      System.err.println("RECV: " + channel);
    }
  }
}
