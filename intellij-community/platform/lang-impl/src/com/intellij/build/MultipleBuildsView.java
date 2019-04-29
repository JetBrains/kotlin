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

import com.intellij.build.events.BuildEvent;
import com.intellij.build.events.FinishBuildEvent;
import com.intellij.build.events.StartBuildEvent;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ThreeComponentsSplitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.impl.ContentImpl;
import com.intellij.util.Alarm;
import com.intellij.util.SmartList;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EdtInvocationManager;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
public class MultipleBuildsView implements BuildProgressListener, Disposable {

  protected final Project myProject;
  protected final BuildContentManager myBuildContentManager;
  private final AtomicBoolean isInitializeStarted;
  private final List<Runnable> myPostponedRunnables;
  private final ProgressWatcher myProgressWatcher;
  private final ThreeComponentsSplitter myThreeComponentsSplitter;
  private final JBList<AbstractViewManager.BuildInfo> myBuildsList;
  private final Map<Object, AbstractViewManager.BuildInfo> myBuildsMap;
  private final Map<AbstractViewManager.BuildInfo, BuildView> myViewMap;
  private final AbstractViewManager myViewManager;
  private volatile Content myContent;
  private volatile DefaultActionGroup myToolbarActions;
  private volatile boolean myDisposed;

  public MultipleBuildsView(Project project,
                            BuildContentManager buildContentManager,
                            AbstractViewManager viewManager) {
    myProject = project;
    myBuildContentManager = buildContentManager;
    myViewManager = viewManager;
    isInitializeStarted = new AtomicBoolean();
    myPostponedRunnables = ContainerUtil.createConcurrentList();
    myThreeComponentsSplitter = new ThreeComponentsSplitter();
    Disposer.register(this, myThreeComponentsSplitter);
    myBuildsList = new JBList<>();
    myBuildsList.setModel(new DefaultListModel<>());
    myBuildsList.setFixedCellHeight(UIUtil.LIST_FIXED_CELL_HEIGHT * 2);
    myBuildsList.installCellRenderer(obj -> {
      AbstractViewManager.BuildInfo buildInfo = (AbstractViewManager.BuildInfo)obj;
      JPanel panel = new JPanel(new BorderLayout());
      SimpleColoredComponent mainComponent = new SimpleColoredComponent();
      mainComponent.setIcon(buildInfo.getIcon());
      mainComponent.append(buildInfo.getTitle() + ": ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      mainComponent.append(buildInfo.message, SimpleTextAttributes.REGULAR_ATTRIBUTES);
      panel.add(mainComponent, BorderLayout.NORTH);
      if (buildInfo.statusMessage != null) {
        SimpleColoredComponent statusComponent = new SimpleColoredComponent();
        statusComponent.setIcon(EmptyIcon.ICON_16);
        statusComponent.append(buildInfo.statusMessage, SimpleTextAttributes.GRAY_ATTRIBUTES);
        panel.add(statusComponent, BorderLayout.SOUTH);
      }
      return panel;
    });
    myViewMap = ContainerUtil.newConcurrentMap();
    myBuildsMap = ContainerUtil.newConcurrentMap();
    myProgressWatcher = new ProgressWatcher();
  }

  @Override
  public void dispose() {
    myDisposed = true;
  }

  public Content getContent() {
    return myContent;
  }

  public Map<AbstractViewManager.BuildInfo, BuildView> getBuildsMap() {
    return Collections.unmodifiableMap(myViewMap);
  }

  public boolean shouldConsume(BuildEvent event) {
    return (event.getParentId() != null && myBuildsMap.containsKey(event.getParentId())) || myBuildsMap.containsKey(event.getId());
  }

  @Override
  public void onEvent(@NotNull BuildEvent event) {
    List<Runnable> runOnEdt = new SmartList<>();
    if (event instanceof StartBuildEvent) {
      StartBuildEvent startBuildEvent = (StartBuildEvent)event;
      if (isInitializeStarted.get()) {
        clearOldBuilds(runOnEdt, startBuildEvent);
      }
      AbstractViewManager.BuildInfo buildInfo = new AbstractViewManager.BuildInfo(
        event.getId(), startBuildEvent.getBuildTitle(), startBuildEvent.getWorkingDir(), event.getEventTime());
      myBuildsMap.put(event.getId(), buildInfo);
    }
    else {
      if (event.getParentId() != null) {
        AbstractViewManager.BuildInfo buildInfo = myBuildsMap.get(event.getParentId());
        assert buildInfo != null;
        myBuildsMap.put(event.getId(), buildInfo);
      }
    }

    runOnEdt.add(() -> {
      final AbstractViewManager.BuildInfo buildInfo = myBuildsMap.get(event.getId());
      assert buildInfo != null;
      if (event instanceof StartBuildEvent) {
        StartBuildEvent startBuildEvent = (StartBuildEvent)event;
        buildInfo.message = startBuildEvent.getMessage();

        DefaultListModel<AbstractViewManager.BuildInfo> listModel =
          (DefaultListModel<AbstractViewManager.BuildInfo>)myBuildsList.getModel();
        listModel.addElement(buildInfo);

        BuildView view = myViewMap.computeIfAbsent(buildInfo, info -> {
          final BuildDescriptor buildDescriptor = new DefaultBuildDescriptor(
            startBuildEvent.getId(), startBuildEvent.getBuildTitle(), startBuildEvent.getWorkingDir(), startBuildEvent.getEventTime());
          String selectionStateKey = "build.toolwindow." + myViewManager.getViewName() + ".selection.state";
          final BuildView buildView = new BuildView(myProject, buildDescriptor, selectionStateKey, myViewManager);
          Disposer.register(myThreeComponentsSplitter, buildView);
          return buildView;
        });
        view.onEvent(startBuildEvent);

        myContent.setPreferredFocusedComponent(view::getPreferredFocusableComponent);

        RunContentDescriptor contentDescriptor;
        Supplier<RunContentDescriptor> contentDescriptorSupplier = startBuildEvent.getContentDescriptorSupplier();
        contentDescriptor = contentDescriptorSupplier != null ? contentDescriptorSupplier.get() : null;
        if (contentDescriptor != null) {
          boolean activateToolWindow = contentDescriptor.isActivateToolWindowWhenAdded();
          buildInfo.activateToolWindowWhenAdded = activateToolWindow;
          if (contentDescriptor instanceof BuildContentDescriptor) {
            buildInfo.activateToolWindowWhenFailed = ((BuildContentDescriptor)contentDescriptor).isActivateToolWindowWhenFailed();
          }
          boolean focusContent = contentDescriptor.isAutoFocusContent();
          myBuildContentManager.setSelectedContent(
            myContent, focusContent, focusContent, activateToolWindow, contentDescriptor.getActivationCallback());
          Disposer.register(view, contentDescriptor);
        }
        else {
          myBuildContentManager.setSelectedContent(myContent, true, true, false, null);
        }
        buildInfo.content = myContent;

        if (myThreeComponentsSplitter.getLastComponent() == null) {
          myThreeComponentsSplitter.setLastComponent(view);
          myViewManager.configureToolbar(myToolbarActions, this, view);
        }
        if (myBuildsList.getModel().getSize() > 1) {
          JBScrollPane scrollPane = new JBScrollPane();
          scrollPane.setBorder(JBUI.Borders.empty());
          scrollPane.setViewportView(myBuildsList);
          myThreeComponentsSplitter.setFirstComponent(scrollPane);
          myBuildsList.setVisible(true);
          myBuildsList.setSelectedIndex(0);
          myThreeComponentsSplitter.repaint();

          for (BuildView consoleView : myViewMap.values()) {
            BuildTreeConsoleView buildConsoleView = consoleView.getView(BuildTreeConsoleView.class.getName(), BuildTreeConsoleView.class);
            if (buildConsoleView != null) {
              buildConsoleView.hideRootNode();
            }
          }
        }
        else {
          myThreeComponentsSplitter.setFirstComponent(null);
        }
        myViewManager.onBuildStart(buildInfo);
        myProgressWatcher.addBuild(buildInfo);
        ((BuildContentManagerImpl)myBuildContentManager).startBuildNotified(buildInfo, buildInfo.content, startBuildEvent.getProcessHandler());
      }
      else {
        if (event instanceof FinishBuildEvent) {
          buildInfo.endTime = event.getEventTime();
          buildInfo.message = event.getMessage();
          buildInfo.result = ((FinishBuildEvent)event).getResult();
          myProgressWatcher.stopBuild(buildInfo);
          ((BuildContentManagerImpl)myBuildContentManager).finishBuildNotified(buildInfo, buildInfo.content);
          myViewManager.onBuildFinish(buildInfo);
        }
        else {
          buildInfo.statusMessage = event.getMessage();
        }

        myViewMap.get(buildInfo).onEvent(event);
      }
    });

    if (myContent == null) {
      myPostponedRunnables.addAll(runOnEdt);
      if (isInitializeStarted.compareAndSet(false, true)) {
        EdtExecutorService.getInstance().execute(() -> {
          if (myDisposed) return;
          myBuildsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
          myBuildsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
              AbstractViewManager.BuildInfo selectedBuild = myBuildsList.getSelectedValue();
              if (selectedBuild == null) return;

              BuildView view = myViewMap.get(selectedBuild);
              JComponent lastComponent = myThreeComponentsSplitter.getLastComponent();
              if (view != null && lastComponent != view.getComponent()) {
                myThreeComponentsSplitter.setLastComponent(view.getComponent());
                view.getComponent().setVisible(true);
                if (lastComponent != null) {
                  lastComponent.setVisible(false);
                }
                myViewManager.configureToolbar(myToolbarActions, MultipleBuildsView.this, view);
                view.getComponent().repaint();
              }

              int firstSize = myThreeComponentsSplitter.getFirstSize();
              int lastSize = myThreeComponentsSplitter.getLastSize();
              if (firstSize == 0 && lastSize == 0) {
                EdtInvocationManager.getInstance().invokeLater(() -> {
                  Container container = myThreeComponentsSplitter;
                  int containerWidth = 0;
                  while (container != null && (containerWidth = container.getWidth()) == 0) {
                    container = container.getParent();
                  }
                  int width = Math.round(containerWidth / 4f);
                  myThreeComponentsSplitter.setFirstSize(width);
                });
              }
            }
          });

          final JComponent consoleComponent = new JPanel(new BorderLayout());
          consoleComponent.add(myThreeComponentsSplitter, BorderLayout.CENTER);
          myToolbarActions = new DefaultActionGroup();
          consoleComponent.add(ActionManager.getInstance().createActionToolbar(
            "BuildView", myToolbarActions, false).getComponent(), BorderLayout.WEST);

          myContent = new ContentImpl(consoleComponent, myViewManager.getViewName(), true);
          Disposer.register(myContent, this);
          Disposer.register(myContent, new Disposable() {
            @Override
            public void dispose() {
              myViewManager.onBuildsViewRemove(MultipleBuildsView.this);
            }
          });
          Icon contentIcon = myViewManager.getContentIcon();
          if (contentIcon != null) {
            myContent.setIcon(contentIcon);
            myContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
          }
          myBuildContentManager.addContent(myContent);

          List<Runnable> postponedRunnables = new ArrayList<>(myPostponedRunnables);
          myPostponedRunnables.clear();
          for (Runnable postponedRunnable : postponedRunnables) {
            postponedRunnable.run();
          }
        });
      }
    }
    else {
      EdtExecutorService.getInstance().execute(() -> {
        if (myDisposed) return;
        for (Runnable runnable : runOnEdt) {
          runnable.run();
        }
      });
    }
  }

  private void clearOldBuilds(List<Runnable> runOnEdt, StartBuildEvent startBuildEvent) {
    long currentTime = System.currentTimeMillis();
    DefaultListModel<AbstractViewManager.BuildInfo> listModel = (DefaultListModel<AbstractViewManager.BuildInfo>)myBuildsList.getModel();
    boolean clearAll = !listModel.isEmpty();
    List<AbstractViewManager.BuildInfo> sameBuildsToClear = new SmartList<>();
    for (int i = 0; i < listModel.getSize(); i++) {
      AbstractViewManager.BuildInfo build = listModel.getElementAt(i);
      boolean sameBuild = build.getWorkingDir().equals(startBuildEvent.getWorkingDir());
      if (!build.isRunning() && sameBuild) {
        sameBuildsToClear.add(build);
      }
      boolean buildFinishedRecently = currentTime - build.endTime < TimeUnit.SECONDS.toMillis(1);
      if (build.isRunning() || !sameBuild && buildFinishedRecently) {
        clearAll = false;
      }
    }
    if (clearAll) {
      myBuildsMap.clear();
      SmartList<BuildView> viewsToDispose = new SmartList<>(myViewMap.values());
      runOnEdt.add(() -> viewsToDispose.forEach(Disposer::dispose));

      myViewMap.clear();
      listModel.clear();
      myBuildsList.setVisible(false);
      runOnEdt.add(() -> {
        myThreeComponentsSplitter.setFirstComponent(null);
        myThreeComponentsSplitter.setLastComponent(null);
      });
      myToolbarActions.removeAll();
    }
    else {
      sameBuildsToClear.forEach(info -> {
        BuildView buildView = myViewMap.remove(info);
        if (buildView != null) {
          runOnEdt.add(() -> Disposer.dispose(buildView));
        }
        listModel.removeElement(info);
      });
    }
  }

  public boolean hasRunningBuilds() {
    return !myProgressWatcher.myBuilds.isEmpty();
  }

  private class ProgressWatcher implements Runnable {

    private final Alarm myRefreshAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    private final Set<AbstractViewManager.BuildInfo> myBuilds = ContainerUtil.newConcurrentSet();

    @Override
    public void run() {
      myRefreshAlarm.cancelAllRequests();
      JComponent firstComponent = myThreeComponentsSplitter.getFirstComponent();
      if (firstComponent != null) {
        firstComponent.revalidate();
        firstComponent.repaint();
      }
      if (!myBuilds.isEmpty()) {
        myRefreshAlarm.addRequest(this, 300);
      }
    }

    void addBuild(AbstractViewManager.BuildInfo buildInfo) {
      myBuilds.add(buildInfo);
      if (myBuilds.size() > 1) {
        myRefreshAlarm.cancelAllRequests();
        myRefreshAlarm.addRequest(this, 300);
      }
    }

    void stopBuild(AbstractViewManager.BuildInfo buildInfo) {
      myBuilds.remove(buildInfo);
    }
  }
}
