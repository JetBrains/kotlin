// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
  // todo [Vlad] remove the map when BuildProgressListener.onEvent(BuildEvent) method will be removed
  private final Map<Object, Object> idsMap = ContainerUtil.newConcurrentMap();

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

    if (buildId == UNKNOWN_BUILD_ID) {
      Object buildIdCandidate = event instanceof StartBuildEvent ? event.getId() :
                                idsMap.get(ObjectUtils.notNull(event.getParentId(), event.getId()));
      if (buildIdCandidate == null) {
        return;
      }
      buildId = buildIdCandidate;
      if (event instanceof StartEvent) {
        idsMap.put(event.getId(), buildId);
      }
    }

    MultipleBuildsView buildsView;
    if (event instanceof StartBuildEvent) {
      configurePinnedContent();
      buildsView = myBuildsViewValue.getValue();
    }
    else {
      buildsView = myBuildsViewValue.getValue();
      if (!buildsView.shouldConsume(buildId, event)) {
        Object finalBuildId = buildId;
        buildsView = myPinnedViews.stream()
          .filter(pinnedView -> pinnedView.shouldConsume(finalBuildId, event))
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
    clearIdsOf(Collections.singleton(buildDescriptor));
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
    idsMap.clear();
  }

  void onBuildsViewRemove(@NotNull MultipleBuildsView buildsView) {
    if (isDisposed.get()) return;

    if (myBuildsViewValue.getValue() == buildsView) {
      myBuildsViewValue.drop();
    }
    else {
      myPinnedViews.remove(buildsView);
    }

    clearIdsOf(buildsView.getBuildsMap().keySet());
  }

  private void clearIdsOf(@NotNull Collection<? extends BuildDescriptor> builds) {
    if (idsMap.isEmpty()) return;
    Set ids = builds.stream().map(BuildDescriptor::getId).collect(Collectors.toSet());
    idsMap.values().removeIf(val -> ids.contains(val));
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
      Toggleable.setSelected(e.getPresentation(), selected);
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
      Toggleable.setSelected(e.getPresentation(), selected);

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
