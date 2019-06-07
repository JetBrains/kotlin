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
package com.intellij.build;

import com.intellij.build.events.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.notification.Notification;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AtomicClearableLazyValue;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.ui.SystemNotifications;
import com.intellij.ui.content.Content;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.build.ExecutionNode.getEventResultIcon;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
public abstract class AbstractViewManager implements ViewManager, BuildProgressListener, Disposable {
  private static final Key<Boolean> PINNED_EXTRACTED_CONTENT = new Key<>("PINNED_EXTRACTED_CONTENT");

  protected final Project myProject;
  protected final BuildContentManager myBuildContentManager;
  private final AtomicClearableLazyValue<MultipleBuildsView> myBuildsViewValue;
  private final Set<MultipleBuildsView> myPinnedViews;
  private final AtomicBoolean isDisposed = new AtomicBoolean(false);

  public AbstractViewManager(Project project, BuildContentManager buildContentManager) {
    myProject = project;
    myBuildContentManager = buildContentManager;
    myBuildsViewValue = new AtomicClearableLazyValue<MultipleBuildsView>() {
      @NotNull
      @Override
      protected MultipleBuildsView compute() {
        MultipleBuildsView buildsView = new MultipleBuildsView(myProject, myBuildContentManager, AbstractViewManager.this);
        Disposer.register(AbstractViewManager.this, buildsView);
        return buildsView;
      }
    };
    myPinnedViews = ContainerUtil.newConcurrentSet();
  }

  @Override
  public boolean isConsoleEnabledByDefault() {
    return false;
  }

  @Override
  public boolean isBuildContentView() {
    return true;
  }

  @NotNull
  protected abstract String getViewName();

  protected Map<BuildInfo, BuildView> getBuildsMap() {
    return myBuildsViewValue.getValue().getBuildsMap();
  }

  @Override
  public void onEvent(@NotNull Object buildId, @NotNull BuildEvent event) {
    if (isDisposed.get()) return;

    MultipleBuildsView buildsView;
    if (event instanceof StartBuildEvent) {
      configurePinnedContent();
      buildsView = myBuildsViewValue.getValue();
    }
    else {
      buildsView = myBuildsViewValue.getValue();
      if (!buildsView.shouldConsume(buildId, event)) {
        buildsView = myPinnedViews.stream()
          .filter(pinnedView -> pinnedView.shouldConsume(buildId, event))
          .findFirst().orElse(null);
      }
    }
    if (buildsView != null) {
      buildsView.onEvent(buildId, event);
    }
  }

  void configureToolbar(@NotNull DefaultActionGroup toolbarActions,
                        @NotNull MultipleBuildsView buildsView,
                        @NotNull BuildView view) {
    toolbarActions.removeAll();
    toolbarActions.addAll(view.createConsoleActions());
    toolbarActions.add(new PinBuildViewAction(buildsView));
    toolbarActions.add(BuildTreeFilters.createFilteringActionsGroup(view));
  }

  @Nullable
  protected Icon getContentIcon() {
    return null;
  }

  protected void onBuildStart(BuildDescriptor buildDescriptor) {
  }

  protected void onBuildFinish(BuildDescriptor buildDescriptor) {
    BuildInfo buildInfo = (BuildInfo)buildDescriptor;
    if (buildInfo.result instanceof FailureResult) {
      boolean activate = buildInfo.isActivateToolWindowWhenFailed();
      myBuildContentManager.setSelectedContent(buildInfo.content, false, false, activate, null);
      List<? extends Failure>
        failures = ((FailureResult)buildInfo.result).getFailures();
      if (failures.isEmpty()) return;
      Failure failure = failures.get(0);
      Notification notification = failure.getNotification();
      if (notification != null) {
        final String title = notification.getTitle();
        final String content = notification.getContent();
        SystemNotifications.getInstance().notify(ToolWindowId.BUILD, title, content);
      }
    }
  }

