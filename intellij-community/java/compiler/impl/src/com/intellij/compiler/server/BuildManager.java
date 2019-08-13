// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.server;

import com.intellij.ProjectTopics;
import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.CompilerConfigurationImpl;
import com.intellij.compiler.CompilerWorkspaceConfiguration;
import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.compiler.impl.javaCompiler.BackendCompiler;
import com.intellij.compiler.impl.javaCompiler.eclipse.EclipseCompilerConfiguration;
import com.intellij.compiler.impl.javaCompiler.javac.JavacConfiguration;
import com.intellij.compiler.server.impl.BuildProcessClasspathManager;
import com.intellij.concurrency.JobScheduler;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.ide.DataManager;
import com.intellij.ide.PowerSaveMode;
import com.intellij.ide.file.BatchFileChangeListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.*;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.compiler.CompilerTopics;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.*;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.projectRoots.ex.JavaSdkUtil;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.impl.FileNameCache;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.util.*;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import com.intellij.util.containers.IntArrayList;
import com.intellij.util.io.BaseOutputReader;
import com.intellij.util.io.PathKt;
import com.intellij.util.io.storage.HeavyProcessLatch;
import com.intellij.util.lang.JavaVersion;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.net.NetUtils;
import com.intellij.util.text.DateFormatUtil;
import gnu.trove.THashSet;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.internal.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.BuiltInServerManager;
import org.jetbrains.ide.BuiltInServerManagerImpl;
import org.jetbrains.io.ChannelRegistrar;
import org.jetbrains.jps.api.*;
import org.jetbrains.jps.cmdline.BuildMain;
import org.jetbrains.jps.cmdline.ClasspathBootstrap;
import org.jetbrains.jps.incremental.Utils;
import org.jetbrains.jps.model.java.compiler.JavaCompilers;

import javax.tools.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.openapi.util.Pair.pair;
import static org.jetbrains.jps.api.CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope;

/**
 * @author Eugene Zhuravlev
 */
public class BuildManager implements Disposable {
  public static final Key<Boolean> ALLOW_AUTOMAKE = Key.create("_allow_automake_when_process_is_active_");
  private static final Key<Integer> COMPILER_PROCESS_DEBUG_PORT = Key.create("_compiler_process_debug_port_");
  private static final Key<String> FORCE_MODEL_LOADING_PARAMETER = Key.create(BuildParametersKeys.FORCE_MODEL_LOADING);
  private static final Key<CharSequence> STDERR_OUTPUT = Key.create("_process_launch_errors_");
  private static final SimpleDateFormat USAGE_STAMP_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

  private static final Logger LOG = Logger.getInstance("#com.intellij.compiler.server.BuildManager");
  private static final String COMPILER_PROCESS_JDK_PROPERTY = "compiler.process.jdk";
  public static final String SYSTEM_ROOT = "compile-server";
  public static final String TEMP_DIR_NAME = "_temp_";
  // do not make static in order not to access application on class load
  private final boolean IS_UNIT_TEST_MODE;
  private static final String IWS_EXTENSION = ".iws";
  private static final String IPR_EXTENSION = ".ipr";
  private static final String IDEA_PROJECT_DIR_PATTERN = "/.idea/";
  private static final Function<String, Boolean> PATH_FILTER =
    SystemInfo.isFileSystemCaseSensitive ?
    s -> !(s.contains(IDEA_PROJECT_DIR_PATTERN) || s.endsWith(IWS_EXTENSION) || s.endsWith(IPR_EXTENSION)) :
    s -> !(StringUtil.endsWithIgnoreCase(s, IWS_EXTENSION) || StringUtil.endsWithIgnoreCase(s, IPR_EXTENSION) || StringUtil.containsIgnoreCase(s, IDEA_PROJECT_DIR_PATTERN));

  private final List<String> myFallbackJdkParams = new SmartList<>();
  private final ProjectManager myProjectManager;

  private final Map<TaskFuture, Project> myAutomakeFutures = Collections.synchronizedMap(new HashMap<>());
  private final Map<String, RequestFuture> myBuildsInProgress = Collections.synchronizedMap(new HashMap<>());
  private final Map<String, Future<Pair<RequestFuture<PreloadedProcessMessageHandler>, OSProcessHandler>>> myPreloadedBuilds = Collections.synchronizedMap(new HashMap<>());
  private final BuildProcessClasspathManager myClasspathManager = new BuildProcessClasspathManager();
  private final ExecutorService myRequestsProcessor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor(
    "BuildManager RequestProcessor Pool");
  private final List<VFileEvent> myUnprocessedEvents = new ArrayList<>();
  private final ExecutorService myAutomakeTrigger = SequentialTaskExecutor.createSequentialApplicationPoolExecutor(
    "BuildManager Auto-Make Trigger");
  private final Map<String, ProjectData> myProjectDataMap = Collections.synchronizedMap(new HashMap<>());
  private volatile int myFileChangeCounter;

  private final BuildManagerPeriodicTask myAutoMakeTask = new BuildManagerPeriodicTask() {
    @Override
    protected int getDelay() {
      return Registry.intValue("compiler.automake.trigger.delay");
    }

    @Override
    protected void runTask() {
      runAutoMake();
    }

    @Override
    protected boolean shouldPostpone() {
      return shouldPostponeAutomake();
    }
  };

  private final BuildManagerPeriodicTask myDocumentSaveTask = new BuildManagerPeriodicTask() {
    @Override
    protected int getDelay() {
      return Registry.intValue("compiler.document.save.trigger.delay");
    }

    @Override
    public void runTask() {
      if (shouldSaveDocuments()) {
        TransactionGuard.getInstance().submitTransactionAndWait(() ->
          ((FileDocumentManagerImpl)FileDocumentManager.getInstance()).saveAllDocuments(false));
      }
    }

    private boolean shouldSaveDocuments() {
      final Project contextProject = getCurrentContextProject();
      return contextProject != null && canStartAutoMake(contextProject);
    }
  };

  private final Runnable myGCTask = () -> {
    // todo: make customizable in UI?
    final int unusedThresholdDays = Registry.intValue("compiler.build.data.unused.threshold", -1);
    if (unusedThresholdDays <= 0) {
      return;
    }
    File buildSystemDir = getBuildSystemDirectory().toFile();
    File[] dirs = buildSystemDir.listFiles(pathname -> pathname.isDirectory() && !TEMP_DIR_NAME.equals(pathname.getName()));
    if (dirs != null) {
      final Date now = new Date();
      for (File buildDataProjectDir : dirs) {
        File usageFile = getUsageFile(buildDataProjectDir);
        if (usageFile.exists()) {
          final Pair<Date, File> usageData = readUsageFile(usageFile);
          if (usageData != null) {
            final File projectFile = usageData.second;
            if (projectFile != null && !projectFile.exists() || DateFormatUtil.getDifferenceInDays(usageData.first, now) > unusedThresholdDays) {
              LOG.info("Clearing project build data because the project does not exist or was not opened for more than " + unusedThresholdDays + " days: " + buildDataProjectDir);
              FileUtil.delete(buildDataProjectDir);
            }
          }
        }
        else {
          updateUsageFile(null, buildDataProjectDir); // set usage stamp to start countdown
        }
      }
    }
  };

  private final ChannelRegistrar myChannelRegistrar = new ChannelRegistrar();

  private final BuildMessageDispatcher myMessageDispatcher = new BuildMessageDispatcher();
  private volatile int myListenPort = -1;
  @NotNull
  private final Charset mySystemCharset = CharsetToolkit.getDefaultSystemCharset();
  private volatile boolean myBuildProcessDebuggingEnabled;

  public BuildManager(final ProjectManager projectManager) {
    final Application application = ApplicationManager.getApplication();
    IS_UNIT_TEST_MODE = application.isUnitTestMode();
    myProjectManager = projectManager;

    final String fallbackSdkHome = getFallbackSdkHome();
    if (fallbackSdkHome != null) {
      myFallbackJdkParams.add("-D" + GlobalOptions.FALLBACK_JDK_HOME + "=" + fallbackSdkHome);
      myFallbackJdkParams.add("-D" + GlobalOptions.FALLBACK_JDK_VERSION + "=" + SystemInfo.JAVA_VERSION);
    }

    MessageBusConnection connection = application.getMessageBus().connect();
    connection.subscribe(ProjectManager.TOPIC, new ProjectWatcher());
    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        if (!IS_UNIT_TEST_MODE) {
          synchronized (myUnprocessedEvents) {
            myUnprocessedEvents.addAll(events);
          }
          myAutomakeTrigger.submit(() -> {
            if (!application.isDisposed()) {
              ReadAction.run(()->{
                final List<VFileEvent> snapshot;
                synchronized (myUnprocessedEvents) {
                  if (myUnprocessedEvents.isEmpty()) {
                    return;
                  }
                  snapshot = new ArrayList<>(myUnprocessedEvents);
                  myUnprocessedEvents.clear();
                }
                if (shouldTriggerMake(snapshot)) {
                  scheduleAutoMake();
                }
              });
            }
            else {
              synchronized (myUnprocessedEvents) {
                myUnprocessedEvents.clear();
              }
            }
          });
        }
      }

