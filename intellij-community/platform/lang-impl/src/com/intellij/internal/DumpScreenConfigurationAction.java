/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.internal;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScreenUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import javax.swing.Action;
import javax.swing.JComponent;

/**
 * @author Sergey.Malenkov
 */
public class DumpScreenConfigurationAction extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(DumpScreenConfigurationAction.class);

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    new ScreenDialog(event).show();
  }

  private static Rectangle minimize(Rectangle bounds) {
    return new Rectangle(bounds.x / 10, bounds.y / 10, bounds.width / 10, bounds.height / 10);
  }

  private static void append(StringBuilder sb, GraphicsDevice device) {
    append(sb, "id", device.getIDstring());
    append(sb, "type", getTypeName(device.getType()));
    append(sb, device.getDisplayMode());
    GraphicsConfiguration configuration = device.getDefaultConfiguration();
    append(sb, "outer", configuration.getBounds());
    append(sb, "inner", ScreenUtil.getScreenRectangle(configuration));
    append(sb, "default image", configuration.getImageCapabilities());
    BufferCapabilities capabilities = configuration.getBufferCapabilities();
    append(sb, "front buffer image", capabilities.getFrontBufferCapabilities());
    append(sb, "back buffer image", capabilities.getBackBufferCapabilities());
    sb.append("page flipping: ").append(capabilities.getFlipContents());
    if (capabilities.isFullScreenRequired()) {
      sb.append("; full-screen exclusive mode is required");
    }
    if (capabilities.isMultiBufferAvailable()) {
      sb.append("; more than two buffers can be used");
    }
    sb.append("\n");
  }

  private static void append(StringBuilder sb, String name, String text) {
    sb.append(name).append(": ").append(text).append("\n");
  }

  private static void append(StringBuilder sb, String name, Rectangle bounds) {
    sb.append(name);
    sb.append(": x=").append(bounds.x);
    sb.append(", y=").append(bounds.y);
    sb.append(", width=").append(bounds.width);
    sb.append(", height=").append(bounds.height);
    sb.append("\n");
  }

  private static void append(StringBuilder sb, DisplayMode mode) {
    sb.append("mode: ").append(mode.getWidth()).append("x").append(mode.getHeight());
    sb.append("; bit depth=").append(mode.getBitDepth());
    sb.append("; refresh rate=").append(mode.getRefreshRate());
    sb.append("\n");
  }

  private static void append(StringBuilder sb, String name, ImageCapabilities capabilities) {
    if (capabilities != null) {
      sb.append(name).append(": accelerated=").append(capabilities.isAccelerated());
      if (capabilities.isTrueVolatile()) {
        sb.append("; true volatile");
      }
      sb.append("\n");
    }
  }

  private static String getTypeName(int type) {
    if (type == 0) return "raster screen";
    if (type == 1) return "printer";
    if (type == 2) return "image buffer";
    return "unknown: " + type;
  }

  private static final class ScreenDialog extends DialogWrapper {
    private ScreenDialog(AnActionEvent event) {
      super(event.getProject());
      init();
      setOKButtonText("Dump");
      setTitle("Screen Configuration");
    }

    @Override
    protected JComponent createCenterPanel() {
      return new ScreenView();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
      return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
      StringBuilder sb = new StringBuilder();
      GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
      append(sb, "number of devices", Integer.toString(devices.length));
      for (GraphicsDevice device : devices) {
        append(sb.append("\n"), device);
      }
      LOG.warn(sb.toString());
      super.doOKAction();
    }
  }

  private static final class ScreenInfo {
    private final Rectangle myOuterBounds = new Rectangle();
    private final Rectangle myInnerBounds = new Rectangle();

    private boolean update(GraphicsConfiguration configuration) {
      boolean updated = false;
      Rectangle outer = minimize(configuration.getBounds());
      if (!myOuterBounds.equals(outer)) {
        myOuterBounds.setBounds(outer);
        updated = true;
      }
      Rectangle inner = minimize(ScreenUtil.getScreenRectangle(configuration));
      if (!myInnerBounds.equals(inner)) {
        myInnerBounds.setBounds(inner);
        updated = true;
      }
      return updated;
    }
  }

  private static final class ScreenView extends JComponent {
    private final ArrayList<ScreenInfo> myScreenList = new ArrayList<>();
    private final Rectangle myBounds = new Rectangle();

    private boolean update() {
      boolean updated = false;
      GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
      while (devices.length > myScreenList.size()) {
        myScreenList.add(new ScreenInfo());
        updated = true;
      }
      while (devices.length < myScreenList.size()) {
        myScreenList.remove(devices.length);
        updated = true;
      }
      for (int i = 0; i < devices.length; i++) {
        if (myScreenList.get(i).update(devices[i].getDefaultConfiguration())) {
          updated = true;
        }
      }
      if (updated) {
        int minX = 0;
        int maxX = 0;
        int minY = 0;
        int maxY = 0;
        for (ScreenInfo info : myScreenList) {
          int x = info.myOuterBounds.x;
          if (minX > x) {
            minX = x;
          }
          x += info.myOuterBounds.width;
          if (maxX < x) {
            maxX = x;
          }
          int y = info.myOuterBounds.y;
          if (minY > y) {
            minY = y;
          }
          y += info.myOuterBounds.height;
          if (maxY < y) {
            maxY = y;
          }
        }
        myBounds.setBounds(minX, minY, maxX - minX, maxY - minY);
      }
      return updated;
    }

    @Override
    protected void paintComponent(Graphics g) {
      if (update()) {
        setPreferredSize(myBounds.getSize());
        setMinimumSize(myBounds.getSize());
        revalidate();
        repaint();
      }
      g = g.create();
      if (g instanceof Graphics2D) {
        UISettings.setupAntialiasing(g);
      }
      for (int i = 0; i < myScreenList.size(); i++) {
        ScreenInfo info = myScreenList.get(i);

        Rectangle bounds = info.myOuterBounds;
        int x = bounds.x - myBounds.x + getX();
        int y = bounds.y - myBounds.y + getY();
        g.setColor(JBColor.BLUE);
        g.fillRect(x, y, bounds.width, bounds.height);

        bounds = info.myInnerBounds;
        x = bounds.x - myBounds.x + getX();
        y = bounds.y - myBounds.y + getY();
        g.setColor(JBColor.BLACK);
        g.fillRect(x, y, bounds.width, bounds.height);

        String id = String.valueOf(i + 1);
        int size = Math.min(bounds.width << 1, bounds.height);
        g.setColor(JBColor.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, size));
        FontMetrics fm = g.getFontMetrics();
        x += (bounds.width - fm.stringWidth(id)) / 2;
        y += (bounds.height - fm.getHeight()) / 2;
        g.drawString(id, x, y + size);
      }
      g.dispose();
    }
  }
}
