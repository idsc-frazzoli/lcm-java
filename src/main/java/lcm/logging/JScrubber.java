// code by lcm
package lcm.logging;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.MouseInputAdapter;

class JScrubber extends JComponent {
  static final int BARHEIGHT = 5;
  static final int KNOBSIZE = 10;
  static final int MARGIN = 10;
  static final int MIN_HEIGHT = KNOBSIZE + 2 * MARGIN + BARHEIGHT * 5;
  static final int BOOKMARK_HEIGHT = 15, BOOKMARK_WIDTH = 6;
  static final int CLICK_CLOSENESS = 5;
  static final int REPEAT_DOT_SIZE = 4;
  static final int BOOKMARK_PLAIN = 0, BOOKMARK_LREPEAT = 1, BOOKMARK_RREPEAT = 2;
  // ---
  private double position = 0.5;
  private double zoomfrac = 0.1;
  /** positions (0,1) representing the left and right end of the zoom scrubber */
  private double zoom0, zoom1;
  private int cy, cy2;
  private int lastDrawX = -1, lastDrawX2 = -1; // used to eliminate extra redraws
  private List<JScrubberListener> listeners = new ArrayList<>();
  private boolean inhibitGeometryChanges = false;
  private JPopupMenu popupMenu = new JPopupMenu();
  private double popupPosition;
  private int mouseDownRow = 0;

  class Bookmark {
    double position;
    int type;

    Bookmark(double position, int type) {
      this.position = position;
      this.type = type;
    }
  }

  List<Bookmark> bookmarks = new ArrayList<>();

  public JScrubber() {
    MyMouseAdapter ma = new MyMouseAdapter();
    addMouseListener(ma);
    addMouseMotionListener(ma);
    popupMenu.add(new PopupAction("Bookmark", BOOKMARK_PLAIN));
    popupMenu.add(new PopupAction("Right repeat :]", BOOKMARK_RREPEAT));
    popupMenu.add(new PopupAction("Left repeat [:", BOOKMARK_LREPEAT));
    popupMenu.addSeparator();
    int zooms[] = new int[] { 10, 20, 50, 100, 200, 500, 1000 };
    ButtonGroup group = new ButtonGroup();
    for (int i = 0; i < zooms.length; i++) {
      JRadioButtonMenuItem jmi = new JRadioButtonMenuItem("Zoom " + zooms[i] + "x");
      jmi.addActionListener(new ZoomAction("foo", zooms[i]));
      group.add(jmi);
      popupMenu.add(jmi);
      if (i == 3) {
        jmi.setSelected(true);
        zoomfrac = 1.0 / zooms[i];
      }
    }
    popupMenu.addSeparator();
    popupMenu.add(new ExportAction());
  }

  public void clearBookmarks() {
    bookmarks = new ArrayList<>();
    repaint();
  }

  public void addBookmark(int type, double position) {
    bookmarks.add(new Bookmark(position, type));
    repaint();
  }

  public List<Bookmark> getBookmarks() {
    return bookmarks;
  }

  class ExportAction extends AbstractAction {
    public ExportAction() {
      super("Export log snippet...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      Bookmark b0 = new Bookmark(0, BOOKMARK_PLAIN);
      Bookmark b1 = new Bookmark(1, BOOKMARK_PLAIN);
      if (bookmarks.size() == 0) {
        System.out.println("didn't find bookmark region");
        return;
      } else //
      if (bookmarks.size() == 1) {
        // if there is only one bookmark, use from beginning/end to it
        Bookmark b = bookmarks.get(0);
        if (position > b.position)
          b0 = b;
        else
          b1 = b;
      } else if (bookmarks.size() == 2) {
        // if there are only two bookmarks, just use those.
        b0 = bookmarks.get(0);
        b1 = bookmarks.get(1);
        if (b0.position > b1.position) { // b0 should be before b1
          @SuppressWarnings("unused")
          Bookmark swp = b0;
          b0 = b1;
          b1 = b0;
        }
      } else {
        // find previous and next book marks.
        for (Bookmark b : bookmarks) {
          if (b.position < position && b0.position < b.position)
            b0 = b;
          if (b.position > position && b1.position > b.position)
            b1 = b;
        }
      }
      for (JScrubberListener jsl : listeners)
        jsl.scrubberExportRegion(JScrubber.this, b0.position, b1.position);
    }
  }