      private boolean shouldTriggerMake(List<? extends VFileEvent> events) {
        if (PowerSaveMode.isEnabled()) {
          return false;
        }

        Project project = null;
        ProjectFileIndex fileIndex = null;

        for (VFileEvent event : events) {
          final VirtualFile eventFile = event.getFile();
          if (eventFile == null) {
            continue;
          }
          if (!eventFile.isValid()) {
            return true; // should be deleted
          }

          if (project == null) {
            // lazy init
            project = getCurrentContextProject();
            if (project == null) {
              return false;
            }
            fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
          }

          if (fileIndex.isInContent(eventFile)) {
            if (ProjectUtil.isProjectOrWorkspaceFile(eventFile) || GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(eventFile, project)) {
              // changes in project files or generated stuff should not trigger auto-make
              continue;
            }
            return true;
          }
        }
        return false;
      }

    });

    connection.subscribe(BatchFileChangeListener.TOPIC, new BatchFileChangeListener() {
      @Override
      public void batchChangeStarted(@NotNull Project project, @Nullable String activityName) {
        myFileChangeCounter++;
        cancelAutoMakeTasks(project);
      }

      @Override
      public void batchChangeCompleted(@NotNull Project project) {
        myFileChangeCounter--;
      }
    });

    EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent e) {
        if (Registry.is("compiler.document.save.enabled", false)) {
          final Document document = e.getDocument();
          if (FileDocumentManager.getInstance().isDocumentUnsaved(document)) {
            final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
            if (file != null && file.isInLocalFileSystem()) {
              scheduleProjectSave();
            }
          }
        }
      }
    });

    ShutDownTracker.getInstance().registerShutdownTask(this::stopListening);

    if (!IS_UNIT_TEST_MODE) {
      ScheduledFuture<?> future = JobScheduler.getScheduler().scheduleWithFixedDelay(() -> runCommand(myGCTask), 3, 180, TimeUnit.MINUTES);
      Disposer.register(this, () -> future.cancel(false));
    }
  }

  @Nullable
  private static String getFallbackSdkHome() {
    String home = SystemProperties.getJavaHome(); // should point either to jre or jdk
    if (home == null) return null;

    if (!JdkUtil.checkForJdk(home)) {
      String parent = new File(home).getParent();
      if (parent != null && JdkUtil.checkForJdk(parent)) {
        home = parent;
      }
    }
    return FileUtil.toSystemIndependentName(home);
  }

  private List<Project> getOpenProjects() {
    final Project[] projects = myProjectManager.getOpenProjects();
    if (projects.length == 0) {
      return Collections.emptyList();
    }
    final List<Project> projectList = new SmartList<>();
    for (Project project : projects) {
      if (isValidProject(project)) {
        projectList.add(project);
      }
    }
    return projectList;
  }

  private static boolean isValidProject(@Nullable Project project) {
    return project != null && !project.isDisposed() && !project.isDefault() && project.isInitialized();
  }

  public static BuildManager getInstance() {
    return ApplicationManager.getApplication().getComponent(BuildManager.class);
  }

  public void notifyFilesChanged(final Collection<? extends File> paths) {
    doNotify(paths, false);
  }

  public void notifyFilesDeleted(Collection<? extends File> paths) {
    doNotify(paths, true);
  }

  public void runCommand(@NotNull Runnable command) {
    myRequestsProcessor.submit(command);
  }

  private void doNotify(final Collection<? extends File> paths, final boolean notifyDeletion) {
    // ensure events processed in the order they arrived
    runCommand(() -> {
      final List<String> filtered = new ArrayList<>(paths.size());
      for (File file : paths) {
        final String path = FileUtil.toSystemIndependentName(file.getPath());
        if (PATH_FILTER.fun(path)) {
          filtered.add(path);
        }
      }
      if (filtered.isEmpty()) {
        return;
      }
      synchronized (myProjectDataMap) {
        //if (IS_UNIT_TEST_MODE) {
        //  if (notifyDeletion) {
        //    LOG.info("Registering deleted paths: " + filtered);
        //  }
        //  else {
        //    LOG.info("Registering changed paths: " + filtered);
        //  }
        //}
        for (Map.Entry<String, ProjectData> entry : myProjectDataMap.entrySet()) {
          final ProjectData data = entry.getValue();
          if (notifyDeletion) {
            data.addDeleted(filtered);
          }
          else {
            data.addChanged(filtered);
          }
          final RequestFuture future = myBuildsInProgress.get(entry.getKey());
          if (future != null && !future.isCancelled() && !future.isDone()) {
            final UUID sessionId = future.getRequestID();
            final Channel channel = myMessageDispatcher.getConnectedChannel(sessionId);
            if (channel != null) {
              final CmdlineRemoteProto.Message.ControllerMessage message =
                CmdlineRemoteProto.Message.ControllerMessage.newBuilder().setType(
                  CmdlineRemoteProto.Message.ControllerMessage.Type.FS_EVENT).setFsEvent(data.createNextEvent()).build();
              channel.writeAndFlush(CmdlineProtoUtil.toMessage(sessionId, message));
            }
          }
        }
      }
    });
  }

  public static void forceModelLoading(CompileContext context) {
    context.getCompileScope().putUserData(FORCE_MODEL_LOADING_PARAMETER, Boolean.TRUE.toString());
  }

  public void clearState(Project project) {
    final String projectPath = getProjectPath(project);

    cancelPreloadedBuilds(projectPath);

    synchronized (myProjectDataMap) {
      final ProjectData data = myProjectDataMap.get(projectPath);
      if (data != null) {
        data.dropChanges();
      }
    }
    scheduleAutoMake();
  }

  public void clearState() {
    final boolean cleared;
    synchronized (myProjectDataMap) {
      cleared = !myProjectDataMap.isEmpty();
      for (Map.Entry<String, ProjectData> entry : myProjectDataMap.entrySet()) {
        cancelPreloadedBuilds(entry.getKey());
        entry.getValue().dropChanges();
      }
    }
    if (cleared) {
      scheduleAutoMake();
    }
  }

  public boolean isProjectWatched(Project project) {
    return myProjectDataMap.containsKey(getProjectPath(project));
  }

  @Nullable
  public List<String> getFilesChangedSinceLastCompilation(Project project) {
    String projectPath = getProjectPath(project);
    synchronized (myProjectDataMap) {
      ProjectData data = myProjectDataMap.get(projectPath);
      if (data != null && !data.myNeedRescan) {
        return convertToStringPaths(data.myChanged);
      }
      return null;
    }
  }

  private static List<String> convertToStringPaths(final Collection<? extends InternedPath> interned) {
    final ArrayList<String> list = new ArrayList<>(interned.size());
    for (InternedPath path : interned) {
      list.add(path.getValue());
    }
    return list;
  }

  @Nullable
  private static String getProjectPath(final Project project) {
    final String url = project.getPresentableUrl();
    if (url == null) {
      return null;
    }
    return VirtualFileManager.extractPath(url);
  }

  public void scheduleAutoMake() {
    if (!IS_UNIT_TEST_MODE && !PowerSaveMode.isEnabled()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Automake scheduled:\n" + getThreadTrace(Thread.currentThread(), 10));
      }
      myAutoMakeTask.schedule();
    }
  }

  @NotNull
  private static String getThreadTrace(Thread thread, final int depth) { // debugging
    final StringBuilder buf = new StringBuilder();
    final StackTraceElement[] trace = thread.getStackTrace();
    for (int i = 0; i < depth && i < trace.length; i++) {
      final StackTraceElement element = trace[i];
      buf.append("\tat ").append(element).append("\n");
    }
    return buf.toString();
  }

  private void scheduleProjectSave() {
    if (!IS_UNIT_TEST_MODE && !PowerSaveMode.isEnabled()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Automake canceled; reason: project save scheduled");
      }
      myAutoMakeTask.cancelPendingExecution();
      myDocumentSaveTask.schedule();
    }
  }

  private void runAutoMake() {
    final Project project = getCurrentContextProject();
    if (project == null || !canStartAutoMake(project)) {
      return;
    }
    final List<TargetTypeBuildScope> scopes = CmdlineProtoUtil.createAllModulesScopes(false);
    final AutoMakeMessageHandler handler = new AutoMakeMessageHandler(project);
    final TaskFuture future = scheduleBuild(
      project, false, true, false, scopes, Collections.emptyList(), Collections.singletonMap(BuildParametersKeys.IS_AUTOMAKE, "true"), handler
    );
    if (future != null) {
      myAutomakeFutures.put(future, project);
      try {
        future.waitFor();
      }
      finally {
        myAutomakeFutures.remove(future);
        if (handler.unprocessedFSChangesDetected()) {
          scheduleAutoMake();
        }
      }
    }
  }

  private static boolean canStartAutoMake(@NotNull Project project) {
    if (project.isDisposed()) {
      return false;
    }
    final CompilerWorkspaceConfiguration config = CompilerWorkspaceConfiguration.getInstance(project);
    if (!config.MAKE_PROJECT_ON_SAVE) {
      return false;
    }
    return config.allowAutoMakeWhileRunningApplication() || !hasRunningProcess(project);
  }

  private static boolean shouldPostponeAutomake() {
    // Heuristics for postpone-decision:
    // 1. There are unsaved documents OR
    // 2. The IDE is not idle: the last activity happened less than 3 seconds ago (registry-configurable)
    if (FileDocumentManager.getInstance().getUnsavedDocuments().length > 0) {
      return true;
    }
    final long threshold = Registry.intValue("compiler.automake.postpone.when.idle.less.than", 3000); // todo: UI option instead of registry?
    final long idleSinceLastActivity = ApplicationManager.getApplication().getIdleTime();
    return idleSinceLastActivity < threshold;
  }

  @Nullable
  private Project getCurrentContextProject() {
    final List<Project> openProjects = getOpenProjects();
    if (openProjects.isEmpty()) {
      return null;
    }
    if (openProjects.size() == 1) {
      return openProjects.get(0);
    }

    Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    if (window == null) {
      return null;
    }

    Component comp = window;
    while (true) {
      final Container _parent = comp.getParent();
      if (_parent == null) {
        break;
      }
      comp = _parent;
    }

    Project project = null;
    if (comp instanceof IdeFrame) {
      project = ((IdeFrame)comp).getProject();
    }
    if (project == null) {
      project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(comp));
    }

    return isValidProject(project)? project : null;
  }

  private static boolean hasRunningProcess(@NotNull Project project) {
    for (ProcessHandler handler : ExecutionManager.getInstance(project).getRunningProcesses()) {
      if (!handler.isProcessTerminated() && !ALLOW_AUTOMAKE.get(handler, Boolean.FALSE)) { // active process
        return true;
      }
    }
    return false;
  }

  @NotNull
  public Collection<TaskFuture> cancelAutoMakeTasks(@NotNull Project project) {
    final Collection<TaskFuture> futures = new SmartList<>();
    synchronized (myAutomakeFutures) {
      for (Map.Entry<TaskFuture, Project> entry : myAutomakeFutures.entrySet()) {
        if (entry.getValue().equals(project)) {
          final TaskFuture future = entry.getKey();
          future.cancel(false);
          futures.add(future);
        }
      }
    }
    if (LOG.isDebugEnabled() && !futures.isEmpty()) {
      LOG.debug("Automake cancel (all tasks):\n" + getThreadTrace(Thread.currentThread(), 10));
    }
    return futures;
  }

  private void cancelAllPreloadedBuilds() {
    String[] paths = ArrayUtilRt.toStringArray(myPreloadedBuilds.keySet());
    for (String path : paths) {
      cancelPreloadedBuilds(path);
    }
  }

  private void cancelPreloadedBuilds(final String projectPath) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Cancel preloaded build for " + projectPath + "\n" + getThreadTrace(Thread.currentThread(), 50));
    }
    runCommand(() -> {
      Pair<RequestFuture<PreloadedProcessMessageHandler>, OSProcessHandler> pair = takePreloadedProcess(projectPath);
      if (pair != null) {
        final RequestFuture<PreloadedProcessMessageHandler> future = pair.first;
        final OSProcessHandler processHandler = pair.second;
        myMessageDispatcher.cancelSession(future.getRequestID());
        // waiting for preloaded process from project's task queue guarantees no build is started for this project
        // until this one gracefully exits and closes all its storages
        getProjectData(projectPath).taskQueue.submit(() -> {
          Throwable error = null;
          try {
            while (!processHandler.waitFor()) {
              LOG.info("processHandler.waitFor() returned false for session " + future.getRequestID() + ", continue waiting");
            }
          }
          catch (Throwable e) {
            error = e;
          }
          finally {
            notifySessionTerminationIfNeeded(future.getRequestID(), error);
          }
        });
      }
    });
  }

  @Nullable
  private Pair<RequestFuture<PreloadedProcessMessageHandler>, OSProcessHandler> takePreloadedProcess(String projectPath) {
    Pair<RequestFuture<PreloadedProcessMessageHandler>, OSProcessHandler> result;
    final Future<Pair<RequestFuture<PreloadedProcessMessageHandler>, OSProcessHandler>> preloadProgress = myPreloadedBuilds.remove(projectPath);
    try {
      result = preloadProgress != null ? preloadProgress.get() : null;
    }
    catch (Throwable e) {
      LOG.info(e);
      result = null;
    }
    return result != null && !result.first.isDone()? result : null;
  }

  @Nullable
  public TaskFuture scheduleBuild(
    final Project project, final boolean isRebuild, final boolean isMake,
    final boolean onlyCheckUpToDate, final List<TargetTypeBuildScope> scopes,
    final Collection<String> paths,
    final Map<String, String> userData, final DefaultMessageHandler messageHandler) {

    final String projectPath = getProjectPath(project);
    final boolean isAutomake = messageHandler instanceof AutoMakeMessageHandler;
    final BuilderMessageHandler handler = new NotifyingMessageHandler(project, messageHandler, isAutomake);
    try {
      ensureListening();
    }
    catch (Exception e) {
      final UUID sessionId = UUID.randomUUID(); // the actual session did not start, use random UUID
      handler.handleFailure(sessionId, CmdlineProtoUtil.createFailure(e.getMessage(), null));
      handler.sessionTerminated(sessionId);
      return null;
    }

    final DelegateFuture _future = new DelegateFuture();
    // by using the same queue that processes events we ensure that
    // the build will be aware of all events that have happened before this request
    runCommand(() -> {
      final Pair<RequestFuture<PreloadedProcessMessageHandler>, OSProcessHandler> preloaded = takePreloadedProcess(projectPath);
      final RequestFuture<PreloadedProcessMessageHandler> preloadedFuture = Pair.getFirst(preloaded);
      final boolean usingPreloadedProcess = preloadedFuture != null;

      final UUID sessionId;
      if (usingPreloadedProcess) {
        LOG.info("Using preloaded build process to compile " + projectPath);
        sessionId = preloadedFuture.getRequestID();
        preloadedFuture.getMessageHandler().setDelegateHandler(handler);
      }
      else {
        sessionId = UUID.randomUUID();
      }

      final RequestFuture<? extends BuilderMessageHandler> future = usingPreloadedProcess? preloadedFuture : new RequestFuture<>(handler,
                                                                                                                                 sessionId,
                                                                                                                                 new CancelBuildSessionAction<>());
      // futures we need to wait for: either just "future" or both "future" and "buildFuture" below
      TaskFuture[] delegatesToWait = {future};

      if (!usingPreloadedProcess && (future.isCancelled() || project.isDisposed())) {
        // in case of preloaded process the process was already running, so the handler will be notified upon process termination
        handler.sessionTerminated(sessionId);
        future.setDone();
      }
      else {
        final CmdlineRemoteProto.Message.ControllerMessage.GlobalSettings globals =
          CmdlineRemoteProto.Message.ControllerMessage.GlobalSettings.newBuilder().setGlobalOptionsPath(PathManager.getOptionsPath())
            .build();
        CmdlineRemoteProto.Message.ControllerMessage.FSEvent currentFSChanges;
        final ExecutorService projectTaskQueue;
        final boolean needRescan;
        synchronized (myProjectDataMap) {
          final ProjectData data = getProjectData(projectPath);
          if (isRebuild) {
            data.dropChanges();
          }
          if (IS_UNIT_TEST_MODE) {
            LOG.info("Scheduling build for " +
                     projectPath +
                     "; CHANGED: " +
                     new HashSet<>(convertToStringPaths(data.myChanged)) +
                     "; DELETED: " +
                     new HashSet<>(convertToStringPaths(data.myDeleted)));
          }
          needRescan = data.getAndResetRescanFlag();
          currentFSChanges = needRescan ? null : data.createNextEvent();
          projectTaskQueue = data.taskQueue;
        }

        final CmdlineRemoteProto.Message.ControllerMessage params;
        if (isRebuild) {
          params = CmdlineProtoUtil.createBuildRequest(projectPath, scopes, Collections.emptyList(), userData, globals, null);
        }
        else if (onlyCheckUpToDate) {
          params = CmdlineProtoUtil.createUpToDateCheckRequest(projectPath, scopes, paths, userData, globals, currentFSChanges);
        }
        else {
          params = CmdlineProtoUtil.createBuildRequest(projectPath, scopes, isMake ? Collections.emptyList() : paths, userData, globals, currentFSChanges);
        }
        if (!usingPreloadedProcess) {
          myMessageDispatcher.registerBuildMessageHandler(future, params);
        }

        try {
          Future<?> buildFuture = projectTaskQueue.submit(() -> {
            Throwable execFailure = null;
            try {
              if (project.isDisposed()) {
                if (usingPreloadedProcess) {
                  future.cancel(false);
                }
                else {
                  return;
                }
              }
              myBuildsInProgress.put(projectPath, future);
              final OSProcessHandler processHandler;
              CharSequence errorsOnLaunch;
              if (usingPreloadedProcess) {
                final boolean paramsSent = myMessageDispatcher.sendBuildParameters(future.getRequestID(), params);
                if (!paramsSent) {
                  myMessageDispatcher.cancelSession(future.getRequestID());
                }
                processHandler = preloaded.second;
                errorsOnLaunch = STDERR_OUTPUT.get(processHandler);
              }
              else {
                if (isAutomake && needRescan) {
                  // if project state was cleared because of roots changed or this is the first compilation after project opening,
                  // ensure project model is saved on disk, so that automake sees the latest model state.
                  // For ordinary make all project, app settings and unsaved docs are always saved before build starts.
                  try {
                    TransactionGuard.getInstance().submitTransactionAndWait(project::save);
                  }
                  catch (Throwable e) {
                    LOG.info(e);
                  }
                }

                processHandler = launchBuildProcess(project, myListenPort, sessionId, false);
                errorsOnLaunch = new StringBuffer();
                processHandler.addProcessListener(new StdOutputCollector((StringBuffer)errorsOnLaunch));
                processHandler.startNotify();
              }

              Integer debugPort = processHandler.getUserData(COMPILER_PROCESS_DEBUG_PORT);
              if (debugPort != null) {
                String message = "Build: waiting for debugger connection on port " + debugPort;
                messageHandler
                  .handleCompileMessage(sessionId, CmdlineProtoUtil.createCompileProgressMessageResponse(message).getCompileMessage());
              }

              while (!processHandler.waitFor()) {
                LOG.info("processHandler.waitFor() returned false for session " + sessionId + ", continue waiting");
              }

              final int exitValue = processHandler.getProcess().exitValue();
              if (exitValue != 0) {
                final StringBuilder msg = new StringBuilder();
                msg.append("Abnormal build process termination: ");
                if (errorsOnLaunch != null && errorsOnLaunch.length() > 0) {
                  msg.append("\n").append(errorsOnLaunch);
                  if (StringUtil.contains(errorsOnLaunch, "java.lang.NoSuchMethodError")) {
                    msg.append(
                      "\nThe error may be caused by JARs in Java Extensions directory which conflicts with libraries used by the external build process.")
                      .append(
                        "\nTry adding -Djava.ext.dirs=\"\" argument to 'Build process VM options' in File | Settings | Build, Execution, Deployment | Compiler to fix the problem.");
                  }
                }
                else {
                  msg.append("unknown error");
                }
                handler.handleFailure(sessionId, CmdlineProtoUtil.createFailure(msg.toString(), null));
              }
            }
            catch (Throwable e) {
              execFailure = e;
            }
            finally {
              myBuildsInProgress.remove(projectPath);
              notifySessionTerminationIfNeeded(sessionId, execFailure);

              if (isProcessPreloadingEnabled(project)) {
                runCommand(() -> {
                  if (!myPreloadedBuilds.containsKey(projectPath)) {
                    try {
                      final Future<Pair<RequestFuture<PreloadedProcessMessageHandler>, OSProcessHandler>> preloadResult =
                        launchPreloadedBuildProcess(project, projectTaskQueue);
                      myPreloadedBuilds.put(projectPath, preloadResult);
                    }
                    catch (Throwable e) {
                      LOG.info("Error pre-loading build process for project " + projectPath, e);
                    }
                  }
                });
              }
            }
          });
          delegatesToWait = new TaskFuture[]{future, new TaskFutureAdapter<>(buildFuture)};
        }
        catch (Throwable e) {
          handleProcessExecutionFailure(sessionId, e);
        }
      }
      boolean set = _future.setDelegates(delegatesToWait);
      assert set;
    });

    return _future;
  }

  private boolean isProcessPreloadingEnabled(Project project) {
    // automatically disable process preloading when debugging or testing
    if (IS_UNIT_TEST_MODE || !Registry.is("compiler.process.preload") || myBuildProcessDebuggingEnabled) {
      return false;
    }
    if (project.isDisposed()) {
      return true;
    }
    for (BuildProcessParametersProvider provider : BuildProcessParametersProvider.EP_NAME.getExtensionList(project)) {
      if (!provider.isProcessPreloadingEnabled()) {
        return false;
      }
    }
    return true;
  }

  private void notifySessionTerminationIfNeeded(@NotNull UUID sessionId, @Nullable Throwable execFailure) {
    if (myMessageDispatcher.getAssociatedChannel(sessionId) == null) {
      // either the connection has never been established (process not started or execution failed), or no messages were sent from the launched process.
      // in this case the session cannot be unregistered by the message dispatcher
      final BuilderMessageHandler unregistered = myMessageDispatcher.unregisterBuildMessageHandler(sessionId);
      if (unregistered != null) {
        if (execFailure != null) {
          unregistered.handleFailure(sessionId, CmdlineProtoUtil.createFailure(execFailure.getMessage(), execFailure));
        }
        unregistered.sessionTerminated(sessionId);
      }
    }
  }

  private void handleProcessExecutionFailure(@NotNull UUID sessionId, Throwable e) {
    final BuilderMessageHandler unregistered = myMessageDispatcher.unregisterBuildMessageHandler(sessionId);
    if (unregistered != null) {
      unregistered.handleFailure(sessionId, CmdlineProtoUtil.createFailure(e.getMessage(), e));
      unregistered.sessionTerminated(sessionId);
    }
  }

  @NotNull
  private ProjectData getProjectData(String projectPath) {
    synchronized (myProjectDataMap) {
      return myProjectDataMap.computeIfAbsent(projectPath, k -> new ProjectData(
        SequentialTaskExecutor.createSequentialApplicationPoolExecutor("BuildManager Pool")));
    }
  }

  private void ensureListening() {
    if (myListenPort < 0) {
      synchronized (this) {
        if (myListenPort < 0) {
          myListenPort = startListening();
        }
      }
    }
  }

  @Override
  public void dispose() {
    stopListening();
  }

  @NotNull
  public static Pair<Sdk, JavaSdkVersion> getBuildProcessRuntimeSdk(@NotNull Project project) {
    return getRuntimeSdk(project, 8);
  }

  @NotNull
  public static Pair<Sdk, JavaSdkVersion> getJavacRuntimeSdk(@NotNull Project project) {
    return getRuntimeSdk(project, 6);
  }

  private static Pair<Sdk, JavaSdkVersion> getRuntimeSdk(Project project, int oldestPossibleVersion) {
    final Set<Sdk> candidates = new LinkedHashSet<>();
    final Sdk defaultSdk = ProjectRootManager.getInstance(project).getProjectSdk();
    if (defaultSdk != null && defaultSdk.getSdkType() instanceof JavaSdkType) {
      candidates.add(defaultSdk);
    }

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
      if (sdk != null && sdk.getSdkType() instanceof JavaSdkType) {
        candidates.add(sdk);
      }
    }

    // now select the latest version from the sdks that are used in the project, but not older than the internal sdk version
    final JavaSdk javaSdkType = JavaSdk.getInstance();
    return candidates.stream()
      .map(sdk -> pair(sdk, JavaVersion.tryParse(sdk.getVersionString())))
      .filter(p -> p.second != null && p.second.isAtLeast(oldestPossibleVersion))
      .max(Comparator.comparing(p -> p.second))
      .map(p -> pair(p.first, JavaSdkVersion.fromJavaVersion(p.second)))
      .orElseGet(() -> {
        Sdk internalJdk = JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
        return pair(internalJdk, javaSdkType.getVersion(internalJdk));
      });
  }

  private Future<Pair<RequestFuture<PreloadedProcessMessageHandler>, OSProcessHandler>> launchPreloadedBuildProcess(final Project project, ExecutorService projectTaskQueue) {
    ensureListening();

    // launching build process from projectTaskQueue ensures that no other build process for this project is currently running
    return projectTaskQueue.submit(() -> {
      if (project.isDisposed()) {
        return null;
      }
      final RequestFuture<PreloadedProcessMessageHandler> future =
        new RequestFuture<>(new PreloadedProcessMessageHandler(), UUID.randomUUID(),
                            new CancelBuildSessionAction<>());
      try {
        myMessageDispatcher.registerBuildMessageHandler(future, null);
        final OSProcessHandler processHandler = launchBuildProcess(project, myListenPort, future.getRequestID(), true);
        final StringBuffer errors = new StringBuffer();
        processHandler.addProcessListener(new StdOutputCollector(errors));
        STDERR_OUTPUT.set(processHandler, errors);

        processHandler.startNotify();
        return Pair.create(future, processHandler);
      }
      catch (Throwable e) {
        handleProcessExecutionFailure(future.getRequestID(), e);
        throw e instanceof Exception? (Exception)e : new RuntimeException(e);
      }
    });
  }

  private OSProcessHandler launchBuildProcess(@NotNull Project project, final int port, @NotNull UUID sessionId, boolean requestProjectPreload) throws ExecutionException {
    String compilerPath;
    final String vmExecutablePath;
    JavaSdkVersion sdkVersion = null;

    final String forcedCompiledJdkHome = Registry.stringValue(COMPILER_PROCESS_JDK_PROPERTY);

    if (StringUtil.isEmptyOrSpaces(forcedCompiledJdkHome)) {
      // choosing sdk with which the build process should be run
      final Pair<Sdk, JavaSdkVersion> pair = getBuildProcessRuntimeSdk(project);
      final Sdk projectJdk = pair.first;
      sdkVersion = pair.second;

      final Sdk internalJdk = JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
      // validate tools.jar presence
      final JavaSdkType projectJdkType = (JavaSdkType)projectJdk.getSdkType();
      if (FileUtil.pathsEqual(projectJdk.getHomePath(), internalJdk.getHomePath())) {
        // important: because internal JDK can be either JDK or JRE,
        // this is the most universal way to obtain tools.jar path in this particular case
        final JavaCompiler systemCompiler = ToolProvider.getSystemJavaCompiler();
        if (systemCompiler == null) {
          //temporary workaround for IDEA-169747
          try {
            compilerPath = ClasspathBootstrap.getResourcePath(Class.forName("com.sun.tools.javac.api.JavacTool", false, BuildManager.class.getClassLoader()));
          }
          catch (Throwable t) {
            LOG.info(t);
            compilerPath = null;
          }
          if (compilerPath == null) {
            throw new ExecutionException("No system java compiler is provided by the JRE. Make sure tools.jar is present in IntelliJ IDEA classpath.");
          }
        }
        else {
          compilerPath = ClasspathBootstrap.getResourcePath(systemCompiler.getClass());
        }
      }
      else {
        compilerPath = projectJdkType.getToolsPath(projectJdk);
        if (compilerPath == null && !JavaSdkUtil.isJdkAtLeast(projectJdk, JavaSdkVersion.JDK_1_9)) {
          throw new ExecutionException("Cannot determine path to 'tools.jar' library for " + projectJdk.getName() + " (" + projectJdk.getHomePath() + ")");
        }
      }

      vmExecutablePath = projectJdkType.getVMExecutablePath(projectJdk);
    }
    else {
      compilerPath = new File(forcedCompiledJdkHome, "lib/tools.jar").getAbsolutePath();
      vmExecutablePath = new File(forcedCompiledJdkHome, "bin/java").getAbsolutePath();
    }

    final CompilerConfiguration projectConfig = CompilerConfiguration.getInstance(project);
    final CompilerWorkspaceConfiguration config = CompilerWorkspaceConfiguration.getInstance(project);
    final GeneralCommandLine cmdLine = new GeneralCommandLine();
    cmdLine.setExePath(vmExecutablePath);

    boolean isProfilingMode = false;
    String userDefinedHeapSize = null;
    final List<String> userAdditionalOptionsList = new SmartList<>();
    final String userAdditionalVMOptions = config.COMPILER_PROCESS_ADDITIONAL_VM_OPTIONS;
    final boolean userLocalOptionsActive = !StringUtil.isEmptyOrSpaces(userAdditionalVMOptions);
    final String additionalOptions = userLocalOptionsActive ? userAdditionalVMOptions : projectConfig.getBuildProcessVMOptions();
    if (!StringUtil.isEmptyOrSpaces(additionalOptions)) {
      final StringTokenizer tokenizer = new StringTokenizer(additionalOptions, " ", false);
      while (tokenizer.hasMoreTokens()) {
        final String option = tokenizer.nextToken();
        if (StringUtil.startsWithIgnoreCase(option, "-Xmx")) {
          if (userLocalOptionsActive) {
            userDefinedHeapSize = option;
          }
        }
        else {
          if ("-Dprofiling.mode=true".equals(option)) {
            isProfilingMode = true;
          }
          userAdditionalOptionsList.add(option);
        }
      }
    }

    if (userDefinedHeapSize != null) {
      cmdLine.addParameter(userDefinedHeapSize);
    }
    else {
      final int heapSize = projectConfig.getBuildProcessHeapSize(JavacConfiguration.getOptions(project, JavacConfiguration.class).MAXIMUM_HEAP_SIZE);
      cmdLine.addParameter("-Xmx" + heapSize + "m");
    }

    if (SystemInfo.isMac && sdkVersion != null && JavaSdkVersion.JDK_1_6.equals(sdkVersion) && Registry.is("compiler.process.32bit.vm.on.mac")) {
      // unfortunately -d32 is supported on jdk 1.6 only
      cmdLine.addParameter("-d32");
    }

    cmdLine.addParameter("-Djava.awt.headless=true");
    if (sdkVersion != null && sdkVersion.ordinal() < JavaSdkVersion.JDK_1_9.ordinal()) {
      //-Djava.endorsed.dirs is not supported in JDK 9+, may result in abnormal process termination
      cmdLine.addParameter("-Djava.endorsed.dirs=\"\""); // turn off all jre customizations for predictable behaviour
    }
    if (IS_UNIT_TEST_MODE) {
      cmdLine.addParameter("-Dtest.mode=true");
    }
    cmdLine.addParameter("-Djdt.compiler.useSingleThread=true"); // always run eclipse compiler in single-threaded mode

    if (requestProjectPreload) {
      cmdLine.addParameter("-Dpreload.project.path=" + FileUtil.toCanonicalPath(getProjectPath(project)));
      cmdLine.addParameter("-Dpreload.config.path=" + FileUtil.toCanonicalPath(PathManager.getOptionsPath()));
    }

    if (ProjectUtilCore.isExternalStorageEnabled(project)) {
      cmdLine.addParameter("-D" + GlobalOptions.EXTERNAL_PROJECT_CONFIG + "=" + ProjectUtil.getExternalConfigurationDir(project));
    }

    final String shouldGenerateIndex = System.getProperty(GlobalOptions.GENERATE_CLASSPATH_INDEX_OPTION);
    if (shouldGenerateIndex != null) {
      cmdLine.addParameter("-D"+ GlobalOptions.GENERATE_CLASSPATH_INDEX_OPTION +"=" + shouldGenerateIndex);
    }
    cmdLine.addParameter("-D" + GlobalOptions.COMPILE_PARALLEL_OPTION + "=" + config.PARALLEL_COMPILATION);
    if (config.PARALLEL_COMPILATION) {
      final boolean allowParallelAutomake = Registry.is("compiler.automake.allow.parallel", true);
      if (!allowParallelAutomake) {
        cmdLine.addParameter("-D" + GlobalOptions.ALLOW_PARALLEL_AUTOMAKE_OPTION + "=false");
      }
    }
    cmdLine.addParameter("-D" + GlobalOptions.REBUILD_ON_DEPENDENCY_CHANGE_OPTION + "=" + config.REBUILD_ON_DEPENDENCY_CHANGE);

    if (Registry.is("compiler.build.report.statistics")) {
      cmdLine.addParameter("-D" + GlobalOptions.REPORT_BUILD_STATISTICS + "=true");
    }

    if (Boolean.TRUE.equals(Boolean.valueOf(System.getProperty("java.net.preferIPv4Stack", "false")))) {
      cmdLine.addParameter("-Djava.net.preferIPv4Stack=true");
    }

    // this will make netty initialization faster on some systems
    cmdLine.addParameter("-Dio.netty.initialSeedUniquifier=" + ThreadLocalRandom.getInitialSeedUniquifier());

    for (String option : userAdditionalOptionsList) {
      cmdLine.addParameter(option);
    }
    if (isProfilingMode) {
      cmdLine.addParameter("-agentlib:yjpagent=disablealloc,delay=10000,sessionname=ExternalBuild");
    }

    // debugging
    int debugPort = -1;
    if (myBuildProcessDebuggingEnabled) {
      debugPort = Registry.intValue("compiler.process.debug.port");
      if (debugPort <= 0) {
        try {
          debugPort = NetUtils.findAvailableSocketPort();
        }
        catch (IOException e) {
          throw new ExecutionException("Cannot find free port to debug build process", e);
        }
      }
      if (debugPort > 0) {
        cmdLine.addParameter("-XX:+HeapDumpOnOutOfMemoryError");
        cmdLine.addParameter("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + debugPort);
      }
    }

    // javac's VM should use the same default locale that IDEA uses in order for javac to print messages in 'correct' language
    cmdLine.setCharset(mySystemCharset);
    cmdLine.addParameter("-D" + CharsetToolkit.FILE_ENCODING_PROPERTY + "=" + mySystemCharset.name());
    String[] propertiesToPass = {"user.language", "user.country", "user.region", PathManager.PROPERTY_PATHS_SELECTOR, "idea.case.sensitive.fs"};
    for (String name : propertiesToPass) {
      final String value = System.getProperty(name);
      if (value != null) {
        cmdLine.addParameter("-D" + name + "=" + value);
      }
    }
    cmdLine.addParameter("-D" + PathManager.PROPERTY_HOME_PATH + "=" + PathManager.getHomePath());
    cmdLine.addParameter("-D" + PathManager.PROPERTY_CONFIG_PATH + "=" + PathManager.getConfigPath());
    cmdLine.addParameter("-D" + PathManager.PROPERTY_PLUGINS_PATH + "=" + PathManager.getPluginsPath());

    cmdLine.addParameter("-D" + GlobalOptions.LOG_DIR_OPTION + "=" + FileUtil.toSystemIndependentName(getBuildLogDirectory().getAbsolutePath()));
    cmdLine.addParameters(myFallbackJdkParams);

    cmdLine.addParameter("-Dio.netty.noUnsafe=true");

    final Path workDirectory = getBuildSystemDirectory();
    try {
      Files.createDirectories(workDirectory);
    }
    catch (IOException e) {
      LOG.warn(e);
    }

    final File projectSystemRoot = getProjectSystemDirectory(project);
    if (projectSystemRoot != null) {
      cmdLine.addParameter("-Djava.io.tmpdir=" + FileUtil.toSystemIndependentName(projectSystemRoot.getPath()) + "/" + TEMP_DIR_NAME);
    }

    for (BuildProcessParametersProvider provider : BuildProcessParametersProvider.EP_NAME.getExtensionList(project)) {
      final List<String> args = provider.getVMArguments();
      cmdLine.addParameters(args);
    }

    @SuppressWarnings("UnnecessaryFullyQualifiedName")
    final Class<?> launcherClass = org.jetbrains.jps.cmdline.Launcher.class;

    final List<String> launcherCp = new ArrayList<>();
    launcherCp.add(ClasspathBootstrap.getResourcePath(launcherClass));
    launcherCp.addAll(BuildProcessClasspathManager.getLauncherClasspath(project));
    if (compilerPath != null) {   // can be null in case of jdk9
      launcherCp.add(compilerPath);
    }

    boolean includeBundledEcj = shouldIncludeEclipseCompiler(projectConfig);
    File customEcjPath = null;
    if (includeBundledEcj) {
      final String path = EclipseCompilerConfiguration.getOptions(project, EclipseCompilerConfiguration.class).ECJ_TOOL_PATH;
      if (!StringUtil.isEmptyOrSpaces(path)) {
        customEcjPath = new File(path);
        if (customEcjPath.exists()) {
          includeBundledEcj = false;
        }
        else {
          throw new ExecutionException("Path to eclipse ecj compiler does not exist: " + customEcjPath.getAbsolutePath());
          //customEcjPath = null;
        }
      }
    }

    ClasspathBootstrap.appendJavaCompilerClasspath(launcherCp, includeBundledEcj);
    if (customEcjPath != null) {
      launcherCp.add(customEcjPath.getAbsolutePath());
    }

    cmdLine.addParameter("-classpath");
    cmdLine.addParameter(classpathToString(launcherCp));

    cmdLine.addParameter(launcherClass.getName());

    final List<String> cp = ClasspathBootstrap.getBuildProcessApplicationClasspath();
    cp.addAll(myClasspathManager.getBuildProcessPluginsClasspath(project));
    if (isProfilingMode) {
      cp.add(workDirectory.resolve("yjp-controller-api-redist.jar").toString());
    }
    cmdLine.addParameter(classpathToString(cp));

    cmdLine.addParameter(BuildMain.class.getName());
    cmdLine.addParameter(Boolean.valueOf(System.getProperty("java.net.preferIPv6Addresses", "false"))? "::1" : "127.0.0.1");
    cmdLine.addParameter(Integer.toString(port));
    cmdLine.addParameter(sessionId.toString());

    cmdLine.addParameter(PathKt.getSystemIndependentPath(workDirectory));

    cmdLine.setWorkDirectory(workDirectory.toFile());

    try {
      ApplicationManager.getApplication().getMessageBus().syncPublisher(BuildManagerListener.TOPIC).beforeBuildProcessStarted(project, sessionId);
    }
    catch (Throwable e) {
      LOG.error(e);
    }

    final OSProcessHandler processHandler = new OSProcessHandler(cmdLine) {
      @Override
      protected boolean shouldDestroyProcessRecursively() {
        return true;
      }

      @NotNull
      @Override
      protected BaseOutputReader.Options readerOptions() {
        return BaseOutputReader.Options.BLOCKING;
      }
    };
    processHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        // re-translate builder's output to idea.log
        final String text = event.getText();
        if (!StringUtil.isEmptyOrSpaces(text)) {
          if (ProcessOutputTypes.SYSTEM.equals(outputType)) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("BUILDER_PROCESS [" + outputType + "]: " + text.trim());
            }
          }
          else {
            LOG.info("BUILDER_PROCESS [" + outputType + "]: " + text.trim());
          }
        }
      }
    });
    if (debugPort > 0) {
      processHandler.putUserData(COMPILER_PROCESS_DEBUG_PORT, debugPort);
    }

    return processHandler;
  }

  private static boolean shouldIncludeEclipseCompiler(CompilerConfiguration config) {
    if (config instanceof CompilerConfigurationImpl) {
      final BackendCompiler javaCompiler = ((CompilerConfigurationImpl)config).getDefaultCompiler();
      final String compilerId = javaCompiler != null? javaCompiler.getId() : null;
      return JavaCompilers.ECLIPSE_ID.equals(compilerId)  || JavaCompilers.ECLIPSE_EMBEDDED_ID.equals(compilerId);
    }
    return true;
  }

  @NotNull
  public Path getBuildSystemDirectory() {
    return PathManagerEx.getAppSystemDir().resolve(SYSTEM_ROOT);
  }

  @NotNull
  public static File getBuildLogDirectory() {
    return new File(PathManager.getLogPath(), "build-log");
  }

  @Nullable
  public File getProjectSystemDirectory(Project project) {
    final String projectPath = getProjectPath(project);
    return projectPath != null ? Utils.getDataStorageRoot(getBuildSystemDirectory().toFile(), projectPath) : null;
  }

  private static File getUsageFile(@NotNull File projectSystemDir) {
    return new File(projectSystemDir, "ustamp");
  }

  private static void updateUsageFile(@Nullable Project project, @NotNull File projectSystemDir) {
    File usageFile = getUsageFile(projectSystemDir);
    StringBuilder content = new StringBuilder();
    try {
      synchronized (USAGE_STAMP_DATE_FORMAT) {
        content.append(USAGE_STAMP_DATE_FORMAT.format(System.currentTimeMillis()));
      }
      if (project != null && !project.isDisposed()) {
        final String projectFilePath = project.getProjectFilePath();
        if (!StringUtil.isEmptyOrSpaces(projectFilePath)) {
          content.append("\n").append(FileUtil.toCanonicalPath(projectFilePath));
        }
      }
      FileUtil.writeToFile(usageFile, content.toString());
    }
    catch (Throwable e) {
      LOG.info(e);
    }
  }

  @Nullable
  private static Pair<Date, File> readUsageFile(File usageFile) {
    try {
      List<String> lines = FileUtil.loadLines(usageFile, StandardCharsets.UTF_8.name());
      if (!lines.isEmpty()) {
        final String dateString = lines.get(0);
        final Date date;
        synchronized (USAGE_STAMP_DATE_FORMAT) {
          date = USAGE_STAMP_DATE_FORMAT.parse(dateString);
        }
        final File projectFile = lines.size() > 1? new File(lines.get(1)) : null;
        return Pair.create(date, projectFile);
      }
    }
    catch (Throwable e) {
      LOG.info(e);
    }
    return null;
  }

  private void stopListening() {
    myListenPort = -1;
    myChannelRegistrar.close();
  }

  private int startListening() {
    BuiltInServerManager builtInServerManager = BuiltInServerManager.getInstance();
    builtInServerManager.waitForStart();
    ServerBootstrap bootstrap = ((BuiltInServerManagerImpl)builtInServerManager).createServerBootstrap();
    bootstrap.childHandler(new ChannelInitializer() {
      @Override
      protected void initChannel(@NotNull Channel channel) {
        channel.pipeline().addLast(myChannelRegistrar,
                                   new ProtobufVarint32FrameDecoder(),
                                   new ProtobufDecoder(CmdlineRemoteProto.Message.getDefaultInstance()),
                                   new ProtobufVarint32LengthFieldPrepender(),
                                   new ProtobufEncoder(),
                                   myMessageDispatcher);
      }
    });
    Channel serverChannel = bootstrap.bind(InetAddress.getLoopbackAddress(), 0).syncUninterruptibly().channel();
    myChannelRegistrar.setServerChannel(serverChannel, false);
    return ((InetSocketAddress)serverChannel.localAddress()).getPort();
  }

  private static String classpathToString(List<String> cp) {
    StringBuilder builder = new StringBuilder();
    for (String file : cp) {
      if (builder.length() > 0) {
        builder.append(File.pathSeparator);
      }
      builder.append(FileUtil.toCanonicalPath(file));
    }
    return builder.toString();
  }

  public boolean isBuildProcessDebuggingEnabled() {
    return myBuildProcessDebuggingEnabled;
  }

  public void setBuildProcessDebuggingEnabled(boolean buildProcessDebuggingEnabled) {
    myBuildProcessDebuggingEnabled = buildProcessDebuggingEnabled;
    if (myBuildProcessDebuggingEnabled) {
      cancelAllPreloadedBuilds();
    }
  }

  private abstract class BuildManagerPeriodicTask implements Runnable {
    private final Alarm myAlarm;
    private final AtomicBoolean myInProgress = new AtomicBoolean(false);
    private final Runnable myTaskRunnable = () -> {
      try {
        runTask();
      }
      finally {
        myInProgress.set(false);
      }
    };

    BuildManagerPeriodicTask() {
      myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, BuildManager.this);
    }

    final void schedule() {
      cancelPendingExecution();
      final int delay = Math.max(100, getDelay());
      myAlarm.addRequest(this, delay);
    }

    void cancelPendingExecution() {
      myAlarm.cancelAllRequests();
    }

    protected boolean shouldPostpone() {
      return false;
    }

    protected abstract int getDelay();

    protected abstract void runTask();

    @Override
    public final void run() {
      if (!HeavyProcessLatch.INSTANCE.isRunning() && myFileChangeCounter <= 0 && !shouldPostpone() && !myInProgress.getAndSet(true)) {
        try {
          ApplicationManager.getApplication().executeOnPooledThread(myTaskRunnable);
        }
        catch (RejectedExecutionException ignored) {
          // we were shut down
          myInProgress.set(false);
        }
        catch (Throwable e) {
          myInProgress.set(false);
          throw new RuntimeException(e);
        }
      }
      else {
        schedule();
      }
    }
  }

  private static class NotifyingMessageHandler extends DelegatingMessageHandler {
    private final Project myProject;
    private final BuilderMessageHandler myDelegateHandler;
    private final boolean myIsAutomake;

    NotifyingMessageHandler(@NotNull Project project, @NotNull BuilderMessageHandler delegateHandler, final boolean isAutomake) {
      myProject = project;
      myDelegateHandler = delegateHandler;
      myIsAutomake = isAutomake;
    }

    @Override
    protected BuilderMessageHandler getDelegateHandler() {
      return myDelegateHandler;
    }

    @Override
    public void buildStarted(@NotNull UUID sessionId) {
      super.buildStarted(sessionId);
      try {
        ApplicationManager
          .getApplication().getMessageBus().syncPublisher(BuildManagerListener.TOPIC).buildStarted(myProject, sessionId, myIsAutomake);
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }

    @Override
    public void sessionTerminated(@NotNull UUID sessionId) {
      try {
        super.sessionTerminated(sessionId);
      }
      finally {
        try {
          ApplicationManager.getApplication().getMessageBus().syncPublisher(BuildManagerListener.TOPIC).buildFinished(myProject, sessionId, myIsAutomake);
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }
    }
  }

  private static final class StdOutputCollector extends ProcessAdapter {
    private final Appendable myOutput;
    private int myStoredLength;
    StdOutputCollector(@NotNull Appendable outputSink) {
      myOutput = outputSink;
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
      String text;

      synchronized (this) {
        if (myStoredLength > 16384) {
          return;
        }
        text = event.getText();
        if (StringUtil.isEmptyOrSpaces(text)) {
          return;
        }
        myStoredLength += text.length();
      }

      try {
        myOutput.append(text);
      }
      catch (IOException ignored) {
      }
    }
  }

  private class ProjectWatcher implements ProjectManagerListener {
    private final Map<Project, MessageBusConnection> myConnections = new HashMap<>();

    @Override
    public void projectOpened(@NotNull final Project project) {
      if (ApplicationManager.getApplication().isUnitTestMode()) return;
      final MessageBusConnection conn = project.getMessageBus().connect();
      myConnections.put(project, conn);
      conn.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
        @Override
        public void rootsChanged(@NotNull final ModuleRootEvent event) {
          final Object source = event.getSource();
          if (source instanceof Project) {
            clearState((Project)source);
          }
        }
      });
      conn.subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
        @Override
        public void processStarting(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
          cancelAutoMakeTasks(env.getProject()); // make sure to cancel all automakes waiting in the build queue
        }

        @Override
        public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
          // make sure to cancel all automakes added to the build queue after processStaring and before this event
          cancelAutoMakeTasks(env.getProject());
        }

        @Override
        public void processNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
          // augmenting reaction to processTerminated(): in case any automakes were canceled before process start
          scheduleAutoMake();
        }

        @Override
        public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
          scheduleAutoMake();
        }
      });
      conn.subscribe(CompilerTopics.COMPILATION_STATUS, new CompilationStatusListener() {
        private final Set<String> myRootsToRefresh = new THashSet<>(FileUtil.PATH_HASHING_STRATEGY);

        @Override
        public void automakeCompilationFinished(int errors, int warnings, @NotNull CompileContext compileContext) {
          if (!compileContext.getProgressIndicator().isCanceled()) {
            refreshSources(compileContext);
          }
        }

        @Override
        public void compilationFinished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
          refreshSources(compileContext);
        }

        private void refreshSources(CompileContext compileContext) {
          if (project.isDisposed()) {
            return;
          }
          final Set<String> candidates = new THashSet<>(FileUtil.PATH_HASHING_STRATEGY);
          synchronized (myRootsToRefresh) {
            candidates.addAll(myRootsToRefresh);
            myRootsToRefresh.clear();
          }
          if (compileContext.isAnnotationProcessorsEnabled()) {
            // annotation processors may have re-generated code
            final CompilerConfiguration config = CompilerConfiguration.getInstance(project);
            for (Module module : compileContext.getCompileScope().getAffectedModules()) {
              if (config.getAnnotationProcessingConfiguration(module).isEnabled()) {
                final String productionPath = CompilerPaths.getAnnotationProcessorsGenerationPath(module, false);
                if (productionPath != null) {
                  candidates.add(productionPath);
                }
                final String testsPath = CompilerPaths.getAnnotationProcessorsGenerationPath(module, true);
                if (testsPath != null) {
                  candidates.add(testsPath);
                }
              }
            }
          }

          if (!candidates.isEmpty()) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
              if (project.isDisposed()) {
                return;
              }

              CompilerUtil.refreshOutputRoots(candidates);

              LocalFileSystem lfs = LocalFileSystem.getInstance();
              Set<VirtualFile> toRefresh = ReadAction.compute(() -> {
                if (project.isDisposed()) {
                  return Collections.emptySet();
                }
                else {
                  ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
                  return candidates.stream()
                    .map(lfs::findFileByPath)
                    .filter(root -> root != null && fileIndex.isInSourceContent(root))
                    .collect(Collectors.toSet());
                }
              });

              if (!toRefresh.isEmpty()) {
                lfs.refreshFiles(toRefresh, true, true, null);
              }
            });
          }
        }

        @Override
        public void fileGenerated(@NotNull String outputRoot, @NotNull String relativePath) {
          synchronized (myRootsToRefresh) {
            myRootsToRefresh.add(outputRoot);
          }
        }
      });
      final String projectPath = getProjectPath(project);
      Disposer.register(project, () -> {
        cancelPreloadedBuilds(projectPath);
        myProjectDataMap.remove(projectPath);
      });
      StartupManager.getInstance(project).registerPostStartupActivity(() -> {
        runCommand(() -> {
          final File projectSystemDir = getProjectSystemDirectory(project);
          if (projectSystemDir != null) {
            updateUsageFile(project, projectSystemDir);
          }
        });
        scheduleAutoMake(); // run automake after project opened
      });
    }

    @Override
    public void projectClosingBeforeSave(@NotNull Project project) {
      cancelAutoMakeTasks(project);
    }

    @Override
    public void projectClosing(@NotNull Project project) {
      cancelPreloadedBuilds(getProjectPath(project));
      for (TaskFuture future : cancelAutoMakeTasks(project)) {
        future.waitFor(500, TimeUnit.MILLISECONDS);
      }
    }

    @Override
    public void projectClosed(@NotNull Project project) {
      myProjectDataMap.remove(getProjectPath(project));
      final MessageBusConnection conn = myConnections.remove(project);
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  private static class ProjectData {
    @NotNull
    final ExecutorService taskQueue;
    private final Set<InternedPath> myChanged = new THashSet<>();
    private final Set<InternedPath> myDeleted = new THashSet<>();
    private long myNextEventOrdinal;
    private boolean myNeedRescan = true;

    private ProjectData(@NotNull ExecutorService taskQueue) {
      this.taskQueue = taskQueue;
    }

    void addChanged(Collection<String> paths) {
      if (!myNeedRescan) {
        for (String path : paths) {
          final InternedPath _path = InternedPath.create(path);
          myDeleted.remove(_path);
          myChanged.add(_path);
        }
      }
    }

    void addDeleted(Collection<String> paths) {
      if (!myNeedRescan) {
        for (String path : paths) {
          final InternedPath _path = InternedPath.create(path);
          myChanged.remove(_path);
          myDeleted.add(_path);
        }
      }
    }

    CmdlineRemoteProto.Message.ControllerMessage.FSEvent createNextEvent() {
      final CmdlineRemoteProto.Message.ControllerMessage.FSEvent.Builder builder =
        CmdlineRemoteProto.Message.ControllerMessage.FSEvent.newBuilder();
      builder.setOrdinal(++myNextEventOrdinal);

      for (InternedPath path : myChanged) {
        builder.addChangedPaths(path.getValue());
      }
      myChanged.clear();

      for (InternedPath path : myDeleted) {
        builder.addDeletedPaths(path.getValue());
      }
      myDeleted.clear();

      return builder.build();
    }

    boolean getAndResetRescanFlag() {
      final boolean rescan = myNeedRescan;
      myNeedRescan = false;
      return rescan;
    }

    void dropChanges() {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Project build state cleared: " + getThreadTrace(Thread.currentThread(), 20));
      }
      myNeedRescan = true;
      myNextEventOrdinal = 0L;
      myChanged.clear();
      myDeleted.clear();
    }
  }

  private abstract static class InternedPath {
    protected final int[] myPath;

    /**
     * @param path assuming system-independent path with forward slashes
     */
    InternedPath(String path) {
      final IntArrayList list = new IntArrayList();
      final StringTokenizer tokenizer = new StringTokenizer(path, "/", false);
      while(tokenizer.hasMoreTokens()) {
        final String element = tokenizer.nextToken();
        list.add(FileNameCache.storeName(element));
      }
      myPath = list.toArray();
    }

    public abstract String getValue();

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      InternedPath path = (InternedPath)o;

      return Arrays.equals(myPath, path.myPath);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(myPath);
    }

    public static InternedPath create(String path) {
      return path.startsWith("/")? new XInternedPath(path) : new WinInternedPath(path);
    }
  }

  private static class WinInternedPath extends InternedPath {
    private WinInternedPath(String path) {
      super(path);
    }

    @Override
    public String getValue() {
      if (myPath.length == 1) {
        final String name = FileNameCache.getVFileName(myPath[0]).toString();
        // handle case of windows drive letter
        return name.length() == 2 && name.endsWith(":")? name + "/" : name;
      }

      final StringBuilder buf = new StringBuilder();
      for (int element : myPath) {
        if (buf.length() > 0) {
          buf.append("/");
        }
        buf.append(FileNameCache.getVFileName(element));
      }
      return buf.toString();
    }
  }

  private static class XInternedPath extends InternedPath {
    private XInternedPath(String path) {
      super(path);
    }

    @Override
    public String getValue() {
      if (myPath.length > 0) {
        final StringBuilder buf = new StringBuilder();
        for (int element : myPath) {
          buf.append("/").append(FileNameCache.getVFileName(element));
        }
        return buf.toString();
      }
      return "/";
    }
  }

  private static final class DelegateFuture implements TaskFuture {
    @Nullable
    private TaskFuture[] myDelegates;
    private Boolean myRequestedCancelState;

    @NotNull
    private synchronized TaskFuture[] getDelegates() {
      TaskFuture[] delegates = myDelegates;
      while (delegates == null) {
        try {
          wait();
        }
        catch (InterruptedException ignored) {
        }
        delegates = myDelegates;
      }
      return delegates;
    }

    private synchronized boolean setDelegates(@NotNull TaskFuture... delegates) {
      if (myDelegates == null) {
        try {
          myDelegates = delegates;
          if (myRequestedCancelState != null) {
            for (TaskFuture delegate : delegates) {
              delegate.cancel(myRequestedCancelState);
            }
          }
        }
        finally {
          notifyAll();
        }
        return true;
      }
      return false;
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
      Future[] delegates = myDelegates;
      if (delegates == null) {
        myRequestedCancelState = mayInterruptIfRunning;
        return true;
      }
      Stream.of(delegates).forEach(delegate -> delegate.cancel(mayInterruptIfRunning));
      return isDone();
    }

    @Override
    public void waitFor() {
      Stream.of(getDelegates()).forEach(TaskFuture::waitFor);
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) {
      Stream.of(getDelegates()).forEach(delegate -> delegate.waitFor(timeout, unit));
      return isDone();
    }

    @Override
    public boolean isCancelled() {
      final Future[] delegates;
      synchronized (this) {
        delegates = myDelegates;
        if (delegates == null) {
          return myRequestedCancelState != null;
        }
      }
      return Stream.of(delegates).anyMatch(Future::isCancelled);
    }

    @Override
    public boolean isDone() {
      final Future[] delegates;
      synchronized (this) {
        delegates = myDelegates;
        if (delegates == null) {
          return false;
        }
      }
      return Stream.of(delegates).allMatch(Future::isDone);
    }

    @Override
    public Object get() throws InterruptedException, java.util.concurrent.ExecutionException {
      for (Future delegate : getDelegates()) {
        delegate.get();
      }
      return null;
    }

    @Override
    public Object get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, java.util.concurrent.ExecutionException, TimeoutException {
      for (Future delegate : getDelegates()) {
        delegate.get(timeout, unit);
      }
      return null;
    }
  }

  private class CancelBuildSessionAction<T extends BuilderMessageHandler> implements RequestFuture.CancelAction<T> {
    @Override
    public void cancel(RequestFuture<T> future) {
      myMessageDispatcher.cancelSession(future.getRequestID());
      notifySessionTerminationIfNeeded(future.getRequestID(), null);
    }
  }
}
