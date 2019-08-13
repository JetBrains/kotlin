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
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.OccurenceNavigator;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.OnePixelDivider;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.OnePixelSplitter;
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
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
public class MultipleBuildsView implements BuildProgressListener, Disposable {
  private static final Logger LOG = Logger.getInstance(MultipleBuildsView.class);
  @NonNls private static final String SPLITTER_PROPERTY = "MultipleBuildsView.Splitter.Proportion";

  protected final Project myProject;
  protected final BuildContentManager myBuildContentManager;
  private final AtomicBoolean isInitializeStarted;
  private final AtomicBoolean isFirstErrorShown = new AtomicBoolean();
  private final List<Runnable> myPostponedRunnables;
  private final ProgressWatcher myProgressWatcher;
  private final OnePixelSplitter myThreeComponentsSplitter;
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
    myThreeComponentsSplitter = new OnePixelSplitter(SPLITTER_PROPERTY, 0.25f);
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

  public boolean shouldConsume(@NotNull Object buildId, @NotNull BuildEvent event) {
    return myBuildsMap.containsKey(buildId);
  }

  @Override
  public void onEvent(@NotNull Object buildId, @NotNull BuildEvent event) {
    List<Runnable> runOnEdt = new SmartList<>();
    AbstractViewManager.BuildInfo buildInfo;
    if (event instanceof StartBuildEvent) {
      StartBuildEvent startBuildEvent = (StartBuildEvent)event;
      if (isInitializeStarted.get()) {
        clearOldBuilds(runOnEdt, startBuildEvent);
      }
      buildInfo = new AbstractViewManager.BuildInfo(
        event.getId(), startBuildEvent.getBuildTitle(), startBuildEvent.getWorkingDir(), event.getEventTime());
      myBuildsMap.put(buildId, buildInfo);
    }
    else {
      buildInfo = myBuildsMap.get(buildId);
    }
    if (buildInfo == null) {
      LOG.warn("Build can not be found for buildId: '" + buildId + "'");
      return;
    }

    runOnEdt.add(() -> {
      if (event instanceof StartBuildEvent) {
        StartBuildEvent startBuildEvent = (StartBuildEvent)event;
        buildInfo.message = startBuildEvent.getMessage();

        DefaultListModel<AbstractViewManager.BuildInfo> listModel =
          (DefaultListModel<AbstractViewManager.BuildInfo>)myBuildsList.getModel();
        listModel.addElement(buildInfo);

        RunContentDescriptor contentDescriptor;
        Supplier<RunContentDescriptor> contentDescriptorSupplier = startBuildEvent.getContentDescriptorSupplier();
        contentDescriptor = contentDescriptorSupplier != null ? contentDescriptorSupplier.get() : null;
        final Runnable activationCallback;
        if (contentDescriptor != null) {
          buildInfo.setActivateToolWindowWhenAdded(contentDescriptor.isActivateToolWindowWhenAdded());
          if (contentDescriptor instanceof BuildContentDescriptor) {
            buildInfo.setActivateToolWindowWhenFailed(((BuildContentDescriptor)contentDescriptor).isActivateToolWindowWhenFailed());
          }
          buildInfo.setAutoFocusContent(contentDescriptor.isAutoFocusContent());
          activationCallback = contentDescriptor.getActivationCallback();
        }
        else {
          activationCallback = null;
        }

        BuildView view = myViewMap.computeIfAbsent(buildInfo, info -> {
          final DefaultBuildDescriptor buildDescriptor = new DefaultBuildDescriptor(
            buildInfo.getId(), buildInfo.getTitle(), buildInfo.getWorkingDir(), buildInfo.getStartTime());
          buildDescriptor.setActivateToolWindowWhenAdded(buildInfo.isActivateToolWindowWhenAdded());
          buildDescriptor.setActivateToolWindowWhenFailed(buildInfo.isActivateToolWindowWhenFailed());
          buildDescriptor.setAutoFocusContent(buildInfo.isAutoFocusContent());

          String selectionStateKey = "build.toolwindow." + myViewManager.getViewName() + ".selection.state";
          final BuildView buildView = new BuildView(myProject, buildDescriptor, selectionStateKey, myViewManager);
          Disposer.register(this, buildView);
          if (contentDescriptor != null) {
            Disposer.register(buildView, contentDescriptor);
          }
          return buildView;
        });
        view.onEvent(buildId, startBuildEvent);

        myContent.setPreferredFocusedComponent(view::getPreferredFocusableComponent);

        myBuildContentManager.setSelectedContent(myContent,
                                                 buildInfo.isAutoFocusContent(),
                                                 buildInfo.isAutoFocusContent(),
                                                 buildInfo.isActivateToolWindowWhenAdded(),
                                                 activationCallback);
        buildInfo.content = myContent;

        if (myThreeComponentsSplitter.getSecondComponent() == null) {
          myThreeComponentsSplitter.setSecondComponent(view);
          myViewManager.configureToolbar(myToolbarActions, this, view);
        }
        if (myBuildsList.getModel().getSize() > 1) {
          JBScrollPane scrollPane = new JBScrollPane();
          scrollPane.setBorder(JBUI.Borders.empty());
          scrollPane.setViewportView(myBuildsList);
          myThreeComponentsSplitter.setFirstComponent(scrollPane);
          myBuildsList.setVisible(true);
          myBuildsList.setSelectedIndex(0);

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
        if (!isFirstErrorShown.get() &&
            (event instanceof FinishEvent && ((FinishEvent)event).getResult() instanceof FailureResult) ||
            (event instanceof MessageEvent && ((MessageEvent)event).getResult().getKind() == MessageEvent.Kind.ERROR)) {
          if (isFirstErrorShown.compareAndSet(false, true)) {
            ListModel<AbstractViewManager.BuildInfo> listModel = myBuildsList.getModel();
            IntStream.range(0, listModel.getSize())
              .filter(i -> buildInfo == listModel.getElementAt(i))
              .findFirst()
              .ifPresent(myBuildsList::setSelectedIndex);
          }
        }
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

        myViewMap.get(buildInfo).onEvent(buildId, event);
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
              JComponent lastComponent = myThreeComponentsSplitter.getSecondComponent();
              if (view != null && lastComponent != view.getComponent()) {
                myThreeComponentsSplitter.setSecondComponent(view.getComponent());
                view.getComponent().setVisible(true);
                if (lastComponent != null) {
                  lastComponent.setVisible(false);
                }
                myViewManager.configureToolbar(myToolbarActions, MultipleBuildsView.this, view);
                view.getComponent().repaint();
              }
            }
          });

          final JComponent consoleComponent = new MultipleBuildsPanel();
          consoleComponent.add(myThreeComponentsSplitter, BorderLayout.CENTER);
          myToolbarActions = new DefaultActionGroup();
          ActionToolbar tb = ActionManager.getInstance().createActionToolbar("BuildView", myToolbarActions, false);
          tb.setTargetComponent(consoleComponent);
          tb.getComponent().setBorder(JBUI.Borders.merge(tb.getComponent().getBorder(), JBUI.Borders.customLine(OnePixelDivider.BACKGROUND, 0, 0, 0, 1), true));
          consoleComponent.add(tb.getComponent(), BorderLayout.WEST);

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
      runOnEdt.add(() -> {
        myBuildsList.setVisible(false);
        myThreeComponentsSplitter.setFirstComponent(null);
        myThreeComponentsSplitter.setSecondComponent(null);
      });
      myToolbarActions.removeAll();
      isFirstErrorShown.set(false);
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

  private class MultipleBuildsPanel extends JPanel implements OccurenceNavigator {
    MultipleBuildsPanel() {super(new BorderLayout());}

    @Override
    public boolean hasNextOccurence() {
      return getOccurenceNavigator(true) != null;
    }

    @Nullable
    private Pair<Integer, Supplier<OccurenceInfo>> getOccurenceNavigator(boolean next) {
      if (myBuildsList.getItemsCount() == 0) return null;
      int index = Math.max(myBuildsList.getSelectedIndex(), 0);

      Function<Integer, Pair<Integer, Supplier<OccurenceInfo>>> function = i -> {
        AbstractViewManager.BuildInfo buildInfo = myBuildsList.getModel().getElementAt(i);
        BuildView buildView = myViewMap.get(buildInfo);
        if (buildView == null) return null;
        if (i != index) {
          BuildTreeConsoleView eventView = buildView.getEventView();
          if (eventView == null) return null;
          eventView.getTree().clearSelection();
        }
        if (next) {
          if (buildView.hasNextOccurence()) return Pair.create(i, buildView::goNextOccurence);
        }
        else {
          if (buildView.hasPreviousOccurence()) {
            return Pair.create(i, buildView::goPreviousOccurence);
          }
          else if (i != index && buildView.hasNextOccurence()) {
            return Pair.create(i, buildView::goNextOccurence);
          }
        }
        return null;
      };
      if (next) {
        for (int i = index; i < myBuildsList.getItemsCount(); i++) {
          Pair<Integer, Supplier<OccurenceInfo>> buildViewPair = function.apply(i);
          if (buildViewPair != null) return buildViewPair;
        }
      }
      else {
        for (int i = index; i >= 0; i--) {
          Pair<Integer, Supplier<OccurenceInfo>> buildViewPair = function.apply(i);
          if (buildViewPair != null) return buildViewPair;
        }
      }
      return null;
    }

    @Override
    public boolean hasPreviousOccurence() {
      return getOccurenceNavigator(false) != null;
    }

    @Override
    public OccurenceInfo goNextOccurence() {
      Pair<Integer, Supplier<OccurenceInfo>> navigator = getOccurenceNavigator(true);
      if (navigator != null) {
        myBuildsList.setSelectedIndex(navigator.first);
        return navigator.second.get();
      }
      return null;
    }

    @Override
    public OccurenceInfo goPreviousOccurence() {
      Pair<Integer, Supplier<OccurenceInfo>> navigator = getOccurenceNavigator(false);
      if (navigator != null) {
        myBuildsList.setSelectedIndex(navigator.first);
        return navigator.second.get();
      }
      return null;
    }

    @NotNull
    @Override
    public String getNextOccurenceActionName() {
      return IdeBundle.message("action.next.problem");
    }

    @NotNull
    @Override
    public String getPreviousOccurenceActionName() {
      return IdeBundle.message("action.previous.problem");
    }
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
