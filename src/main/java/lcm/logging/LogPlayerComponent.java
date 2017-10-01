// code by lcm
package lcm.logging;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.red.Max;
import lcm.lcm.LCM;

/** A GUI implementation of a log player allowing seeking. **/
public class LogPlayerComponent extends JComponent {
  static {
    System.setProperty("java.net.preferIPv4Stack", "true");
    System.out.println("LC: Disabling IPV6 support");
  }
  // ---
  private Log log;
  private final JButton playButton = new JButton("Play ");
  private final JButton stepButton = new JButton("Step");
  private final JButton fasterButton;
  private final JButton slowerButton;
  private Scalar speed = RealScalar.ONE;
  private final JLabel speedLabel = new JLabel(speed.toString(), JLabel.CENTER);
  private final JLabel posLabel = new JLabel("Event 0");
  private final JLabel timeLabel = new JLabel("Time 0.0s");
  private final JLabel actualSpeedLabel = new JLabel("1.0x");
  private final JLabel logName = new JLabel("---");
  private PlayerThread player = null;
  private LCM lcm;
  /** The time of the first event in the current log **/
  private long timeOffset = 0;
  private JFileChooser jfc = new JFileChooser();
  private String currentLogPath;
  /** an estimate of how many seconds there are in the file */
  private double total_seconds;
  final BlockingQueue<QueuedEvent> events = new LinkedBlockingQueue<>();
  private final Object sync = new Object();
  private boolean isLaunched = true;
  private Pattern filteredPattern;
  private boolean invertFilteredPattern;
  private final FilterTableModel filterTableModel = new FilterTableModel(this);
  final List<Filter> filters = new ArrayList<>();
  // JTable calls upon filterTableModel which calls upon filters...
  // which needs to exist before that!
  private final JTable filterTable = new JTable(filterTableModel);
  private final Map<String, Filter> filterMap = new HashMap<>();
  private final JScrubber js = new JScrubber();
  private boolean show_absolute_time = false;
  private final JTextField stepChannelField = new JTextField("");
  private QueueThread queueThread;
  private UDPThread udpThread;

  interface QueuedEvent {
    public void execute(LogPlayerComponent lp);
  }

  class QueueThread extends Thread {
    @Override
    public void run() {
      while (isLaunched) {
        try {
          QueuedEvent queuedEvent = events.take();
          queuedEvent.execute(LogPlayerComponent.this);
        } catch (InterruptedException ex) {
          // ---
        }
      }
    }
  }

  /** We have events coming from all over the place: the UI, UDP events,
   * callbacks from the scrubbers. To keep things sanely thread-safe, all of
   * these things simply queue events which are processed in-order. doStop,
   * doPlay, doStep, do(Anything) can only be called from the queue thread. **/
  class PlayPauseEvent implements QueuedEvent {
    boolean toggle = false;
    boolean playstate;

    public PlayPauseEvent() {
      this.toggle = true;
    }

    public PlayPauseEvent(boolean playstate) {
      this.playstate = playstate;
    }

    @Override
    public void execute(LogPlayerComponent lp) {
      if (toggle) {
        if (player != null)
          doStop();
        else
          doPlay();
      } else {
        if (playstate)
          doPlay();
        else
          doStop();
      }
    }
  }

  // seek, preserving the current play/pause state
  class SeekEvent implements QueuedEvent {
    double pos;

    public SeekEvent(double pos) {
      this.pos = pos;
    }

    @Override
    public void execute(LogPlayerComponent lp) {
      boolean player_was_running = (player != null);
      if (player_was_running)
        doStop();
      doSeek(pos);
      if (player_was_running)
        doPlay();
    }
  }

  class StepEvent implements QueuedEvent {
    @Override
    public void execute(LogPlayerComponent lp) {
      doStep();
    }
  }

  class Filter implements Comparable<Filter> {
    String inchannel;
    String outchannel;
    boolean enabled = true;

    @Override
    public int compareTo(Filter filter) {
      return inchannel.compareTo(filter.inchannel);
    }
  }

  // faster/slower would be better as semi-log.
  private static Scalar slowerSpeed(Scalar v) {
    return v.divide(RealScalar.of(2));
  }

