// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action;

import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.ElementsChooser;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskExecutionInfo;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.module.Module;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public final class ExternalSystemActionUtil {

  public static void executeAction(final String actionId, final InputEvent e) {
    executeAction(actionId, "", e);
  }

  public static void executeAction(final String actionId, @NotNull final String place, final InputEvent e) {
    final ActionManager actionManager = ActionManager.getInstance();
    final AnAction action = actionManager.getAction(actionId);
    if (action != null) {
      final Presentation presentation = new Presentation();
      final AnActionEvent event =
        new AnActionEvent(e, DataManager.getInstance().getDataContext(e.getComponent()), place, presentation, actionManager, 0);
      action.update(event);
      if (presentation.isEnabled()) {
        action.actionPerformed(event);
      }
    }
  }

  @Nullable
  public static Module getModule(DataContext context) {
    final Module module = LangDataKeys.MODULE.getData(context);
    return module != null ? module : LangDataKeys.MODULE_CONTEXT.getData(context);
  }

  public static <E> void setElements(ElementsChooser<E> chooser, Collection<? extends E> all, Collection<E> selected, Comparator<? super E> comparator) {
    List<E> selection = chooser.getSelectedElements();
    chooser.clear();
    Collection<E> sorted = new TreeSet<>(comparator);
    sorted.addAll(all);
    for (E element : sorted) {
      chooser.addElement(element, selected.contains(element));
    }
    chooser.selectElements(selection);
  }

  public static void installCheckboxRenderer(final SimpleTree tree, final CheckboxHandler handler) {
    final JCheckBox checkbox = new JCheckBox();

    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(checkbox, BorderLayout.WEST);

    final TreeCellRenderer baseRenderer = tree.getCellRenderer();
    tree.setCellRenderer(new TreeCellRenderer() {
      @Override
      public Component getTreeCellRendererComponent(final JTree tree,
                                                    final Object value,
                                                    final boolean selected,
                                                    final boolean expanded,
                                                    final boolean leaf,
                                                    final int row,
                                                    final boolean hasFocus) {
        final Component baseComponent = baseRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        final Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
        if (!handler.isVisible(userObject)) {
          return baseComponent;
        }

        Color foreground = UIUtil.getTreeForeground(selected, hasFocus);
        Color background = UIUtil.getTreeBackground(selected, hasFocus);

        panel.add(baseComponent, BorderLayout.CENTER);
        panel.setBackground(background);
        panel.setForeground(foreground);

        CheckBoxState state = handler.getState(userObject);
        checkbox.setSelected(state != CheckBoxState.UNCHECKED);
        checkbox.setEnabled(state != CheckBoxState.PARTIAL);
        checkbox.setBackground(background);
        checkbox.setForeground(foreground);

        return panel;
      }
    });

    tree.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        int row = tree.getRowForLocation(e.getX(), e.getY());
        if (row >= 0) {
          TreePath path = tree.getPathForRow(row);
          if (!isCheckboxEnabledFor(path, handler)) return;

          Rectangle checkBounds = checkbox.getBounds();
          checkBounds.setLocation(tree.getRowBounds(row).getLocation());
          if (checkBounds.contains(e.getPoint())) {
            handler.toggle(path, e);
            e.consume();
            tree.setSelectionRow(row);
          }
        }
      }
    });

    tree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          TreePath[] treePaths = tree.getSelectionPaths();
          if (treePaths != null) {
            for (TreePath treePath : treePaths) {
              if (!isCheckboxEnabledFor(treePath, handler)) continue;
              handler.toggle(treePath, e);
            }
            e.consume();
          }
        }
      }
    });
  }

  private static boolean isCheckboxEnabledFor(TreePath path, CheckboxHandler handler) {
    Object userObject = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
    return handler.isVisible(userObject);
  }

  public interface CheckboxHandler {
    void toggle(TreePath treePath, final InputEvent e);

    boolean isVisible(Object userObject);

    CheckBoxState getState(Object userObject);
  }

  public enum CheckBoxState {
    CHECKED, UNCHECKED, PARTIAL
  }

  @NotNull
  public static ExternalTaskExecutionInfo buildTaskInfo(@NotNull TaskData task) {
    ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
    settings.setExternalProjectPath(task.getLinkedExternalProjectPath());
    settings.setTaskNames(Collections.singletonList(task.getName()));
    settings.setTaskDescriptions(Collections.singletonList(task.getDescription()));
    settings.setExternalSystemIdString(task.getOwner().toString());
    return new ExternalTaskExecutionInfo(settings, DefaultRunExecutor.EXECUTOR_ID);
  }
}

