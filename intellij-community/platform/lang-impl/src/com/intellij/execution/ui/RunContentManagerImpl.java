// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.ui;

import com.intellij.execution.*;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.ui.layout.impl.DockableGridContainerFactory;
import com.intellij.ide.DataManager;
import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.content.*;
import com.intellij.ui.docking.DockManager;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class RunContentManagerImpl implements RunContentManager, Disposable {
  public static final Key<Boolean> ALWAYS_USE_DEFAULT_STOPPING_BEHAVIOUR_KEY = Key.create("ALWAYS_USE_DEFAULT_STOPPING_BEHAVIOUR_KEY");
  @ApiStatus.Internal
  public static final Key<RunnerAndConfigurationSettings> TEMPORARY_CONFIGURATION_KEY = Key.create("TemporaryConfiguration");
  private static final Logger LOG = Logger.getInstance(RunContentManagerImpl.class);
  private static final Key<Executor> EXECUTOR_KEY = Key.create("Executor");
  private static final Key<ContentManagerListener> CLOSE_LISTENER_KEY = Key.create("CloseListener");

  private final Project myProject;
  private final Map<String, ContentManager> myToolwindowIdToContentManagerMap = new THashMap<>();
  private final Map<String, Icon> myToolwindowIdToBaseIconMap = new THashMap<>();
  private final ConcurrentLinkedDeque<String> myToolwindowIdZBuffer = new ConcurrentLinkedDeque<>();

  public RunContentManagerImpl(@NotNull Project project) {
    myProject = project;
    DockableGridContainerFactory containerFactory = new DockableGridContainerFactory();
    DockManager.getInstance(project).register(DockableGridContainerFactory.TYPE, containerFactory);
    Disposer.register(myProject, containerFactory);

    AppUIUtil.invokeOnEdt(() -> init(), myProject.getDisposed());
  }

  // must be called on EDT
  private void init() {
    ToolWindowManagerEx toolWindowManager = ToolWindowManagerEx.getInstanceEx(myProject);
    if (toolWindowManager == null) {
      return;
    }

    for (Executor executor : ExecutorRegistry.getInstance().getRegisteredExecutors()) {
      registerToolWindow(executor, toolWindowManager);
    }

    RunDashboardManager dashboardManager = RunDashboardManager.getInstance(myProject);
    dashboardManager.updateDashboard(true);
    initToolWindow(null, dashboardManager.getToolWindowId(), dashboardManager.getToolWindowIcon(),
                   dashboardManager.getDashboardContentManager());

    myProject.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
      @Override
      public void stateChanged() {
        if (myProject.isDisposed()) {
          return;
        }

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
        Set<String> currentWindows = new THashSet<>();
        ContainerUtil.addAll(currentWindows, toolWindowManager.getToolWindowIds());
        myToolwindowIdZBuffer.retainAll(currentWindows);

        final String activeToolWindowId = toolWindowManager.getActiveToolWindowId();
        if (activeToolWindowId != null) {
          if (myToolwindowIdZBuffer.remove(activeToolWindowId)) {
            myToolwindowIdZBuffer.addFirst(activeToolWindowId);
          }
        }
      }
    });
  }

  @Override
  public void dispose() {
  }

  private void registerToolWindow(@NotNull final Executor executor, @NotNull ToolWindowManagerEx toolWindowManager) {
    final String toolWindowId = executor.getToolWindowId();
    if (toolWindowManager.getToolWindow(toolWindowId) != null) {
      return;
    }

    final ToolWindow toolWindow = toolWindowManager.registerToolWindow(toolWindowId, true, ToolWindowAnchor.BOTTOM, this, true);
    final ContentManager contentManager = toolWindow.getContentManager();
    contentManager.addDataProvider(new DataProvider() {
      private int myInsideGetData = 0;

      @Override
      public Object getData(@NotNull String dataId) {
        myInsideGetData++;
        try {
          if (PlatformDataKeys.HELP_ID.is(dataId)) {
            return executor.getHelpId();
          }
          else {
            return myInsideGetData == 1 ? DataManager.getInstance().getDataContext(contentManager.getComponent()).getData(dataId) : null;
          }
        }
        finally {
          myInsideGetData--;
        }
      }
    });

    toolWindow.setIcon(executor.getToolWindowIcon());
    new ContentManagerWatcher(toolWindow, contentManager);
    initToolWindow(executor, toolWindowId, executor.getToolWindowIcon(), contentManager);
  }

  private void initToolWindow(@Nullable final Executor executor, String toolWindowId, Icon toolWindowIcon, ContentManager contentManager) {
    myToolwindowIdToBaseIconMap.put(toolWindowId, toolWindowIcon);
    contentManager.addContentManagerListener(new ContentManagerAdapter() {
      @Override
      public void selectionChanged(@NotNull final ContentManagerEvent event) {
        if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
          Content content = event.getContent();
          Executor contentExecutor = executor;
          if (contentExecutor == null) {
            // Content manager contains contents related with different executors.
            // Try to get executor from content.
            contentExecutor = getExecutorByContent(content);
            // Must contain this user data since all content is added by this class.
            LOG.assertTrue(contentExecutor != null);
          }
          getSyncPublisher().contentSelected(getRunContentDescriptorByContent(content), contentExecutor);
          content.setHelpId(contentExecutor.getHelpId());
        }
      }
    });
    myToolwindowIdToContentManagerMap.put(toolWindowId, contentManager);
    Disposer.register(contentManager, new Disposable() {
      @Override
      public void dispose() {
        myToolwindowIdToContentManagerMap.remove(toolWindowId).removeAllContents(true);
        myToolwindowIdZBuffer.remove(toolWindowId);
        myToolwindowIdToBaseIconMap.remove(toolWindowId);
      }
    });
    myToolwindowIdZBuffer.addLast(toolWindowId);
  }

  private RunContentWithExecutorListener getSyncPublisher() {
    return myProject.getMessageBus().syncPublisher(TOPIC);
  }

  @Override
  public void toFrontRunContent(@NotNull final Executor requestor, @NotNull final ProcessHandler handler) {
    final RunContentDescriptor descriptor = getDescriptorBy(handler, requestor);
    if (descriptor == null) {
      return;
    }
    toFrontRunContent(requestor, descriptor);
  }

  @Override
  public void toFrontRunContent(@NotNull final Executor requestor, @NotNull final RunContentDescriptor descriptor) {
    ApplicationManager.getApplication().invokeLater(() -> {
      ContentManager contentManager = getContentManagerForRunner(requestor, descriptor);
      Content content = getRunContentByDescriptor(contentManager, descriptor);
      if (content != null) {
        contentManager.setSelectedContent(content);
        ToolWindowManager.getInstance(myProject).getToolWindow(getToolWindowIdForRunner(requestor, descriptor)).show(null);
      }
    }, myProject.getDisposed());
  }

  @Override
  public void hideRunContent(@NotNull final Executor executor, final RunContentDescriptor descriptor) {
    ApplicationManager.getApplication().invokeLater(() -> {
      ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(getToolWindowIdForRunner(executor, descriptor));
      if (toolWindow != null) {
        toolWindow.hide(null);
      }
    }, myProject.getDisposed());
  }

  @Override
  @Nullable
  public RunContentDescriptor getSelectedContent() {
    for (String activeWindow : myToolwindowIdZBuffer) {
      final ContentManager contentManager = myToolwindowIdToContentManagerMap.get(activeWindow);
      if (contentManager == null) {
        continue;
      }

      final Content selectedContent = contentManager.getSelectedContent();
      if (selectedContent == null) {
        if (contentManager.getContentCount() == 0) {
          // continue to the next window if the content manager is empty
          continue;
        }
        else {
          // stop iteration over windows because there is some content in the window and the window is the last used one
          break;
        }
      }
      // here we have selected content
      return getRunContentDescriptorByContent(selectedContent);
    }

    return null;
  }

  @Override
  public boolean removeRunContent(@NotNull final Executor executor, @NotNull final RunContentDescriptor descriptor) {
    final ContentManager contentManager = getContentManagerForRunner(executor, descriptor);
    final Content content = getRunContentByDescriptor(contentManager, descriptor);
    return content != null && contentManager.removeContent(content, true);
  }

  @Override
  public void showRunContent(@NotNull Executor executor, @NotNull RunContentDescriptor descriptor) {
    showRunContent(executor, descriptor, descriptor.getExecutionId());
  }

  private void showRunContent(@NotNull final Executor executor, @NotNull final RunContentDescriptor descriptor, final long executionId) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    final ContentManager contentManager = getContentManagerForRunner(executor, descriptor);
    String toolWindowId = getToolWindowIdForRunner(executor, descriptor);
    final RunDashboardManager runDashboardManager = RunDashboardManager.getInstance(myProject);
    Condition<Content> reuseCondition = runDashboardManager.getToolWindowId().equals(toolWindowId) ?
                                        runDashboardManager.getReuseCondition() : null;
    RunContentDescriptor oldDescriptor =
      chooseReuseContentForDescriptor(contentManager, descriptor, executionId, descriptor.getDisplayName(), reuseCondition);
    final Content content;
    if (oldDescriptor == null) {
      content = createNewContent(descriptor, executor);
    }
    else {
      content = oldDescriptor.getAttachedContent();
      LOG.assertTrue(content != null);
      getSyncPublisher().contentRemoved(oldDescriptor, executor);
      Disposer.dispose(oldDescriptor); // is of the same category, can be reused
    }

    content.setExecutionId(executionId);
    content.setComponent(descriptor.getComponent());
    content.setPreferredFocusedComponent(descriptor.getPreferredFocusComputable());
    content.putUserData(RunContentDescriptor.DESCRIPTOR_KEY, descriptor);
    content.putUserData(EXECUTOR_KEY, executor);
    content.setDisplayName(descriptor.getDisplayName());
    descriptor.setAttachedContent(content);

    final ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(toolWindowId);
    final ProcessHandler processHandler = descriptor.getProcessHandler();
    if (processHandler != null) {
      final ProcessAdapter processAdapter = new ProcessAdapter() {
        @Override
        public void startNotified(@NotNull final ProcessEvent event) {
          UIUtil.invokeLaterIfNeeded(() -> {
            content.setIcon(ExecutionUtil.getLiveIndicator(descriptor.getIcon()));
            toolWindow.setIcon(ExecutionUtil.getLiveIndicator(myToolwindowIdToBaseIconMap.get(toolWindowId)));
          });
        }

        @Override
        public void processTerminated(@NotNull final ProcessEvent event) {
          ApplicationManager.getApplication().invokeLater(() -> {
            ContentManager manager = myToolwindowIdToContentManagerMap.get(toolWindowId);
            if (manager == null) return;

            boolean alive = isAlive(manager);
            setToolWindowIcon(alive, toolWindowId, toolWindow);

            Icon icon = descriptor.getIcon();
            content.setIcon(icon == null ? executor.getDisabledIcon() : IconLoader.getTransparentIcon(icon));
          });
        }
      };
      processHandler.addProcessListener(processAdapter);
      final Disposable disposer = content.getDisposer();
      if (disposer != null) {
        Disposer.register(disposer, new Disposable() {
          @Override
          public void dispose() {
            processHandler.removeProcessListener(processAdapter);
          }
        });
      }
    }

    if (oldDescriptor == null) {
      contentManager.addContent(content);
      CloseListener listener = new CloseListener(content, executor);
      content.putUserData(CLOSE_LISTENER_KEY, listener);
    }

    if (descriptor.isSelectContentWhenAdded()
        /* also update selection when reused content is already selected  */
        || oldDescriptor != null && contentManager.isSelected(content)) {
      content.getManager().setSelectedContent(content);
    }

    if (!descriptor.isActivateToolWindowWhenAdded()) {
      return;
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(toolWindowId);
      // let's activate tool window, but don't move focus
      //
      // window.show() isn't valid here, because it will not
      // mark the window as "last activated" windows and thus
      // some action like navigation up/down in stacktrace wont
      // work correctly
      window.activate(descriptor.getActivationCallback(), descriptor.isAutoFocusContent(), descriptor.isAutoFocusContent());
    }, myProject.getDisposed());
  }

  @Nullable
  @Override
  public RunContentDescriptor getReuseContent(@NotNull ExecutionEnvironment executionEnvironment) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return null;
    RunContentDescriptor contentToReuse = executionEnvironment.getContentToReuse();
    if (contentToReuse != null) {
      return contentToReuse;
    }

    String toolWindowId = getContentDescriptorToolWindowId(executionEnvironment);
    final ContentManager contentManager = toolWindowId == null ?
                                          getContentManagerForRunner(executionEnvironment.getExecutor(), null) :
                                          myToolwindowIdToContentManagerMap.get(toolWindowId);
    final RunDashboardManager runDashboardManager = RunDashboardManager.getInstance(myProject);
    Condition<Content> reuseCondition = runDashboardManager.getToolWindowId().equals(toolWindowId) ?
                                        runDashboardManager.getReuseCondition() : null;
    return chooseReuseContentForDescriptor(contentManager, null, executionEnvironment.getExecutionId(),
                                           executionEnvironment.toString(), reuseCondition);
  }

  @Override
  public RunContentDescriptor findContentDescriptor(final Executor requestor, final ProcessHandler handler) {
    return getDescriptorBy(handler, requestor);
  }

  @Override
  public void showRunContent(@NotNull Executor info, @NotNull RunContentDescriptor descriptor, @Nullable RunContentDescriptor contentToReuse) {
    copyContentAndBehavior(descriptor, contentToReuse);
    showRunContent(info, descriptor, descriptor.getExecutionId());
  }

  public static void copyContentAndBehavior(@NotNull RunContentDescriptor descriptor, @Nullable RunContentDescriptor contentToReuse) {
    if (contentToReuse != null) {
      Content attachedContent = contentToReuse.getAttachedContent();
      if (attachedContent != null && attachedContent.isValid()) {
        descriptor.setAttachedContent(attachedContent);
      }
      if (contentToReuse.isReuseToolWindowActivation()) {
        descriptor.setActivateToolWindowWhenAdded(contentToReuse.isActivateToolWindowWhenAdded());
      }
      descriptor.setContentToolWindowId(contentToReuse.getContentToolWindowId());
      descriptor.setSelectContentWhenAdded(contentToReuse.isSelectContentWhenAdded());
    }
  }

  @Nullable
  private static RunContentDescriptor chooseReuseContentForDescriptor(@NotNull ContentManager contentManager,
                                                                      @Nullable RunContentDescriptor descriptor,
                                                                      long executionId,
                                                                      @Nullable String preferredName,
                                                                      @Nullable Condition<? super Content> reuseCondition) {
    Content content = null;
    if (descriptor != null) {
      //Stage one: some specific descriptors (like AnalyzeStacktrace) cannot be reused at all
      if (descriptor.isContentReuseProhibited()) {
        return null;
      }
      //Stage two: try to get content from descriptor itself
      final Content attachedContent = descriptor.getAttachedContent();

      if (attachedContent != null
          && attachedContent.isValid()
          && contentManager.getIndexOfContent(attachedContent) != -1
          && (Comparing.equal(descriptor.getDisplayName(), attachedContent.getDisplayName()) || !attachedContent.isPinned())) {
        content = attachedContent;
      }
    }
    //Stage three: choose the content with name we prefer
    if (content == null) {
      content = getContentFromManager(contentManager, preferredName, executionId, reuseCondition);
    }
    if (content == null || !isTerminated(content) || (content.getExecutionId() == executionId && executionId != 0)) {
      return null;
    }
    final RunContentDescriptor oldDescriptor = getRunContentDescriptorByContent(content);
    if (oldDescriptor != null) {
      if (oldDescriptor.isContentReuseProhibited()) {
        return null;
      }
      if (descriptor == null || oldDescriptor.getReusePolicy().canBeReusedBy(descriptor)) {
        return oldDescriptor;
      }
    }

    return null;
  }

  @Nullable
  private static Content getContentFromManager(ContentManager contentManager,
                                               @Nullable String preferredName,
                                               long executionId,
                                               @Nullable Condition<? super Content> reuseCondition) {
    ArrayList<Content> contents = new ArrayList<>(Arrays.asList(contentManager.getContents()));
    Content first = contentManager.getSelectedContent();
    if (first != null && contents.remove(first)) {//selected content should be checked first
      contents.add(0, first);
    }
    if (preferredName != null) {//try to match content with specified preferred name
      for (Content c : contents) {
        if (canReuseContent(c, executionId) && preferredName.equals(c.getDisplayName())) {
          return c;
        }
      }
    }
    for (Content c : contents) {//return first "good" content
      if (canReuseContent(c, executionId) && (reuseCondition == null || reuseCondition.value(c))) {
        return c;
      }
    }
    return null;
  }

  private static boolean canReuseContent(Content c, long executionId) {
    return c != null && !c.isPinned() && isTerminated(c) && !(c.getExecutionId() == executionId && executionId != 0);
  }

  @NotNull
  private ContentManager getContentManagerForRunner(final Executor executor, final RunContentDescriptor descriptor) {
    final ContentManager contentManager = myToolwindowIdToContentManagerMap.get(getToolWindowIdForRunner(executor, descriptor));
    if (contentManager == null) {
      LOG.error("Runner " + executor.getId() + " is not registered");
    }
    //noinspection ConstantConditions
    return contentManager;
  }

  @NotNull
  private static String getToolWindowIdForRunner(final Executor executor, final RunContentDescriptor descriptor) {
    if (descriptor != null && descriptor.getContentToolWindowId() != null) {
      return descriptor.getContentToolWindowId();
    }
    return executor.getToolWindowId();
  }

  private static Content createNewContent(final RunContentDescriptor descriptor, Executor executor) {
    final String processDisplayName = descriptor.getDisplayName();
    final Content content = ContentFactory.SERVICE.getInstance().createContent(descriptor.getComponent(), processDisplayName, true);
    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    Icon icon = descriptor.getIcon();
    content.setIcon(icon == null ? executor.getToolWindowIcon() : icon);
    return content;
  }

  public static boolean isTerminated(@NotNull Content content) {
    RunContentDescriptor descriptor = getRunContentDescriptorByContent(content);
    ProcessHandler processHandler = descriptor == null ? null : descriptor.getProcessHandler();
    return processHandler == null || processHandler.isProcessTerminated();
  }

  @Nullable
  public static RunContentDescriptor getRunContentDescriptorByContent(@NotNull Content content) {
    return content.getUserData(RunContentDescriptor.DESCRIPTOR_KEY);
  }

  @Nullable
  public static Executor getExecutorByContent(@NotNull Content content) {
    return content.getUserData(EXECUTOR_KEY);
  }

  @Override
  @Nullable
  public ToolWindow getToolWindowByDescriptor(@NotNull RunContentDescriptor descriptor) {
    for (Map.Entry<String, ContentManager> entry : myToolwindowIdToContentManagerMap.entrySet()) {
      if (getRunContentByDescriptor(entry.getValue(), descriptor) != null) {
        return ToolWindowManager.getInstance(myProject).getToolWindow(entry.getKey());
      }
    }
    return null;
  }

  @Nullable
  private static Content getRunContentByDescriptor(@NotNull ContentManager contentManager, @NotNull RunContentDescriptor descriptor) {
    for (Content content : contentManager.getContents()) {
      if (descriptor.equals(getRunContentDescriptorByContent(content))) {
        return content;
      }
    }
    return null;
  }

  @Override
  @NotNull
  public List<RunContentDescriptor> getAllDescriptors() {
    if (myToolwindowIdToContentManagerMap.isEmpty()) {
      return Collections.emptyList();
    }

    List<RunContentDescriptor> descriptors = new SmartList<>();
    for (String id : myToolwindowIdToContentManagerMap.keySet()) {
      for (Content content : myToolwindowIdToContentManagerMap.get(id).getContents()) {
        RunContentDescriptor descriptor = getRunContentDescriptorByContent(content);
        if (descriptor != null) {
          descriptors.add(descriptor);
        }
      }
    }
    return descriptors;
  }

  @Override
  public void selectRunContent(@NotNull RunContentDescriptor descriptor) {
    for (Map.Entry<String, ContentManager> entry : myToolwindowIdToContentManagerMap.entrySet()) {
      Content content = getRunContentByDescriptor(entry.getValue(), descriptor);
      if (content != null) {
        entry.getValue().setSelectedContent(content);
      }
    }
  }

  @Override
  @Nullable
  public String getContentDescriptorToolWindowId(@Nullable RunConfiguration configuration) {
    if (configuration != null) {
      RunDashboardManager runDashboardManager = RunDashboardManager.getInstance(myProject);
      if (runDashboardManager.isShowInDashboard(configuration)) {
        return runDashboardManager.getToolWindowId();
      }
    }
    return null;
  }

  @Override
  @NotNull
  public String getToolWindowIdByEnvironment(@NotNull ExecutionEnvironment executionEnvironment) {
    // TODO [konstantin.aleev] Check remaining places where Executor.getToolWindowId() is used
    // Also there are some places where ToolWindowId.RUN or ToolWindowId.DEBUG are used directly.
    // For example, HotSwapProgressImpl.NOTIFICATION_GROUP. All notifications for this group is shown in Debug tool window,
    // however such notifications should be shown in Run Dashboard tool window, if run content is redirected to Run Dashboard tool window.
    String toolWindowId = getContentDescriptorToolWindowId(executionEnvironment);
    return toolWindowId != null ? toolWindowId : executionEnvironment.getExecutor().getToolWindowId();
  }

  @Nullable
  private RunContentDescriptor getDescriptorBy(ProcessHandler handler, Executor runnerInfo) {
    List<Content> contents = new ArrayList<>();
    ContainerUtil.addAll(contents, getContentManagerForRunner(runnerInfo, null).getContents());
    ContainerUtil.addAll(contents,
                         myToolwindowIdToContentManagerMap.get(RunDashboardManager.getInstance(myProject).getToolWindowId()).getContents());
    for (Content content : contents) {
      RunContentDescriptor runContentDescriptor = getRunContentDescriptorByContent(content);
      assert runContentDescriptor != null;
      if (runContentDescriptor.getProcessHandler() == handler) {
        return runContentDescriptor;
      }
    }
    return null;
  }

  public void moveContent(@NotNull Executor executor, @NotNull RunContentDescriptor descriptor) {
    Content content = descriptor.getAttachedContent();
    if (content == null) return;

    ContentManager oldContentManager = content.getManager();
    ContentManager newContentManager = getContentManagerForRunner(executor, descriptor);
    if (oldContentManager == null || oldContentManager == newContentManager) return;

    ContentManagerListener listener = content.getUserData(CLOSE_LISTENER_KEY);
    if (listener != null) {
      oldContentManager.removeContentManagerListener(listener);
    }
    oldContentManager.removeContent(content, false);
    if (isAlive(descriptor)) {
      if (!isAlive(oldContentManager)) {
        updateToolWindowIcon(oldContentManager, false);
      }
      if (!isAlive(newContentManager)) {
        updateToolWindowIcon(newContentManager, true);
      }
    }
    newContentManager.addContent(content);
    if (listener != null) {
      newContentManager.addContentManagerListener(listener);
    }
  }

  private void updateToolWindowIcon(@NotNull ContentManager contentManager, boolean alive) {
    for (Map.Entry<String, ContentManager> entry : myToolwindowIdToContentManagerMap.entrySet()) {
      if (entry.getValue().equals(contentManager)) {
        String toolWindowId = entry.getKey();
        ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(toolWindowId);
        if (toolWindow != null) {
          setToolWindowIcon(alive, toolWindowId, toolWindow);
        }
        return;
      }
    }
  }

  private void setToolWindowIcon(boolean alive, @NotNull String toolWindowId, @NotNull ToolWindow toolWindow) {
    Icon base = myToolwindowIdToBaseIconMap.get(toolWindowId);
    toolWindow.setIcon(alive ? ExecutionUtil.getLiveIndicator(base) : ObjectUtils.notNull(base, EmptyIcon.ICON_13));
  }

  private static boolean isAlive(@NotNull ContentManager contentManager) {
    for (Content content : contentManager.getContents()) {
      RunContentDescriptor descriptor = getRunContentDescriptorByContent(content);
      if (descriptor != null && isAlive(descriptor)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAlive(@NotNull RunContentDescriptor descriptor) {
    ProcessHandler handler = descriptor.getProcessHandler();
    return handler != null && !handler.isProcessTerminated();
  }

  private class CloseListener extends BaseContentCloseListener {
    private final Executor myExecutor;

    private CloseListener(@NotNull final Content content, @NotNull final Executor executor) {
      super(content, myProject);
      myExecutor = executor;
    }

    @Override
    protected void disposeContent(@NotNull Content content) {
      try {
        RunContentDescriptor descriptor = getRunContentDescriptorByContent(content);
        getSyncPublisher().contentRemoved(descriptor, myExecutor);
        if (descriptor != null) {
          Disposer.dispose(descriptor);
        }
      }
      finally {
        content.release();
      }
    }

    @Override
    protected boolean closeQuery(@NotNull Content content, boolean modal) {
      final RunContentDescriptor descriptor = getRunContentDescriptorByContent(content);
      if (descriptor == null) {
        return true;
      }

      final ProcessHandler processHandler = descriptor.getProcessHandler();
      if (processHandler == null || processHandler.isProcessTerminated() || processHandler.isProcessTerminating()) {
        return true;
      }
      final String sessionName = descriptor.getDisplayName();
      final WaitForProcessTask task = new WaitForProcessTask(processHandler, sessionName, modal, myProject) {
        final boolean killable =
          !modal && (processHandler instanceof KillableProcess) && ((KillableProcess)processHandler).canKillProcess();

        {
          if (killable) {
            String cancelText = ExecutionBundle.message("terminating.process.progress.kill");
            setCancelText(cancelText);
            setCancelTooltipText(cancelText);
          }
        }

        @Override
        public void onCancel() {
          if (killable && !processHandler.isProcessTerminated()) {
            ((KillableProcess)processHandler).killProcess();
          }
        }
      };
      return askUserAndWait(processHandler, sessionName, task);
    }
  }
}
