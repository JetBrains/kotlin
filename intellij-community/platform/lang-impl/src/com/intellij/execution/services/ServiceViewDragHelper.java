// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.ide.dnd.DnDAction;
import com.intellij.ide.dnd.DnDDragStartBean;
import com.intellij.ide.dnd.DnDSource;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

class ServiceViewDragHelper {
  static DnDSource createSource(ServiceView serviceView) {
    return new ServiceViewDnDSource(serviceView);
  }

  static String getDisplayName(ItemPresentation presentation) {
    StringBuilder result = new StringBuilder();
    if (presentation instanceof PresentationData) {
      List<PresentableNodeDescriptor.ColoredFragment> fragments = ((PresentationData)presentation).getColoredText();
      if (fragments.isEmpty() && presentation.getPresentableText() != null) {
        result.append(presentation.getPresentableText());
      }
      else {
        for (PresentableNodeDescriptor.ColoredFragment fragment : fragments) {
          result.append(fragment.getText());
        }
      }
    }
    else if (presentation.getPresentableText() != null) {
      result.append(presentation.getPresentableText());
    }
    return result.toString();
  }

  @Nullable
  private static ServiceViewContributor getTheOnlyContributor(List<ServiceViewItem> nodes) {
    ServiceViewContributor result = null;
    for (ServiceViewItem node : nodes) {
      if (result == null) {
        result = node.getContributor();
      }
      else if (result != node.getContributor()) {
        return null;
      }
    }
    return result;
  }

  static class ServiceViewDragBean {
    private final List<ServiceViewItem> myItems;
    private final ServiceViewContributor myContributor;

    ServiceViewDragBean(List<ServiceViewItem> items) {
      myItems = ContainerUtil.filter(items, item -> {
        ServiceViewItem parent = item.getParent();
        while (parent != null) {
          if (items.contains(parent)) {
            return false;
          }
          parent = parent.getParent();
        }
        return true;
      });
      myContributor = getTheOnlyContributor(myItems);
    }

    List<ServiceViewItem> getItems() {
      return myItems;
    }

    @Nullable
    ServiceViewContributor getContributor() {
      return myContributor;
    }
  }

  private static class ServiceViewDnDSource implements DnDSource {
    private final ServiceView myServiceView;

    ServiceViewDnDSource(@NotNull ServiceView serviceView) {
      myServiceView = serviceView;
    }

    @Override
    public boolean canStartDragging(DnDAction action, Point dragOrigin) {
      return !myServiceView.getSelectedItems().isEmpty();
    }

    @Override
    public DnDDragStartBean startDragging(DnDAction action, Point dragOrigin) {
      return new DnDDragStartBean(new ServiceViewDragBean(myServiceView.getSelectedItems()));
    }

    @Override
    public void dragDropEnd() {
    }

    @Override
    public void dropActionChanged(int gestureModifiers) {
    }

    @Override
    public Pair<Image, Point> createDraggedImage(DnDAction action,
                                                 Point dragOrigin,
                                                 @NotNull DnDDragStartBean bean) {
      ServiceViewDragBean dragBean = (ServiceViewDragBean)bean.getAttachedObject();
      int size = dragBean.getItems().size();
      ItemPresentation presentation = null;
      if (size == 1) {
        presentation = dragBean.getItems().get(0).getViewDescriptor().getPresentation();
      }
      else {
        ServiceViewContributor contributor = dragBean.getContributor();
        if (contributor != null) {
          presentation = contributor.getViewDescriptor().getPresentation();
        }
      }

      SimpleColoredComponent c = new SimpleColoredComponent();
      c.setForeground(myServiceView.getForeground());
      c.setBackground(myServiceView.getBackground());
      if (presentation != null) {
        c.setIcon(presentation.getIcon(false));
        c.append(getDisplayName(presentation));
      }
      else {
        String text = size + StringUtil.pluralize(" item", size);
        c.append(text);
      }

      Dimension preferredSize = c.getPreferredSize();
      c.setSize(preferredSize);
      BufferedImage image = UIUtil.createImage(c, preferredSize.width, preferredSize.height, BufferedImage.TYPE_INT_ARGB);
      c.setOpaque(false);
      Graphics2D g = image.createGraphics();
      c.paint(g);
      g.dispose();
      return Pair.create(image, new Point(0, 0));
    }
  }
}
