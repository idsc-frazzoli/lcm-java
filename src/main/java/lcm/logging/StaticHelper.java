// code by jph
package lcm.logging;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

enum StaticHelper {
  ;
  static BufferedImage makeArrowImage(Color fillColor, Color backgroundColor, boolean flip) {
    int height = 18, width = 18;
    BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = im.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // g.setColor(backgroundColor);
    g.setColor(new Color(0, 0, 0, 0));
    // g.setColor(new Color(0,0,255,128));
    g.fillRect(0, 0, width, height);
    if (flip) {
      g.translate(width - 1, height / 2);
      g.scale(-height / 2, height / 2);
    } else {
      g.translate(0, height / 2);
      g.scale(height / 2, height / 2);
    }
    g.setStroke(new BasicStroke(0f));
    GeneralPath gp = new GeneralPath();
    gp.moveTo(0, -1);
    gp.lineTo(1, 0);
    gp.lineTo(0, 1);
    gp.lineTo(0, -1);
    g.setColor(fillColor);
    g.fill(gp);
    g.setColor(Color.black);
    // g.draw(gp);
    g.translate(.75, 0);
    g.setColor(fillColor);
    g.fill(gp);
    g.setColor(Color.black);
    // g.draw(gp);
    return im;
  }
}
