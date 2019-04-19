/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.execution.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.TaskInfo;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListItemDescriptorAdapter;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.StatusBarEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.components.JBList;
import com.intellij.ui.popup.list.GroupedItemsListRenderer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StopBackgroundProcessesAction extends DumbAwareAction implements AnAction.TransparentUpdate{
  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(!getCancellableProcesses(e.getProject()).isEmpty());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    Project project = e.getProject();
    List<StopAction.HandlerItem> handlerItems = getItemsList(getCancellableProcesses(project));

    if (handlerItems.isEmpty()) {
      return;
    }

    final JBList<StopAction.HandlerItem> list = new JBList<>(handlerItems);
    list.setCellRenderer(new GroupedItemsListRenderer<>(new ListItemDescriptorAdapter<StopAction.HandlerItem>() {
      @Nullable
      @Override
      public String getTextFor(StopAction.HandlerItem item) {
        return item.displayName;
      }

      @Nullable
      @Override
      public Icon getIconFor(StopAction.HandlerItem item) {
        return item.icon;
      }

      @Override
      public boolean hasSeparatorAboveOf(StopAction.HandlerItem item) {
        return item.hasSeparator;
      }
    }));

    JBPopup popup = JBPopupFactory.getInstance().createListPopupBuilder(list)
      .setMovable(true)
      .setTitle(handlerItems.size() == 1 ? "Confirm background process stop" : "Stop background process")
      .setNamerForFiltering(o -> ((StopAction.HandlerItem)o).displayName)
      .setItemChoosenCallback(() -> {
        List valuesList = list.getSelectedValuesList();
        for (Object o : valuesList) {
          if (o instanceof StopAction.HandlerItem) ((StopAction.HandlerItem)o).stop();
        }
      })
      .setRequestFocus(true)
      .createPopup();

    InputEvent inputEvent = e.getInputEvent();
    Component component = inputEvent != null ? inputEvent.getComponent() : null;
    if (component != null && (ActionPlaces.MAIN_TOOLBAR.equals(e.getPlace())
                              || ActionPlaces.NAVIGATION_BAR_TOOLBAR.equals(e.getPlace()))) {
      popup.showUnderneathOf(component);
    }
    else if (project == null) {
      popup.showInBestPositionFor(dataContext);
    }
    else {
      popup.showCenteredInCurrentWindow(project);
    }

  }

  @NotNull
  private static List<Pair<TaskInfo, ProgressIndicator>>  getCancellableProcesses(@Nullable Project project) {
    IdeFrame frame = ((WindowManagerEx)WindowManager.getInstance()).findFrameFor(project);
    StatusBarEx statusBar = frame == null ? null : (StatusBarEx)frame.getStatusBar();
    if (statusBar == null) return Collections.emptyList();

    return ContainerUtil.findAll(statusBar.getBackgroundProcesses(),
                                 pair -> pair.first.isCancellable() && !pair.second.isCanceled());
  }

  @NotNull
  private static List<StopAction.HandlerItem> getItemsList(@NotNull List<Pair<TaskInfo, ProgressIndicator>> tasks) {
    List<StopAction.HandlerItem> items = new ArrayList<>(tasks.size());
    for (final Pair<TaskInfo, ProgressIndicator> eachPair : tasks) {
      items.add(new StopAction.HandlerItem(eachPair.first.getTitle(), AllIcons.Process.Step_passive, false) {
        @Override
        void stop() {
          eachPair.second.cancel();
        }
      });
    }
    return items;
  }
}
