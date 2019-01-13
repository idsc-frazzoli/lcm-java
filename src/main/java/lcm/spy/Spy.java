// code by lcm, njenwei, and jph
package lcm.spy;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.Constructor;
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
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import lcm.lcm.LCM;
import lcm.util.ClassDiscovery;
import lcm.util.ClassPaths;
import lcm.util.ClassVisitor;
import lcm.util.LcmStaticHelper;
import lcm.util.TableSorter;

/** Spy main class */
// FIXME process does not terminate if object panel is opened
public class Spy {
  private LCM lcm;
  final LcmTypeDatabase lcmTypeDatabase; // accessed in UniversalSubscriber
  final Map<String, ChannelData> channelMap = new HashMap<>();
  final List<ChannelData> channelList = new ArrayList<>();
  final ChannelTableModel _channelTableModel = new ChannelTableModel(channelList);
  final TableSorter channelTableModel = new TableSorter(_channelTableModel);
  final JTable channelTable = new JTable(channelTableModel);
  final ChartData chartData;
  private final List<SpyPlugin> plugins = new ArrayList<>();
  public final JFrame jFrame = new JFrame("LCM Spy");
  private final JButton clearButton = new JButton("Clear");
  private final HzThread hzThread; // Added by Jen
  final JLabel jLabelInfo = new JLabel();
  long totalBytes = 0;
  long totalBytesRate = 0;

  public Spy(String lcmurl) throws IOException {
    this(lcmurl, LcmTypeDatabaseBuilder.create(ClassPaths.getDefault()));
  }

  public Spy(String lcmurl, LcmTypeDatabase lcmTypeDatabase) throws IOException {
    // sortedChannelTableModel.addMouseListenerToHeaderInTable(channelTable);
    channelTableModel.setTableHeader(channelTable.getTableHeader());
    channelTableModel.setSortingStatus(0, TableSorter.ASCENDING);
    this.lcmTypeDatabase = lcmTypeDatabase;
    {
      TableColumnModel tcm = channelTable.getColumnModel();
      DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();
      dtcr.setHorizontalAlignment(SwingConstants.RIGHT);
      tcm.getColumn(0).setMinWidth(140);
      tcm.getColumn(1).setMinWidth(140);
      tcm.getColumn(2).setMaxWidth(100);
      tcm.getColumn(2).setCellRenderer(dtcr);
      tcm.getColumn(3).setMaxWidth(100);
      tcm.getColumn(3).setCellRenderer(dtcr);
      tcm.getColumn(4).setMaxWidth(100);
      tcm.getColumn(4).setCellRenderer(dtcr);
      tcm.getColumn(5).setMaxWidth(100);
      tcm.getColumn(5).setCellRenderer(dtcr);
      tcm.getColumn(6).setMaxWidth(100);
      tcm.getColumn(6).setCellRenderer(dtcr);
      tcm.getColumn(7).setCellRenderer(dtcr);
    }
    jFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    jFrame.setLayout(new BorderLayout());
    jFrame.add(channelTable.getTableHeader(), BorderLayout.PAGE_START);
    {
      JToolBar jToolBar = new JToolBar();
      jToolBar.add(clearButton);
      jToolBar.addSeparator();
      jToolBar.add(jLabelInfo);
      jToolBar.setFloatable(false);
      jFrame.add(jToolBar, BorderLayout.NORTH);
    }
    jFrame.add(new JScrollPane(channelTable), BorderLayout.CENTER);
    chartData = new ChartData(utime_now());
    jFrame.setSize(800, 600);
    jFrame.setLocationByPlatform(true);
    jFrame.setVisible(true);
    if (null == lcmurl)
      lcm = new LCM();
    else
      lcm = new LCM(lcmurl);
    lcm.subscribeAll(new UniversalSubscriber(this));
    hzThread = new HzThread(this);
    hzThread.start();
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
      public void mouseClicked(MouseEvent mouseEvent) {
        int mods = mouseEvent.getModifiersEx();
        if (mouseEvent.getButton() == 3) {
          showPopupMenu(mouseEvent);
        } else if (mouseEvent.getClickCount() == 2) {
          Point p = mouseEvent.getPoint();
          int row = rowAtPoint(p);
          ChannelData cd = channelList.get(row);
          boolean got_one = false;
          for (SpyPlugin plugin : plugins) {
            if (!got_one && plugin.canHandle(cd.fingerprint)) {
              // start the plugin
              new PluginStarter(plugin, cd).getAction().actionPerformed(null);
              got_one = true;
            }
          }
          if (!got_one)
            createViewer(channelList.get(row));
        }
      }
    });
    jFrame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent windowEvent) {
        System.out.println("Spy quitting");
        close(); // added by Jen
      }
    });
    ClassDiscovery.execute(ClassPaths.getDefault(), new PluginClassVisitor());
    if (!plugins.isEmpty()) {
      System.out.println("Found " + plugins.size() + " plugins");
      for (SpyPlugin plugin : plugins)
        System.out.println(" " + plugin);
    }
  }

  public void close() {
    hzThread.interrupt(); // added by Jen
    // ---
    jFrame.setVisible(false);
    jFrame.dispose();
  }

  class PluginClassVisitor implements ClassVisitor {
    @Override
    public void classFound(String jar, Class<?> cls) {
      Class<?> interfaces[] = cls.getInterfaces();
      for (Class<?> iface : interfaces)
        if (iface.equals(SpyPlugin.class))
          try {
            Constructor<?> c = cls.getConstructor(new Class[0]);
            SpyPlugin plugin = (SpyPlugin) c.newInstance(new Object[0]);
            plugins.add(plugin);
          } catch (Exception ex) {
            System.out.println("ex: " + ex);
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

  public static void main(String args[]) {
    LcmStaticHelper.checkJre();
    String lcmurl = null;
    for (int optind = 0; optind < args.length; optind++) {
      String c = args[optind];
      if (c.equals("-h") || c.equals("--help")) {
        StaticHelper.spyUsage();
      } else if (c.equals("-l") || c.equals("--lcm-url") || c.startsWith("--lcm-url=")) {
        String optarg = null;
        if (c.startsWith("--lcm-url=")) {
          optarg = c.substring(10);
        } else if (optind < args.length) {
          optind++;
          optarg = args[optind];
        }
        if (null == optarg) {
          StaticHelper.spyUsage();
        } else {
          lcmurl = optarg;
        }
      } else {
        StaticHelper.spyUsage();
      }
    }
    try {
      new Spy(lcmurl);
    } catch (IOException ex) {
      System.out.println(ex);
    }
  }
}
