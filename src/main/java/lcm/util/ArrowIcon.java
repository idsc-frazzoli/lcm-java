// code by jph
package lcm.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/* package */ class ArrowIcon implements Icon {
  private final boolean descending;
  private final int size;
  private final int priority;

  public ArrowIcon(boolean descending, int size, int priority) {
    this.descending = descending;
    this.size = size;
    this.priority = priority;
  }

  @Override // from Icon
  public void paintIcon(Component component, Graphics graphics, int x, int y) {
    Color color = component == null ? Color.GRAY : component.getBackground();
    // In a compound sort, make each succesive triangle 20%
    // smaller than the previous one.
    int dx = (int) (size / 2 * Math.pow(0.8, priority));
    int dy = descending ? dx : -dx;
    // Align icon (roughly) with font baseline.
    y = y + 5 * size / 6 + (descending ? -dy : 0);
    int shift = descending ? 1 : -1;
    graphics.translate(x, y);
    // Right diagonal.
    graphics.setColor(color.darker());
    graphics.drawLine(dx / 2, dy, 0, 0);
    graphics.drawLine(dx / 2, dy + shift, 0, shift);
    // Left diagonal.
    graphics.setColor(color.brighter());
    graphics.drawLine(dx / 2, dy, dx, 0);
    graphics.drawLine(dx / 2, dy + shift, dx, shift);
    // Horizontal line.
    graphics.setColor(descending ? color.darker().darker() : color.brighter().brighter());
    graphics.drawLine(dx, 0, 0, 0);
    graphics.setColor(color);
    graphics.translate(-x, -y);
  }

  @Override // from Icon
  public int getIconWidth() {
    return size;
  }

  @Override // from Icon
  public int getIconHeight() {
    return size;
  }
}