  class ZoomAction extends AbstractAction {
    int zoom;

    public ZoomAction(String name, int zoom) {
      super(name);
      this.zoom = zoom;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      setZoomFraction(1.0 / zoom);
    }
  }

  class PopupAction extends AbstractAction {
    int op;

    public PopupAction(String name, int op) {
      super(name);
      this.op = op;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      bookmarks.add(new Bookmark(popupPosition, op));
      repaint();
    }
  }

  public double getZoomFraction() {
    return zoomfrac;
  }

  public void setZoomFraction(double f) {
    zoomfrac = f;
    updateGeometry();
    repaint();
  }

  public void addScrubberListener(JScrubberListener l) {
    listeners.add(l);
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(100, MIN_HEIGHT);
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension d = super.getPreferredSize();
    return new Dimension((int) d.getWidth(), (int) Math.max(d.getHeight(), MIN_HEIGHT));
  }

  // convert a position to an X coordinate for the main scrubber
  int getX(double v) {
    return (int) (MARGIN + v * (getWidth() - MARGIN * 2));
  }

  // convert a position to an X coordinate for the zoom scrubber
  int getX2(double v) {
    return getX((v - zoom0) / (zoom1 - zoom0));
  }

  // convert an X coordinate to a position for the main scrubber
  double getPosition(int x) {
    double pos = ((double) x - MARGIN) / (getWidth() - 2 * MARGIN);
    if (pos < 0)
      pos = 0;
    if (pos > 1)
      pos = 1;
    return pos;
  }

  // convert an X coordinate to a position for the zoomed-in scrubber
  double getPosition2(int x) {
    double pos = ((double) x - MARGIN) / (getWidth() - 2 * MARGIN);
    if (pos < 0)
      return zoom0;
    if (pos > 1)
      return zoom1;
    double pos2 = pos * (zoom1 - zoom0) + zoom0;
    return pos2;
  }

  int getRow(int x, int y) {
    return y > ((cy + cy2) / 2) ? 1 : 0;
  }

  double getPosition(int x, int y) {
    double position;
    if (getRow(x, y) == 0)
      position = getPosition(x);
    else
      position = getPosition2(x);
    return position;
  }

  /** Get position for a mouse click on a particular row Set row = 1 for the
   * zoomed in slider. */
  double getPosition(int x, int y, int row) {
    if (row == 1)
      return getPosition2(x);
    return getPosition(x);
  }

  void updateGeometry() {
    if (inhibitGeometryChanges)
      return;
    if (position < zoomfrac / 2) {
      zoom0 = 0;
      zoom1 = zoomfrac;
    } else if (position > 1 - zoomfrac / 2) {
      zoom0 = 1 - zoomfrac;
      zoom1 = 1;
    } else {
      zoom0 = position - zoomfrac / 2;
      zoom1 = position + zoomfrac / 2;
    }
    cy = getHeight() / 3;
    cy2 = cy + BARHEIGHT * 4;
  }

  void drawBookmark(Graphics g, Bookmark b, int x, int cy) {
    g.setColor(new Color(0, 200, 0));
    g.fillRect(x - BOOKMARK_WIDTH / 2, cy - BOOKMARK_HEIGHT / 2, BOOKMARK_WIDTH, BOOKMARK_HEIGHT);
    g.setColor(new Color(0, 100, 0));
    g.drawRect(x - BOOKMARK_WIDTH / 2, cy - BOOKMARK_HEIGHT / 2, BOOKMARK_WIDTH, BOOKMARK_HEIGHT);
    if (b.type == BOOKMARK_LREPEAT) {
      g.setColor(new Color(0, 50, 0));
      g.fillRect(x + BOOKMARK_WIDTH / 2, cy - BOOKMARK_HEIGHT / 2, REPEAT_DOT_SIZE, REPEAT_DOT_SIZE);
      g.fillRect(x + BOOKMARK_WIDTH / 2, cy + BOOKMARK_HEIGHT / 2 - REPEAT_DOT_SIZE, REPEAT_DOT_SIZE, REPEAT_DOT_SIZE);
    }
    if (b.type == BOOKMARK_RREPEAT) {
      g.setColor(new Color(0, 50, 0));
      g.fillRect(x - BOOKMARK_WIDTH / 2 - REPEAT_DOT_SIZE, cy - BOOKMARK_HEIGHT / 2, REPEAT_DOT_SIZE, REPEAT_DOT_SIZE);
      g.fillRect(x - BOOKMARK_WIDTH / 2 - REPEAT_DOT_SIZE, cy + BOOKMARK_HEIGHT / 2 - REPEAT_DOT_SIZE, REPEAT_DOT_SIZE, REPEAT_DOT_SIZE);
    }
  }

