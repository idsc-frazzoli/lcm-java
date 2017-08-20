// code by lcm
package lcm.spy;

import java.util.List;

import javax.swing.table.AbstractTableModel;

class ChannelTableModel extends AbstractTableModel {
  private final List<ChannelData> channelList;

  public ChannelTableModel(List<ChannelData> channelList) {
    this.channelList = channelList;
  }

  @Override
  public int getColumnCount() {
    return 8;
  }

  @Override
  public int getRowCount() {
    return channelList.size();
  }

  @Override
  public Object getValueAt(int row, int col) {
    ChannelData cd = channelList.get(row);
    // new Double(value)
    if (cd == null)
      return "";
    switch (col) {
    case 0:
      return cd.name;
    case 1:
      if (cd.cls == null)
        return String.format("?? %016x", cd.fingerprint);
      String s = cd.cls.getName();
      return s.substring(s.lastIndexOf('.') + 1);
    case 2:
      // return "" + cd.nreceived;
      return new Long(cd.nreceived);
    case 3:
      // return new Double(cd.hz);
      return String.format("%6.2f", cd.hz);
    case 4:
      return String.format("%6.2f", 1000.0 / cd.hz); // cd.max_interval/1000.0);
    case 5:
      return String.format("%6.2f", (cd.max_interval - cd.min_interval) / 1000.0);
    case 6:
      return String.format("%6.2f", (cd.bandwidth / 1024.0));
    case 7:
      return "" + cd.nerrors;
    default:
      return "???";
    }
  }

  private static final String[] COLUMNS = { "Channel", "Type", "Num Msgs", "[Hz]", "[1/Hz]", "ms", "[kB/s]", "Undecodable" };

  @Override
  public String getColumnName(int col) {
    return COLUMNS[col];
  }
}