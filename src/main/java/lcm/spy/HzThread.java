// code by lcm
package lcm.spy;

class HzThread extends Thread {
  private final Spy spy;

  public HzThread(Spy spy) {
    this.spy = spy;
    // setDaemon(true); // Modified by Jen
    setName("LCM-Spy"); // Modified by Jen
  }

  @Override
  public void run() {
    while (!isInterrupted()) {
      long utime = Spy.utime_now();
      synchronized (spy.channelList) {
        for (ChannelData cd : spy.channelList) {
          long diff_recv = cd.nreceived - cd.hz_last_nreceived;
          cd.hz_last_nreceived = cd.nreceived;
          long dutime = utime - cd.hz_last_utime;
          cd.hz_last_utime = utime;
          cd.hz = diff_recv / (dutime / 1000000.0);
          cd.min_interval = cd.hz_min_interval;
          cd.max_interval = cd.hz_max_interval;
          cd.hz_min_interval = 9999;
          cd.hz_max_interval = 0;
          cd.bandwidth = cd.hz_bytes / (dutime / 1000000.0);
          cd.hz_bytes = 0;
        }
      }
      int selrow = spy.channelTable.getSelectedRow();
      spy.channelTableModel.fireTableDataChanged();
      if (selrow >= 0)
        spy.channelTable.setRowSelectionInterval(selrow, selrow);
      {
        String rate = FriendlyFormat.byteSize(spy.totalBytesRate, true);
        String total = FriendlyFormat.byteSize(spy.totalBytes, true);
        spy.jLabelInfo.setText(rate + "/s " + total);
      }
      spy.totalBytesRate = 0;
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        interrupt();
      }
    }
  }
}