  @Override
  @SuppressWarnings("unused")
  public void paint(Graphics g) {
    Graphics2D g2d = (Graphics2D) g;
    int width = getWidth(), height = getHeight();
    int margin = 4;
    int barheight = 10;
    g.setColor(getBackground());
    g.fillRect(0, 0, width, height);
    double position2 = (position - zoom0) / zoomfrac;
    // draw the trapezoid between the two bars
    g.setColor(Color.lightGray);
    GeneralPath gp = new GeneralPath();
    gp.moveTo(getX(zoom0), cy + BARHEIGHT / 2 + 1);
    gp.lineTo(getX(zoom1), cy + BARHEIGHT / 2 + 1);
    gp.lineTo(getX(1.0), cy2 - BARHEIGHT / 2);
    gp.lineTo(getX(0.0), cy2 - BARHEIGHT / 2);
    gp.closePath();
    // g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
    // RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.fill(gp);
    // draw the bar
    g.setColor(new Color(150, 150, 255));
    g.fillRect(MARGIN, cy - BARHEIGHT / 2, width - 2 * MARGIN, BARHEIGHT);
    g.setColor(Color.blue);
    g.drawRect(MARGIN, cy - BARHEIGHT / 2, width - 2 * MARGIN, BARHEIGHT);
    g.setColor(new Color(150, 255, 255));
    g.fillRect(MARGIN, cy2 - BARHEIGHT / 2, width - 2 * MARGIN, BARHEIGHT);
    g.setColor(Color.blue);
    g.drawRect(MARGIN, cy2 - BARHEIGHT / 2, width - 2 * MARGIN, BARHEIGHT);
    g.setColor(Color.darkGray);
    g.fillRect(getX(zoom0), cy - BARHEIGHT * 1 / 3, getX(zoom1) - getX(zoom0), BARHEIGHT);
    for (Bookmark b : bookmarks) {
      drawBookmark(g, b, getX(b.position), cy);
      if (b.position >= zoom0 && b.position <= zoom1)
        drawBookmark(g, b, getX2(b.position), cy2);
    }
    // draw the knob
    g.setColor(Color.yellow);
    g.fillOval(getX(position) - KNOBSIZE / 2, cy - KNOBSIZE / 2, KNOBSIZE, KNOBSIZE);
    g.setColor(Color.black);
    g.drawOval(getX(position) - KNOBSIZE / 2, cy - KNOBSIZE / 2, KNOBSIZE, KNOBSIZE);
    g.setColor(Color.yellow);
    g.fillOval(getX2(position) - KNOBSIZE / 2, cy2 - KNOBSIZE / 2, KNOBSIZE, KNOBSIZE);
    g.setColor(Color.black);
    g.drawOval(getX2(position) - KNOBSIZE / 2, cy2 - KNOBSIZE / 2, KNOBSIZE, KNOBSIZE);
    lastDrawX = getX(position);
    lastDrawX2 = getX2(position);
  }

  void userSet(double newpos) {
    position = newpos;
    for (JScrubberListener l : listeners)
      l.scrubberMovedByUser(this, position);
    updateGeometry();
    repaint();
  }