  @Override
  public void dispose() {
    isDisposed.set(true);
    myPinnedViews.clear();
    myBuildsViewValue.drop();
  }

  void onBuildsViewRemove(@NotNull MultipleBuildsView buildsView) {
    if (isDisposed.get()) return;

    if (myBuildsViewValue.getValue() == buildsView) {
      myBuildsViewValue.drop();
    }
    else {
      myPinnedViews.remove(buildsView);
    }
  }

  static class BuildInfo extends DefaultBuildDescriptor {
    String message;
    String statusMessage;
    long endTime = -1;
    EventResult result;
    Content content;

    BuildInfo(@NotNull Object id,
                     @NotNull String title,
                     @NotNull String workingDir,
                     long startTime) {
      super(id, title, workingDir, startTime);
    }

    public Icon getIcon() {
      return getEventResultIcon(result);
    }

    public boolean isRunning() {
      return endTime == -1;
    }
  }

  private void configurePinnedContent() {
    MultipleBuildsView buildsView = myBuildsViewValue.getValue();
    Content content = buildsView.getContent();
    if (content != null && content.isPinned()) {
      String tabName = getPinnedTabName(buildsView);
      UIUtil.invokeLaterIfNeeded(() -> {
        content.setPinnable(false);
        if (content.getIcon() == null) {
          content.setIcon(EmptyIcon.ICON_8);
        }
        content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        ((BuildContentManagerImpl)myBuildContentManager).updateTabDisplayName(content, tabName);
      });
      myPinnedViews.add(buildsView);
      myBuildsViewValue.drop();
      content.putUserData(PINNED_EXTRACTED_CONTENT, Boolean.TRUE);
    }
  }

  private String getPinnedTabName(MultipleBuildsView buildsView) {
    Map<BuildInfo, BuildView> buildsMap = buildsView.getBuildsMap();

    AbstractViewManager.BuildInfo buildInfo =
      buildsMap.keySet().stream()
               .reduce((b1, b2) -> b1.getStartTime() <= b2.getStartTime() ? b1 : b2)
               .orElse(null);
    if (buildInfo != null) {
      String title = buildInfo.getTitle();
      String viewName = getViewName().split(" ")[0];
      String tabName = viewName + ": " + StringUtil.trimStart(title, viewName);
      if (buildsMap.size() > 1) {
        tabName += String.format(" and %d more", buildsMap.size() - 1);
      }
      return tabName;
    }
    return getViewName();
  }

  private static class PinBuildViewAction extends DumbAwareAction implements Toggleable {
    private final Content myContent;

    PinBuildViewAction(MultipleBuildsView buildsView) {
      myContent = buildsView.getContent();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      boolean selected = !myContent.isPinned();
      if (selected) {
        myContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
      }
      myContent.setPinned(selected);
      e.getPresentation().putClientProperty(SELECTED_PROPERTY, selected);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      if (!myContent.isValid()) return;
      Boolean isPinnedAndExtracted = myContent.getUserData(PINNED_EXTRACTED_CONTENT);
      if (isPinnedAndExtracted == Boolean.TRUE) {
        e.getPresentation().setEnabledAndVisible(false);
        return;
      }

      boolean isActiveTab = myContent.getManager().getSelectedContent() == myContent;
      boolean selected = myContent.isPinned();

      e.getPresentation().setIcon(AllIcons.General.Pin_tab);
      e.getPresentation().putClientProperty(SELECTED_PROPERTY, selected);

      String text;
      if (!isActiveTab) {
        text = selected ? IdeBundle.message("action.unpin.active.tab") : IdeBundle.message("action.pin.active.tab");
      }
      else {
        text = selected ? IdeBundle.message("action.unpin.tab") : IdeBundle.message("action.pin.tab");
      }
      e.getPresentation().setText(text);
      e.getPresentation().setEnabledAndVisible(true);
    }
  }
}
