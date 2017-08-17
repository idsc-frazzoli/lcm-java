package lcm.spy;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.table.TableColumnModel;

import ch.ethz.idsc.lcm.util.FriendlyFormat;
import lcm.lcm.LCM;
import lcm.lcm.LCMDataInputStream;
import lcm.lcm.LCMSubscriber;
import lcm.util.ClassDiscoverer;
import lcm.util.TableSorter;

/** Spy main class. **/
public class Spy {
  LCM lcm;
  LCMTypeDatabase handlers;
  long startuTime; // time that lcm-spy started
  Map<String, ChannelData> channelMap = new HashMap<>();
  List<ChannelData> channelList = new ArrayList<>();
  ChannelTableModel _channelTableModel = new ChannelTableModel(channelList);
  TableSorter channelTableModel = new TableSorter(_channelTableModel);
  JTable channelTable = new JTable(channelTableModel);
  ChartData chartData;
  List<SpyPlugin> plugins = new ArrayList<>();
  JButton clearButton = new JButton("Clear");
  JFrame jif;
  HzThread thread; // Added by Jen
  JLabel jLabelInfo = new JLabel();
  long totalBytes = 0;
  long totalBytesRate = 0;

  public Spy(String lcmurl) throws IOException {
    // sortedChannelTableModel.addMouseListenerToHeaderInTable(channelTable);
    channelTableModel.setTableHeader(channelTable.getTableHeader());
    channelTableModel.setSortingStatus(0, TableSorter.ASCENDING);
    handlers = new LCMTypeDatabase();
    TableColumnModel tcm = channelTable.getColumnModel();
    tcm.getColumn(0).setMinWidth(140);
    tcm.getColumn(1).setMinWidth(140);
    tcm.getColumn(2).setMaxWidth(100);
    tcm.getColumn(3).setMaxWidth(100);
    tcm.getColumn(4).setMaxWidth(100);
    tcm.getColumn(5).setMaxWidth(100);
    tcm.getColumn(6).setMaxWidth(100);
    jif = new JFrame("LCM Spy");
    jif.setLayout(new BorderLayout());
    jif.add(channelTable.getTableHeader(), BorderLayout.PAGE_START);
    {
      JToolBar jToolBar = new JToolBar();
      jToolBar.add(clearButton);
      jToolBar.addSeparator();
      jToolBar.add(jLabelInfo);
      jToolBar.setFloatable(false);
      jif.add(jToolBar, BorderLayout.NORTH);
    }
    jif.add(new JScrollPane(channelTable), BorderLayout.CENTER);
    chartData = new ChartData(utime_now());
    jif.setSize(800, 600);
    jif.setLocationByPlatform(true);
    jif.setVisible(true);
    if (null == lcmurl)
      lcm = new LCM();
    else
      lcm = new LCM(lcmurl);
    lcm.subscribeAll(new MySubscriber());
    thread = new HzThread();
    thread.start();
    clearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        channelMap.clear();
        channelList.clear();
        channelTableModel.fireTableDataChanged();
      }
    });
    channelTable.addMouseListener(new MouseAdapter() {
      @Override
      @SuppressWarnings("unused")
      public void mouseClicked(MouseEvent e) {
        int mods = e.getModifiersEx();
        if (e.getButton() == 3) {
          showPopupMenu(e);
        } else if (e.getClickCount() == 2) {
          Point p = e.getPoint();
          int row = rowAtPoint(p);
          ChannelData cd = channelList.get(row);
          boolean got_one = false;
          for (SpyPlugin plugin : plugins) {
            if (!got_one && plugin.canHandle(cd.fingerprint)) {
              // start the plugin
              (new PluginStarter(plugin, cd)).getAction().actionPerformed(null);
              got_one = true;
            }
          }
          if (!got_one)
            createViewer(channelList.get(row));
        }
      }
    });
    jif.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        System.out.println("Spy quitting");
        close(); // Added by Jen
        // System.exit(0); // jan deactivated this
      }
    });
    ClassDiscoverer.findClasses(new PluginClassVisitor());
    System.out.println("Found " + plugins.size() + " plugins");
    for (SpyPlugin plugin : plugins) {
      System.out.println(" " + plugin);
    }
  }

  public void close() {
    // use try because user might close the window itself
    try {
      lcm.close(); // Added by Jen
    } catch (Exception e) {
    }
    thread.interrupt(); // Added by Jen
    jif.setVisible(false);
    jif.dispose();
  }

  class PluginClassVisitor implements ClassDiscoverer.ClassVisitor {
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void classFound(String jar, Class cls) {
      Class interfaces[] = cls.getInterfaces();
      for (Class iface : interfaces) {
        if (iface.equals(SpyPlugin.class)) {
          try {
            Constructor c = cls.getConstructor(new Class[0]);
            SpyPlugin plugin = (SpyPlugin) c.newInstance(new Object[0]);
            plugins.add(plugin);
          } catch (Exception ex) {
            System.out.println("ex: " + ex);
          }
        }
      }
    }
  }

  void createViewer(ChannelData cd) {
    if (cd.viewerFrame != null && !cd.viewerFrame.isVisible()) {
      cd.viewerFrame.dispose();
      cd.viewer = null;
    }
    if (cd.viewer == null) {
      cd.viewerFrame = new JFrame(cd.name);
      cd.viewer = new ObjectPanel(cd.name, chartData);
      // cd.viewer = new ObjectViewer(cd.name, cd.cls, null);
      cd.viewerFrame.setLayout(new BorderLayout());
      // default scroll speed is too slow, so increase it
      JScrollPane viewerScrollPane = new JScrollPane(cd.viewer);
      viewerScrollPane.getVerticalScrollBar().setUnitIncrement(16);
      // we need to tell the viewer what its viewport is so that it can
      // make smart decisions about which elements are in view of the user
      // so it can avoid drawing items outside the view
      cd.viewer.setViewport(viewerScrollPane.getViewport());
      cd.viewerFrame.add(viewerScrollPane, BorderLayout.CENTER);
      cd.viewer.setObject(cd.last, cd.last_utime);
      // jdp.add(cd.viewerFrame);
      cd.viewerFrame.setSize(650, 400);
      cd.viewerFrame.setLocationByPlatform(true);
      cd.viewerFrame.setVisible(true);
    } else {
      cd.viewerFrame.setVisible(true);
      // cd.viewerFrame.moveToFront();
    }
  }

  static final long utime_now() {
    return System.nanoTime() / 1000;
  }

  class MySubscriber implements LCMSubscriber {
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void messageReceived(LCM lcm, String channel, LCMDataInputStream dins) {
      Object o = null;
      ChannelData cd = channelMap.get(channel);
      int msg_size = 0;
      try {
        msg_size = dins.available();
        long fingerprint = (msg_size >= 8) ? dins.readLong() : -1;
        dins.reset();
        Class cls = handlers.getClassByFingerprint(fingerprint);
        if (cd == null) {
          cd = new ChannelData();
          cd.name = channel;
          cd.cls = cls;
          cd.fingerprint = fingerprint;
          cd.row = channelList.size();
          synchronized (channelList) {
            channelMap.put(channel, cd);
            channelList.add(cd);
            _channelTableModel.fireTableDataChanged();
          }
        } else {
          if (cls != null && cd.cls != null && !cd.cls.equals(cls)) {
            System.out.println("WARNING: Class changed for channel " + channel);
            cd.nerrors++;
          }
        }
        long utime = utime_now();
        long interval = utime - cd.last_utime;
        cd.hz_min_interval = Math.min(cd.hz_min_interval, interval);
        cd.hz_max_interval = Math.max(cd.hz_max_interval, interval);
        cd.hz_bytes += msg_size;
        totalBytes += msg_size;
        totalBytesRate += msg_size;
        cd.last_utime = utime;
        cd.nreceived++;
        o = cd.cls.getConstructor(DataInput.class).newInstance(dins);
        cd.last = o;
        if (cd.viewer != null)
          cd.viewer.setObject(o, cd.last_utime);
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

  class HzThread extends Thread {
    public HzThread() {
      // setDaemon(true); // Modified by Jen
      setName("LCM-Spy"); // Modified by Jen
    }

    @Override
    public void run() {
      while (!isInterrupted()) {
        long utime = utime_now();
        synchronized (channelList) {
          for (ChannelData cd : channelList) {
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
        int selrow = channelTable.getSelectedRow();
        channelTableModel.fireTableDataChanged();
        if (selrow >= 0)
          channelTable.setRowSelectionInterval(selrow, selrow);
        { // TODO not the best design...
          String rate = FriendlyFormat.byteSize(totalBytesRate, true);
          String total = FriendlyFormat.byteSize(totalBytes, true);
          jLabelInfo.setText(rate + "/s " + total);
        }
        totalBytesRate = 0;
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          interrupt();
        }
      }
    }
  }

  class DefaultViewer extends AbstractAction {
    ChannelData cd;

    public DefaultViewer(ChannelData cd) {
      super("Structure Viewer...");
      this.cd = cd;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      createViewer(cd);
    }
  }

  int rowAtPoint(Point p) {
    int physicalRow = channelTable.rowAtPoint(p);
    return channelTableModel.modelIndex(physicalRow);
  }

  public void showPopupMenu(MouseEvent e) {
    Point p = e.getPoint();
    int row = rowAtPoint(p);
    ChannelData cd = channelList.get(row);
    JPopupMenu jm = new JPopupMenu("Viewers");
    int prow = channelTable.rowAtPoint(p);
    channelTable.setRowSelectionInterval(prow, prow);
    jm.add(new DefaultViewer(cd));
    if (cd.cls != null) {
      for (SpyPlugin plugin : plugins) {
        if (plugin.canHandle(cd.fingerprint)) {
          jm.add(new PluginStarter(plugin, cd).getAction());
          // jm.add(plugin.getAction(this_desktop_pane, cd));
        }
      }
    }
    jm.show(channelTable, e.getX(), e.getY());
  }

  public static void usage() {
    System.err.println("usage: lcm-spy [options]");
    System.err.println("");
    System.err.println("lcm-spy is the Lightweight Communications and Marshalling traffic ");
    System.err.println("inspection utility.  It is a graphical tool for viewing messages received on ");
    System.err.println("an LCM network, and is analagous to tools like Ethereal/Wireshark and tcpdump");
    System.err.println("in that it is able to inspect all LCM messages received and provide information");
    System.err.println("and statistics on the channels used.");
    System.err.println("");
    System.err.println("When given appropriate LCM type definitions, lcm-spy is able to");
    System.err.println("automatically detect and decode messages, and can display the individual fields");
    System.err.println("of recognized messages.  lcm-spy is limited to displaying statistics for");
    System.err.println("unrecognized messages.");
    System.err.println("");
    System.err.println("Options:");
    System.err.println("  -l, --lcm-url=URL      Use the specified LCM URL");
    System.err.println("  -h, --help             Shows this help text and exits");
    System.err.println("");
    System.exit(1);
  }

  public static void main(String args[]) {
    // check if the JRE is supplied by gcj, and warn the user if it is.
    if (System.getProperty("java.vendor").indexOf("Free Software Foundation") >= 0) {
      System.err.println("WARNING: Detected gcj. lcm-spy is not known to work well with gcj.");
      System.err.println("         The Sun JRE is recommended.");
    }
    String lcmurl = null;
    for (int optind = 0; optind < args.length; optind++) {
      String c = args[optind];
      if (c.equals("-h") || c.equals("--help")) {
        usage();
      } else if (c.equals("-l") || c.equals("--lcm-url") || c.startsWith("--lcm-url=")) {
        String optarg = null;
        if (c.startsWith("--lcm-url=")) {
          optarg = c.substring(10);
        } else if (optind < args.length) {
          optind++;
          optarg = args[optind];
        }
        if (null == optarg) {
          usage();
        } else {
          lcmurl = optarg;
        }
      } else {
        usage();
      }
    }
    try {
      new Spy(lcmurl);
    } catch (IOException ex) {
      System.out.println(ex);
    }
  }
}
