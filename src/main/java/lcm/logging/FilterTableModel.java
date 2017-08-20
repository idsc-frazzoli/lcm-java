// code by lcm
// extracted and modified by jph
package lcm.logging;

import javax.swing.table.AbstractTableModel;

import lcm.logging.LogPlayerComponent.Filter;

class FilterTableModel extends AbstractTableModel {
  private static final String[] COLUMN_NAME = { "Log channel", "Playback channel", "Enable" };
  // ---
  private final LogPlayerComponent logPlayer;

  /** @param logPlayer */
  FilterTableModel(LogPlayerComponent logPlayer) {
    this.logPlayer = logPlayer;
  }

  @Override
  public int getRowCount() {
    return logPlayer.filters.size();
  }

  @Override
  public int getColumnCount() {
    return 3;
  }

  @Override
  public String getColumnName(int column) {
    return COLUMN_NAME[column];
  }

  @Override
  public Class<?> getColumnClass(int column) {
    switch (column) {
    case 0:
    case 1:
      return String.class;
    case 2:
      return Boolean.class;
    default:
      return null;
    }
  }

  @Override
  public Object getValueAt(int row, int column) {
    Filter filter = logPlayer.filters.get(row);
    switch (column) {
    case 0:
      return filter.inchannel;
    case 1:
      return filter.outchannel;
    case 2:
      return filter.enabled;
    default:
      return "??";
    }
  }

  @Override
  public boolean isCellEditable(int row, int column) {
    return column == 1 || column == 2;
  }

  @Override
  public void setValueAt(Object v, int row, int column) {
    Filter filter = logPlayer.filters.get(row);
    if (column == 1)
      filter.outchannel = (String) v;
    if (column == 2)
      filter.enabled = (Boolean) v;
  }
}