  private static Scalar fasterSpeed(Scalar v) {
    return v.multiply(RealScalar.of(2));
  }

  void setSpeed(Scalar value) {
    value = Max.of(RationalScalar.of(1, 1024), value); // minimum supported speed (0.000977x)
    speedLabel.setText(value.toString());
    speed = value;
  }

  void setChannelFilter(String channelFilterRegex) {
    filteredPattern = Pattern.compile(channelFilterRegex);
  }

  void invertChannelFilter() {
    invertFilteredPattern = true;
  }

  LogPlayerComponent(String lcmurl, Scalar _speed) throws IOException {
    setSpeed(_speed);
    setLayout(new GridBagLayout());
    @SuppressWarnings("unused")
    GridBagConstraints gbc = new GridBagConstraints();
    filteredPattern = null;
    invertFilteredPattern = false;
    Insets insets = new Insets(0, 0, 0, 0);
    int row = 0;
    logName.setText("No log loaded");
    logName.setFont(new Font("SansSerif", Font.PLAIN, 10));
    timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
    posLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
    actualSpeedLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
    fasterButton = new JButton(new ImageIcon(StaticHelper.makeArrowImage(Color.blue, getBackground(), false)));
    fasterButton.setRolloverIcon(new ImageIcon(StaticHelper.makeArrowImage(Color.magenta, getBackground(), false)));
    fasterButton.setPressedIcon(new ImageIcon(StaticHelper.makeArrowImage(Color.red, getBackground(), false)));
    fasterButton.setBorderPainted(false);
    fasterButton.setContentAreaFilled(false);
    // Borders keep appearing when the buttons are pressed. Not sure why.
    // fasterButton.setBorder(null); //new
    // javax.swing.border.EmptyBorder(0,0,0,0));
    slowerButton = new JButton(new ImageIcon(StaticHelper.makeArrowImage(Color.blue, getBackground(), true)));
    slowerButton.setRolloverIcon(new ImageIcon(StaticHelper.makeArrowImage(Color.magenta, getBackground(), true)));
    slowerButton.setPressedIcon(new ImageIcon(StaticHelper.makeArrowImage(Color.red, getBackground(), true)));
    slowerButton.setBorderPainted(false);
    slowerButton.setContentAreaFilled(false);
    Font buttonFont = new Font("SansSerif", Font.PLAIN, 10);
    fasterButton.setFont(buttonFont);
    slowerButton.setFont(buttonFont);
    playButton.setFont(buttonFont);
    stepButton.setFont(buttonFont);
    JPanel p = new JPanel();
    p.setLayout(new GridLayout(1, 3, 0, 0));
    p.add(slowerButton);
    p.add(speedLabel);
    p.add(fasterButton);
    // x y w h fillx filly anchor fill insets, ix, iy
    add(logName, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, //
        GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
    add(playButton, new GridBagConstraints(1, row, 1, 1, 0.0, 0.0, //
        GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 0, 0));
    add(stepButton, new GridBagConstraints(2, row, 1, 1, 0.0, 0.0, //
        GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 0, 0));
    add(p, new GridBagConstraints(3, row, GridBagConstraints.REMAINDER, 1, 0.0, 0.0, //
        GridBagConstraints.EAST, GridBagConstraints.NONE, insets, 0, 0));
    row++;
    add(js, new GridBagConstraints(0, row, GridBagConstraints.REMAINDER, 1, 1.0, 0.0, //
        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    row++;
    add(timeLabel, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, //
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    add(actualSpeedLabel, new GridBagConstraints(1, row, 1, 1, 0.0, 0.0, //
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    add(posLabel, new GridBagConstraints(3, row, 1, 1, 0.0, 0.0, //
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
    row++;
    add(new JScrollPane(filterTable), new GridBagConstraints(0, row, GridBagConstraints.REMAINDER, 1, 1.0, 1.0, //
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    row++;
    /// spacers
    add(Box.createHorizontalStrut(90), new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, //
        GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
    add(Box.createHorizontalStrut(100), new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, //
        GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
    ///////////////////////////
    row++;
    JPanel stepPanel = new JPanel(new BorderLayout());
    stepPanel.add(new JLabel("Channel Prefix: "), BorderLayout.WEST);
    stepPanel.add(stepChannelField, BorderLayout.CENTER);
    JButton toggleAllButton = new JButton("Toggle Selected");
    toggleAllButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int[] rowIndices = filterTable.getSelectedRows();
        for (int i = 0; i < rowIndices.length; ++i) {
          Filter f = filters.get(rowIndices[i]);
          f.enabled = !f.enabled;
        }
        filterTableModel.fireTableDataChanged();
        for (int i = 0; i < rowIndices.length; ++i) {
          filterTable.addRowSelectionInterval(rowIndices[i], rowIndices[i]);
        }
      }
    });
    add(toggleAllButton, new GridBagConstraints(0, row, 2, 1, 0.0, 0.0, //
        GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 0, 0));
    add(stepPanel, new GridBagConstraints(2, row, GridBagConstraints.REMAINDER, 1, 1.0, 0.0, //
        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    // position.addChangeListener(new MyChangeListener());
    setPlaying(false);
    fasterButton.addActionListener(e -> setSpeed(fasterSpeed(speed)));
    slowerButton.addActionListener(e -> setSpeed(slowerSpeed(speed)));
    playButton.addActionListener(e -> events.offer(new PlayPauseEvent()));
    stepButton.addActionListener(e -> events.offer(new StepEvent()));
    if (null == lcmurl)
      lcm = new LCM();
    else
      lcm = new LCM(lcmurl);
    logName.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2)
          openDialog();
      }
    });
    timeLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        show_absolute_time = !show_absolute_time;
      }
    });
    js.set(0);
    js.addScrubberListener(new MyScrubberListener());
    filterTable.getColumnModel().getColumn(2).setMaxWidth(50);
    playButton.setEnabled(false);
    // ---
    //
    queueThread = new QueueThread();
    queueThread.start();
  }

  public void startRemoteControl() {
    udpThread = new UDPThread();
    udpThread.start();
  }

  public void stopThreads() {
    isLaunched = false;
    // udpThread.interrupt();
    queueThread.interrupt();
    if (player != null)
      player.requestStop();
  }

  class MyScrubberListener implements JScrubberListener {
    @Override
    public void scrubberMovedByUser(JScrubber js, double x) {
      events.offer(new SeekEvent(x));
    }

    @Override
    public void scrubberPassedRepeat(JScrubber js, double from_pos, double to_pos) {
      events.offer(new SeekEvent(to_pos));
    }

    @Override
    public void scrubberExportRegion(JScrubber js, double p0, double p1) {
      System.out.printf("Export %15f %15f\n", p0, p1);
      String outpath = getOutputFileFromDialog();
      if (outpath == null)
        return;
      System.out.println("Exporting to " + outpath);
      try {
        Log inlog = new Log(log.getPath(), "r");
        Log outlog = new Log(outpath, "rw");
        inlog.seekPositionFraction(p0);
        while (inlog.getPositionFraction() < p1) {
          Log.Event e = inlog.readNext();
          Filter filter = filterMap.get(e.channel);
          if (filter != null && filter.enabled)
            outlog.write(e);
        }
        inlog.close();
        outlog.close();
        System.out.printf("Done!\n");
      } catch (IOException ex) {
        System.out.println("Exception: " + ex);
      }
    }
  }

  // remote control via UDP packets
  class UDPThread extends Thread {
    @Override
    public void run() {
      DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
      try (DatagramSocket datagramSocket = new DatagramSocket(53261, Inet4Address.getByName("127.0.0.1"))) {
        while (isLaunched) {
          try {
            datagramSocket.receive(datagramPacket);
            String cmd = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
            cmd = cmd.trim();
            if (cmd.equals("PLAYPAUSETOGGLE")) {
              events.offer(new PlayPauseEvent());
            } else if (cmd.equals("PLAY")) {
              events.offer(new PlayPauseEvent(true));
            } else if (cmd.equals("PAUSE")) {
              events.offer(new PlayPauseEvent(false));
            } else if (cmd.equals("STEP")) {
              events.offer(new StepEvent());
            } else if (cmd.equals("FASTER")) {
              setSpeed(fasterSpeed(speed));
            } else if (cmd.equals("SLOWER")) {
              setSpeed(slowerSpeed(speed));
            } else if (cmd.startsWith("BACK")) {
              double seconds = Double.parseDouble(cmd.substring(4));
              double pos = log.getPositionFraction() - seconds / total_seconds;
              events.offer(new SeekEvent(pos));
            } else if (cmd.startsWith("FORWARD")) {
              double seconds = Double.parseDouble(cmd.substring(7));
              double pos = log.getPositionFraction() + seconds / total_seconds;
              events.offer(new SeekEvent(pos));
            } else {
              System.out.println("Unknown remote command: " + cmd);
            }
          } catch (IOException ex) {
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      System.out.println("udp thread exit");
    }
  }

  private String getOutputFileFromDialog() {
    JFileChooser chooser = new JFileChooser();
    chooser.setBounds(100, 100, 1000, 800);
    int res = chooser.showSaveDialog(this);
    if (res != JFileChooser.APPROVE_OPTION)
      return null;
    return chooser.getSelectedFile().getPath();
  }

  void openDialog() {
    doStop();
    jfc.setBounds(100, 100, 1000, 800);
    int res = jfc.showOpenDialog(this);
    if (res != JFileChooser.APPROVE_OPTION)
      return;
    try {
      setLog(jfc.getSelectedFile().getPath(), true);
    } catch (IOException ex) {
      System.out.println("Exception: " + ex);
    }
  }

  void savePreferences() throws IOException {
    if (currentLogPath == null)
      return;
    String path = currentLogPath + ".jlp";
    FileWriter fouts = new FileWriter(path);
    BufferedWriter outs = new BufferedWriter(fouts);
    List<JScrubber.Bookmark> bookmarks = js.getBookmarks();
    for (JScrubber.Bookmark b : bookmarks) {
      String type = "PLAIN";
      if (b.type == JScrubber.BOOKMARK_LREPEAT)
        type = "LREPEAT";
      if (b.type == JScrubber.BOOKMARK_RREPEAT)
        type = "RREPEAT";
      outs.write("BOOKMARK " + type + " " + b.position + "\n");
    }
    outs.write("ZOOMFRAC " + js.getZoomFraction() + "\n");
    for (Filter f : filters) {
      outs.write("CHANNEL " + f.inchannel + " " + f.outchannel + " " + f.enabled + "\n");
    }
    outs.close();
    fouts.close();
  }

  private Filter addChannelFilter(String channel, boolean enabledByDefault) {
    Filter filter = new Filter();
    filter.inchannel = channel;
    filter.outchannel = channel;
    if (filteredPattern == null)
      filter.enabled = enabledByDefault;
    else
      filter.enabled = !(invertFilteredPattern ^ filteredPattern.matcher(channel).matches());
    filterMap.put(filter.inchannel, filter);
    filters.add(filter);
    Collections.sort(filters);
    filterTableModel.fireTableDataChanged();
    return filter;
  }

  @SuppressWarnings("resource")
  private void loadPreferences(String path) throws IOException {
    BufferedReader ins;
    js.clearBookmarks();
    filterMap.clear();
    filters.clear();
    try {
      ins = new BufferedReader(new FileReader(path));
    } catch (FileNotFoundException ex) {
      // no error; just a no-op
      return;
    }
    String line;
    while ((line = ins.readLine()) != null) {
      String toks[] = line.split("\\s+");
      if (toks[0].equals("BOOKMARK") && toks.length == 3) {
        int type = JScrubber.BOOKMARK_PLAIN;
        if (toks[1].equals("RREPEAT"))
          type = JScrubber.BOOKMARK_RREPEAT;
        if (toks[1].equals("LREPEAT"))
          type = JScrubber.BOOKMARK_LREPEAT;
        js.addBookmark(type, Double.parseDouble(toks[2]));
      }
      if (toks[0].equals("CHANNEL") && toks.length == 4) {
        Filter filter = filterMap.get(toks[1]);
        if (filter == null) {
          filter = new Filter();
          filter.inchannel = toks[1];
          filter.outchannel = toks[1];
          filterMap.put(toks[1], filter);
          filters.add(filter);
        }
        filter.outchannel = toks[2];
        filter.enabled = Boolean.parseBoolean(toks[3]);
        if (filteredPattern != null) // disable if either the saved
                                     // value, or the filter say it
                                     // should be disabled
          filter.enabled = filter.enabled && !(invertFilteredPattern ^ filteredPattern.matcher(filter.inchannel).matches());
      }
      if (toks[0].equals("ZOOMFRAC"))
        js.setZoomFraction(Double.parseDouble(toks[1]));
    }
    filterTableModel.fireTableDataChanged();
  }

  private void populateChannelFilters() {
    try {
      long logStartUTime = -1;
      while (isLaunched) {
        Log.Event e = log.readNext();
        if (logStartUTime < 0)
          logStartUTime = e.utime;
        // only scan through the first 30sec of the log
        if (e.utime - logStartUTime > 30 * 1e6) {
          break;
        }
        Filter f = filterMap.get(e.channel);
        if (f == null) {
          addChannelFilter(e.channel, !invertFilteredPattern);
        }
      }
    } catch (EOFException ex) {
      // System.err.println("Breaking at end of log");
    } catch (IOException ex) {
      System.err.println("Exception: " + ex);
    }
    try {
      // rewind to beginning of the log
      log.seekPositionFraction(0);
    } catch (IOException ex) {
      System.err.println("Exception: " + ex);
    }
  }

  void setLog(String path, boolean startPlaying) throws IOException {
    if (currentLogPath != null)
      savePreferences();
    currentLogPath = path;
    log = new Log(path, "r");
    logName.setText(new File(path).getName());
    try {
      Log.Event e = log.readNext();
      timeOffset = e.utime;
      playButton.setEnabled(true);
      log.seekPositionFraction(.10);
      Log.Event e10 = log.readNext();
      log.seekPositionFraction(.90);
      Log.Event e90 = log.readNext();
      total_seconds = (e90.utime - e10.utime) / 1000000.0 / 0.8;
      System.out.printf("Total seconds: %f\n", total_seconds);
      log.seekPositionFraction(0);
    } catch (IOException ex) {
      System.out.println("exception: " + ex);
    }
    loadPreferences(path + ".jlp");
    if (startPlaying)
      doPlay();
    else {
      populateChannelFilters();
    }
  }

  private void setPlaying(boolean t) {
    playButton.setText(t ? "Pause" : "Play");
    stepButton.setEnabled(!t);
  }

  // the player can stop automatically on error or EOF; we thus have
  // a potential race condition between auto-stops and requested stops.
  //
  // We protect these two with 'sync'.
  private void doStop() {
    PlayerThread pptr;
    synchronized (sync) {
      if (player == null)
        return;
      pptr = player;
      pptr.requestStop();
    }
    try {
      pptr.join();
    } catch (InterruptedException ex) {
      System.out.println("Exception: " + ex);
    }
  }

  private void doPlay() {
    if (player != null)
      return;
    player = new PlayerThread();
    player.start();
  }

  private void doStep() {
    if (player != null)
      return;
    player = new PlayerThread(stepChannelField.getText());
    player.start();
  }

  private void doSeek(double ratio) {
    assert player == null;
    if (ratio < 0)
      ratio = 0;
    if (ratio > 1)
      ratio = 1;
    try {
      log.seekPositionFraction(ratio);
      Log.Event e = log.readNext();
      log.seekPositionFraction(ratio);
      js.set(log.getPositionFraction());
      lastSystemTime = 0; // reset log-play statistics.
      updateDisplay(e);
    } catch (IOException ex) {
      System.out.println("exception: " + ex);
    }
  }

  private long lastEventTime;
  private long lastSystemTime;

  private void updateDisplay(Log.Event e) {
    if (show_absolute_time) {
      java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy.MM.dd HH:mm:ss.S z");
      Date timestamp = new Date(e.utime / 1000);
      timeLabel.setText(df.format(timestamp));
    } else {
      timeLabel.setText(String.format("%.3f s", (e.utime - timeOffset) / 1000000.0));
    }
    posLabel.setText("" + e.eventNumber);
    long systemTime = System.currentTimeMillis();
    double dt = (systemTime - lastSystemTime) / 1000.0;
    if (dt > 0.5) {
      double actualSpeed = (e.utime - lastEventTime) / 1000000.0 / dt;
      lastEventTime = e.utime;
      lastSystemTime = systemTime;
      actualSpeedLabel.setText(String.format("%.2f x", actualSpeed));
    }
  }

  class PlayerThread extends Thread {
    private boolean stopflag = false;
    private String stopOnChannel;

    public PlayerThread() {
    }

    public PlayerThread(String stopOnChannel) {
      this.stopOnChannel = stopOnChannel;
    }

    void requestStop() {
      stopflag = true;
    }

    @Override
    @SuppressWarnings("unused")
    public void run() {
      long lastTime = 0;
      long lastDisplayTime = 0;
      long localOffset = 0;
      long logOffset = 0;
      long last_e_utime = 0;
      Scalar lastspeed = RealScalar.ZERO;
      synchronized (sync) {
        setPlaying(true);
      }
      try {
        while (!stopflag) {
          Log.Event e = log.readNext();
          if (!speed.equals(lastspeed)) {
            // System.out.printf("Speed changed. Old %12.6f new
            // %12.6f\n",
            // lastspeed, speed);
            logOffset = e.utime;
            localOffset = System.nanoTime() / 1000;
            lastspeed = speed;
          }
          long logRelativeTime = (long) (e.utime - logOffset);
          long now = System.nanoTime();
          long clockRelativeTime = now / 1000 - localOffset;
          // we don't support playback below a rate of 1/1024x
          long speed_scale = (long) Math.max(1, (speed.number().doubleValue() * 1024.0));
          long waitTime = (1024 * logRelativeTime / speed_scale - clockRelativeTime);
          long waitms = waitTime / 1000;
          waitms = Math.max(0, waitms);
          /* System.out.
           * printf("Now 0x%016X ns, %12.6fx playback, %8d/1024 playback, %10dus rel log time, %10dus, %10dus rel clock, "
           * + "wait %10dus (%10dms)\n", now, speed, speed_scale,
           * logRelativeTime, 1024*logRelativeTime/speed_scale,
           * clockRelativeTime, waitTime, waitms); */
          last_e_utime = e.utime;
          try {
            // We might have a very long wait, but
            // only sleep for relatively short amounts
            // of time so that we remain responsive to
            // seek/speed changes.
            while (waitms > 0 && !stopflag) {
              if (waitms > 50) {
                Thread.sleep(50);
                waitms -= 50;
              } else {
                Thread.sleep(waitms);
                waitms = 0;
              }
            }
          } catch (InterruptedException ex) {
            System.out.println("Interrupted");
          }
          // During the sleep, other threads might have
          // run that have asked us to stop (a
          // jscrubber.userset in particular); recheck
          // the stop flag before we blindly proceed.
          // (This ameliorates but does not solve an
          // intrinsic race condition)
          if (stopflag)
            break;
          Filter filter = filterMap.get(e.channel);
          if (filter == null) {
            filter = addChannelFilter(e.channel, !invertFilteredPattern);
          }
          if (filter.enabled && filter.outchannel.length() > 0)
            lcm.publish(filter.outchannel, e.data, 0, e.data.length);
          js.set(log.getPositionFraction());
          // redraw labels no faster than 10 Hz
          long curTime = System.currentTimeMillis();
          if (curTime - lastDisplayTime > 100) {
            updateDisplay(e);
            lastDisplayTime = curTime;
          }
          if (stopOnChannel != null && e.channel.startsWith(stopOnChannel)) {
            stopflag = true;
            break;
          }
        }
      } catch (EOFException ex) {
        stopflag = true;
      } catch (IOException ex) {
        System.out.println("Exception: " + ex);
        stopflag = true;
      }
      synchronized (sync) {
        setPlaying(false);
        player = null;
      }
    }
  }
}
