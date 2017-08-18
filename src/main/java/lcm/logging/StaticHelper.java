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
    Graphics2D graphics = im.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // g.setColor(backgroundColor);
    graphics.setColor(new Color(0, 0, 0, 0));
    // g.setColor(new Color(0,0,255,128));
    graphics.fillRect(0, 0, width, height);
    if (flip) {
      graphics.translate(width - 1, height / 2);
      graphics.scale(-height / 2, height / 2);
    } else {
      graphics.translate(0, height / 2);
      graphics.scale(height / 2, height / 2);
    }
    graphics.setStroke(new BasicStroke(0f));
    GeneralPath gp = new GeneralPath();
    gp.moveTo(0, -1);
    gp.lineTo(1, 0);
    gp.lineTo(0, 1);
    gp.lineTo(0, -1);
    graphics.setColor(fillColor);
    graphics.fill(gp);
    graphics.setColor(Color.black);
    // g.draw(gp);
    graphics.translate(.75, 0);
    graphics.setColor(fillColor);
    graphics.fill(gp);
    graphics.setColor(Color.black);
    // g.draw(gp);
    return im;
  }
}
