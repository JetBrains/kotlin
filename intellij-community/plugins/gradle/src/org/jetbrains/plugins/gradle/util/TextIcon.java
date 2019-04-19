package org.jetbrains.plugins.gradle.util;

import com.intellij.ide.ui.UISettings;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Denis Zhdanov
 */
public class TextIcon implements Icon {

  @NotNull private final String myText;

  private final int myControlWidth;
  private final int myControlHeight;
  
  private int myTextHeight;

  public TextIcon(@NotNull String text) {
    myText = text;
    JLabel label = new JLabel("");
    Font font = label.getFont();
    FontMetrics metrics = label.getFontMetrics(font);
    myControlWidth = metrics.stringWidth(text) + 4;
    myControlHeight = font.getSize();
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    UISettings.setupAntialiasing(g);
    if (myTextHeight <= 0) {
      myTextHeight = g.getFont().createGlyphVector(((Graphics2D)g).getFontRenderContext(), myText).getPixelBounds(null, 0, 0).height;
    }

    g.setColor(UIUtil.getLabelForeground());
    g.drawString(myText, x + 2, y + myControlHeight - ((myControlHeight - myTextHeight) / 2));
  }

  @Override
  public int getIconWidth() {
    return myControlWidth;
  }

  @Override
  public int getIconHeight() {
    return myControlHeight;
  }
}
