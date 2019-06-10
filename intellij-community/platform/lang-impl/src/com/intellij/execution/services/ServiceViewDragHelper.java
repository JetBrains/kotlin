// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.ide.dnd.*;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.impl.InternalDecorator;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

class ServiceViewDragHelper {
  static DnDSource createSource(@NotNull ServiceView serviceView) {
    return new ServiceViewDnDSource(serviceView);
  }

  static void installDnDSupport(@NotNull Project project, @NotNull InternalDecorator decorator, @NotNull ContentManager contentManager) {
    Content dropTargetContent = createDropTargetContent();
    DnDSupport.createBuilder(decorator)
      .setTargetChecker(new DnDTargetChecker() {
        @Override
        public boolean update(DnDEvent event) {
          Object o = event.getAttachedObject();
          boolean dropPossible = o instanceof ServiceViewDragBean && event.getPoint().y < decorator.getHeaderHeight();
          event.setDropPossible(dropPossible, "");
          if (dropPossible) {
            if (contentManager.getIndexOfContent(dropTargetContent) < 0) {
              contentManager.addContent(dropTargetContent);
            }

            ServiceViewDragBean dragBean = (ServiceViewDragBean)o;
            ItemPresentation presentation;
            if (dragBean.getItems().size() > 1 && dragBean.getContributor() != null) {
              presentation = dragBean.getContributor().getViewDescriptor().getPresentation();
            }
            else {
              presentation = dragBean.getItems().get(0).getViewDescriptor().getPresentation();
            }
            dropTargetContent.setDisplayName(getDisplayName(presentation));
            dropTargetContent.setIcon(presentation.getIcon(false));
          }
          else if (contentManager.getIndexOfContent(dropTargetContent) >= 0) {
            contentManager.removeContent(dropTargetContent, false);
          }
          return true;
        }
      })
      .setCleanUpOnLeaveCallback(() -> {
        if (!contentManager.isDisposed() && contentManager.getIndexOfContent(dropTargetContent) >= 0) {
          contentManager.removeContent(dropTargetContent, false);
        }
      })
      .setDropHandler(new DnDDropHandler() {
        @Override
        public void drop(DnDEvent event) {
          Object o = event.getAttachedObject();
          if (o instanceof ServiceViewDragBean) {
            ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project)).extract((ServiceViewDragBean)o);
          }
        }
      })
      .install();
    decorator.addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        if (contentManager.getIndexOfContent(dropTargetContent) >= 0) {
          contentManager.removeContent(dropTargetContent, false);
        }
      }
    });
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

  private static Content createDropTargetContent() {
    Content content = ContentFactory.SERVICE.getInstance().createContent(new JPanel(), null, false);
    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    content.setCloseable(true);
    return content;
  }

  static class ServiceViewDragBean implements DataProvider {
    private final ServiceView myServiceView;
    private final List<ServiceViewItem> myItems;
    private final ServiceViewContributor myContributor;

    ServiceViewDragBean(ServiceView serviceView, List<ServiceViewItem> items) {
      myServiceView = serviceView;
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

    ServiceView getServiceView() {
      return myServiceView;
    }

    List<ServiceViewItem> getItems() {
      return myItems;
    }

    @Nullable
    ServiceViewContributor getContributor() {
      return myContributor;
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
      if (PlatformDataKeys.SELECTED_ITEMS.is(dataId)) {
        return ContainerUtil.map2Array(myItems, ServiceViewItem::getValue);
      }
      return null;
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
      return new DnDDragStartBean(new ServiceViewDragBean(myServiceView, myServiceView.getSelectedItems()));
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