  synchronized public void set(double pos) {
    double oldpos = this.position;
    this.position = pos;
    updateGeometry();
    // now we've updated geometry, maybe we don't need to draw.
    if (Math.abs(getX(position) - lastDrawX) > 1 || Math.abs(getX2(position) - lastDrawX2) > 1)
      repaint();
    for (Bookmark bookmark : bookmarks) {
      if (bookmark.type == BOOKMARK_RREPEAT && //
          bookmark.position > oldpos && //
          bookmark.position <= pos) {
        Bookmark lrepeat = null;
        // find most recent LREPEAT
        for (Bookmark bl : bookmarks)
          if (bl.position < bookmark.position && //
              bl.type == BOOKMARK_LREPEAT && //
              (lrepeat == null || bl.position > lrepeat.position))
            lrepeat = bl;
        double lposition = lrepeat == null //
            ? 0
            : lrepeat.position;
        for (JScrubberListener jScrubberListener : listeners) {
          jScrubberListener.scrubberPassedRepeat(this, bookmark.position, lposition);
        }
      }
    }
  }

  class MyMouseAdapter extends MouseInputAdapter {
    Bookmark trackbookmark;

    Bookmark findBookmark(double position, double tolerance) {
      double minerr = tolerance;
      Bookmark best = null;
      for (Bookmark bookmark : bookmarks) {
        double thiserr = Math.abs(position - bookmark.position);
        if (thiserr < minerr) {
          best = bookmark;
          minerr = thiserr;
        }
      }
      return best;
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
      mouseDownRow = getRow(mouseEvent.getX(), mouseEvent.getY());
      double position = getPosition(mouseEvent.getX(), mouseEvent.getY(), mouseDownRow);
      double tolerance = getPosition(mouseEvent.getX() + CLICK_CLOSENESS, mouseEvent.getY(), mouseDownRow) - position;
      if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
        trackbookmark = findBookmark(position, tolerance);
      }
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
      if (trackbookmark != null) {
        if (trackbookmark.position == 0 || trackbookmark.position == 1) {
          bookmarks.remove(trackbookmark);
          repaint();
        }
        trackbookmark = null;
      }
      inhibitGeometryChanges = false;
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
      int mods = mouseEvent.getModifiersEx();
      boolean shift = (mods & InputEvent.SHIFT_DOWN_MASK) > 0;
      boolean ctrl = (mods & InputEvent.CTRL_DOWN_MASK) > 0;
      boolean alt = shift & ctrl;
      ctrl = ctrl & (!alt);
      shift = shift & (!alt);
      @SuppressWarnings("unused")
      boolean nomods = !(shift | ctrl | alt);
      double tolerance = getPosition(mouseEvent.getX() + CLICK_CLOSENESS, mouseEvent.getY()) - position;
      double position = getPosition(mouseEvent.getX(), mouseEvent.getY());
      if (mouseEvent.getButton() == 1) {
        Bookmark nearest = findBookmark(position, tolerance);
        if (nearest == null)
          userSet(position);
        else
          userSet(nearest.position);
      }
      if (mouseEvent.getButton() == 3) {
        popupPosition = position;
        popupMenu.show(JScrubber.this, mouseEvent.getX(), mouseEvent.getY());
        /* int err = Math.abs(e.getX() - getX(position));
         * 
         * // if they clicked near the current cursor, create // a
         * bookmark exactly where the cursor is int type =
         * BOOKMARK_PLAIN; if (shift) type = BOOKMARK_LREPEAT; if (ctrl)
         * type = BOOKMARK_RREPEAT;
         * 
         * if (err < CLICK_CLOSENESS) bookmarks.add(new
         * Bookmark(position, type)); else bookmarks.add(new
         * Bookmark(position, type));
         * 
         * repaint(); */
      }
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
      double position = getPosition(mouseEvent.getX(), mouseEvent.getY(), mouseDownRow);
      if (mouseDownRow == 1)
        inhibitGeometryChanges = true;
      else
        inhibitGeometryChanges = false;
      if ((mouseEvent.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
        userSet(position);
      }
      if ((mouseEvent.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
        if (trackbookmark != null) {
          trackbookmark.position = position;
          repaint();
        }
      }
    }
  }
}
