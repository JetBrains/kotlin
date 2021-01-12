// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.execution.services.ServiceViewDnDDescriptor.Position;
import com.intellij.ide.dnd.*;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.impl.InternalDecorator;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.awt.RelativeRectangle;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import static com.intellij.execution.services.ServiceViewDnDDescriptor.Position.*;

final class ServiceViewDragHelper {
  static DnDSource createSource(@NotNull ServiceView serviceView) {
    return new ServiceViewDnDSource(serviceView);
  }

  static DnDTarget createTarget(@NotNull JTree tree) {
    return new ServiceViewDnDTarget(tree);
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
              presentation = dragBean.getContributor().getViewDescriptor(project).getPresentation();
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
  static ServiceViewContributor getTheOnlyRootContributor(List<ServiceViewItem> items) {
    ServiceViewContributor result = null;
    for (ServiceViewItem node : items) {
      if (result == null) {
        result = node.getRootContributor();
      }
      else if (result != node.getRootContributor()) {
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

    ServiceViewDragBean(@NotNull ServiceView serviceView, @NotNull List<ServiceViewItem> items) {
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
      myContributor = getTheOnlyRootContributor(myItems);
    }

    @NotNull
    ServiceView getServiceView() {
      return myServiceView;
    }

    @NotNull
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
          presentation = contributor.getViewDescriptor(myServiceView.getProject()).getPresentation();
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
        String text = ExecutionBundle.message("service.view.items", size);
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

  private static class ServiceViewDnDTarget implements DnDTarget {
    private final JTree myTree;

    ServiceViewDnDTarget(@NotNull JTree tree) {
      myTree = tree;
    }

    @Override
    public void drop(DnDEvent event) {
      EventContext eventContext = getEventContext(event.getPoint());
      if (eventContext == null) return;

      if (eventContext.descriptor.canDrop(event, INTO)) {
        eventContext.descriptor.drop(event, INTO);
      }
      else {
        eventContext.descriptor.drop(event, eventContext.getPosition());
      }
      event.hideHighlighter();
    }

    @Override
    public boolean update(DnDEvent event) {
      event.setDropPossible(false);
      EventContext eventContext = getEventContext(event.getPoint());
      if (eventContext == null) return true;

      if (eventContext.descriptor.canDrop(event, INTO)) {
        event.setDropPossible(true);
        RelativeRectangle rectangle = new RelativeRectangle(myTree, eventContext.cellBounds);
        event.setHighlighting(rectangle, DnDEvent.DropTargetHighlightingType.RECTANGLE);
        return false;
      }

      Position position = eventContext.getPosition();
      if (eventContext.descriptor.canDrop(event, position)) {
        event.setDropPossible(true);
        if (position != ABOVE) {
          eventContext.cellBounds.y += eventContext.cellBounds.height - 2;
        }
        RelativeRectangle rectangle = new RelativeRectangle(myTree, eventContext.cellBounds);
        rectangle.getDimension().height = 2;
        event.setHighlighting(rectangle, DnDEvent.DropTargetHighlightingType.FILLED_RECTANGLE);
        return false;
      }

      event.hideHighlighter();
      return false;
    }

    private EventContext getEventContext(Point point) {
      TreePath path = myTree.getPathForLocation(point.x, point.y);
      if (path == null || !(path.getLastPathComponent() instanceof ServiceViewItem)) return null;

      Rectangle cellBounds = myTree.getPathBounds(path);
      if (cellBounds == null) return null;

      ServiceViewItem item = (ServiceViewItem)path.getLastPathComponent();
      ServiceViewDescriptor viewDescriptor = item.getViewDescriptor();
      if (!(viewDescriptor instanceof ServiceViewDnDDescriptor)) return null;

      return new EventContext(point, cellBounds, (ServiceViewDnDDescriptor)viewDescriptor);
    }

    private static class EventContext {
      final Point point;
      final Rectangle cellBounds;
      final ServiceViewDnDDescriptor descriptor;

      private EventContext(Point point, Rectangle cellBounds, ServiceViewDnDDescriptor descriptor) {
        this.point = point;
        this.cellBounds = cellBounds;
        this.descriptor = descriptor;
      }

      Position getPosition() {
        return point.y < cellBounds.y + cellBounds.height / 2 ? ABOVE : BELOW;
      }
    }
  }
}
