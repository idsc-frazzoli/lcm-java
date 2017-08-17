// refactored by jph
package lcm.spy;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;

class PluginStarter {
  private final SpyPlugin plugin;
  private final ChannelData cd;
  private final String name;

  public PluginStarter(SpyPlugin pluginIn, ChannelData cdIn) {
    plugin = pluginIn;
    cd = cdIn;
    Action thisAction = plugin.getAction(null, null);
    name = (String) thisAction.getValue("Name");
  }

  public Action getAction() {
    return new PluginStarterAction();
  }

  class PluginStarterAction extends AbstractAction {
    public PluginStarterAction() {
      super(name);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      // for historical reasons, plugins expect a JDesktopPane
      // here we create a JFrame, add a JDesktopPane, and start the
      // plugin by calling its actionPerformed method
      JFrame pluginFrame = new JFrame(cd.name);
      pluginFrame.setLayout(new BorderLayout());
      JDesktopPane pluginJdp = new JDesktopPane();
      pluginFrame.add(pluginJdp);
      pluginFrame.setSize(500, 400);
      pluginFrame.setLocationByPlatform(true);
      pluginFrame.setVisible(true);
      plugin.getAction(pluginJdp, cd).actionPerformed(null);
    }
  }
}