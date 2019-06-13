// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.codeHighlighting.HighlightingPass;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.daemon.*;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.impl.FileLevelIntentionComponent;
import com.intellij.codeInsight.intention.impl.IntentionHintComponent;
import com.intellij.diagnostic.ThreadDumper;
import com.intellij.ide.PowerSaveMode;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.text.AsyncEditorLoader;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.impl.FileTypeManagerImpl;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.RefreshQueueImpl;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.*;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.io.storage.HeavyProcessLatch;
import com.intellij.util.ui.UIUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * This class also controls the auto-reparse and auto-hints.
 */
@State(
  name = "DaemonCodeAnalyzer",
  storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
public class DaemonCodeAnalyzerImpl extends DaemonCodeAnalyzerEx implements PersistentStateComponent<Element>, Disposable {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl");

  private static final Key<List<HighlightInfo>> FILE_LEVEL_HIGHLIGHTS = Key.create("FILE_LEVEL_HIGHLIGHTS");
  private final Project myProject;
  private final DaemonCodeAnalyzerSettings mySettings;
  @NotNull private final EditorTracker myEditorTracker;
  @NotNull private final PsiDocumentManager myPsiDocumentManager;
  private DaemonProgressIndicator myUpdateProgress = new DaemonProgressIndicator(); //guarded by this

  private final UpdateRunnable myUpdateRunnable;
  // use scheduler instead of Alarm because the latter requires ModalityState.current() which is obtainable from EDT only which requires too many invokeLaters
  private final ScheduledExecutorService myAlarm = EdtExecutorService.getScheduledExecutorInstance();
  @NotNull
  private volatile Future<?> myUpdateRunnableFuture = CompletableFuture.completedFuture(null);
  private boolean myUpdateByTimerEnabled = true; // guarded by this
  private final Collection<VirtualFile> myDisabledHintsFiles = new THashSet<>();
  private final Collection<VirtualFile> myDisabledHighlightingFiles = new THashSet<>();

  private final FileStatusMap myFileStatusMap;
  private DaemonCodeAnalyzerSettings myLastSettings;

  private volatile boolean myDisposed;     // the only possible transition: false -> true
  private volatile boolean myInitialized;  // the only possible transition: false -> true

  @NonNls private static final String DISABLE_HINTS_TAG = "disable_hints";
  @NonNls private static final String FILE_TAG = "file";
  @NonNls private static final String URL_ATT = "url";
  private final PassExecutorService myPassExecutorService;

  public DaemonCodeAnalyzerImpl(@NotNull Project project,
                                @NotNull DaemonCodeAnalyzerSettings daemonCodeAnalyzerSettings,
                                @NotNull EditorTracker editorTracker,
                                @NotNull PsiDocumentManager psiDocumentManager,
                                // DependencyValidationManagerImpl adds scope listener, so, we need to force service creation
                                @SuppressWarnings("unused") @NotNull DependencyValidationManager dependencyValidationManager) {
    myProject = project;
    mySettings = daemonCodeAnalyzerSettings;
    myEditorTracker = editorTracker;
    myPsiDocumentManager = psiDocumentManager;
    myLastSettings = ((DaemonCodeAnalyzerSettingsImpl)daemonCodeAnalyzerSettings).clone();

    myFileStatusMap = new FileStatusMap(project);
    myPassExecutorService = new PassExecutorService(project);
    Disposer.register(this, myPassExecutorService);
    Disposer.register(this, myFileStatusMap);
    DaemonProgressIndicator.setDebug(LOG.isDebugEnabled());

    assert !myInitialized : "Double Initializing";
    Disposer.register(this, new StatusBarUpdater(project));

    myInitialized = true;
    myDisposed = false;
    myFileStatusMap.markAllFilesDirty("DCAI init");
    myUpdateRunnable = new UpdateRunnable(myProject);
    Disposer.register(this, () -> {
      assert myInitialized : "Disposing not initialized component";
      assert !myDisposed : "Double dispose";
      myUpdateRunnable.clearFieldsOnDispose();

      stopProcess(false, "Dispose");

      myDisposed = true;
      myLastSettings = null;
    });
  }

  @Override
  public synchronized void dispose() {
    clearReferences();
  }

  private synchronized void clearReferences() {
    myUpdateProgress = new DaemonProgressIndicator(); // leak of highlight session via user data
    myUpdateRunnableFuture.cancel(true);
  }

  @NotNull
  @TestOnly
  public static List<HighlightInfo> getHighlights(@NotNull Document document, @Nullable HighlightSeverity minSeverity, @NotNull Project project) {
    List<HighlightInfo> infos = new ArrayList<>();
    processHighlights(document, project, minSeverity, 0, document.getTextLength(), Processors.cancelableCollectProcessor(infos));
    return infos;
  }

  @Override
  @NotNull
  @TestOnly
  public List<HighlightInfo> getFileLevelHighlights(@NotNull Project project, @NotNull PsiFile file) {
    VirtualFile vFile = file.getViewProvider().getVirtualFile();
    final FileEditorManager manager = FileEditorManager.getInstance(project);
    return Arrays.stream(manager.getEditors(vFile))
      .map(fileEditor -> fileEditor.getUserData(FILE_LEVEL_HIGHLIGHTS))
      .filter(Objects::nonNull)
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public void cleanFileLevelHighlights(@NotNull Project project, final int group, PsiFile psiFile) {
    if (psiFile == null) return;
    FileViewProvider provider = psiFile.getViewProvider();
    VirtualFile vFile = provider.getVirtualFile();
    final FileEditorManager manager = FileEditorManager.getInstance(project);
    for (FileEditor fileEditor : manager.getEditors(vFile)) {
      final List<HighlightInfo> infos = fileEditor.getUserData(FILE_LEVEL_HIGHLIGHTS);
      if (infos == null) continue;
      List<HighlightInfo> infosToRemove = new ArrayList<>();
      for (HighlightInfo info : infos) {
        if (info.getGroup() == group) {
          manager.removeTopComponent(fileEditor, info.fileLevelComponent);
          infosToRemove.add(info);
        }
      }
      infos.removeAll(infosToRemove);
    }
  }

  @Override
  public void addFileLevelHighlight(@NotNull final Project project,
                                    final int group,
                                    @NotNull final HighlightInfo info,
                                    @NotNull final PsiFile psiFile) {
    VirtualFile vFile = psiFile.getViewProvider().getVirtualFile();
    final FileEditorManager manager = FileEditorManager.getInstance(project);
    for (FileEditor fileEditor : manager.getEditors(vFile)) {
      if (fileEditor instanceof TextEditor) {
        FileLevelIntentionComponent component = new FileLevelIntentionComponent(info.getDescription(), info.getSeverity(),
                                                                                info.getGutterIconRenderer(), info.quickFixActionRanges,
                                                                                project, psiFile, ((TextEditor)fileEditor).getEditor(), info.getToolTip());
        manager.addTopComponent(fileEditor, component);
        List<HighlightInfo> fileLevelInfos = fileEditor.getUserData(FILE_LEVEL_HIGHLIGHTS);
        if (fileLevelInfos == null) {
          fileLevelInfos = new ArrayList<>();
          fileEditor.putUserData(FILE_LEVEL_HIGHLIGHTS, fileLevelInfos);
        }
        info.fileLevelComponent = component;
        info.setGroup(group);
        fileLevelInfos.add(info);
      }
    }
  }

  @Override
  @NotNull
  public List<HighlightInfo> runMainPasses(@NotNull PsiFile psiFile,
                                           @NotNull Document document,
                                           @NotNull final ProgressIndicator progress) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      throw new IllegalStateException("Must not run highlighting from under EDT");
    }
    if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
      throw new IllegalStateException("Must run highlighting from under read action");
    }
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (!(indicator instanceof DaemonProgressIndicator)) {
      throw new IllegalStateException("Must run highlighting under progress with DaemonProgressIndicator");
    }
    // clear status maps to run passes from scratch so that refCountHolder won't conflict and try to restart itself on partially filled maps
    myFileStatusMap.markAllFilesDirty("prepare to run main passes");
    stopProcess(false, "disable background daemon");
    myPassExecutorService.cancelAll(true);

    final List<HighlightInfo> result;
    try {
      result = new ArrayList<>();
      final VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile != null && !virtualFile.getFileType().isBinary()) {
        List<TextEditorHighlightingPass> passes =
          TextEditorHighlightingPassRegistrarEx.getInstanceEx(myProject).instantiateMainPasses(psiFile, document,
                                                                                               HighlightInfoProcessor.getEmpty());

        Collections.sort(passes, (o1, o2) -> {
          if (o1 instanceof GeneralHighlightingPass) return -1;
          if (o2 instanceof GeneralHighlightingPass) return 1;
          return 0;
        });

        try {
          for (TextEditorHighlightingPass pass : passes) {
            pass.doCollectInformation(progress);
            result.addAll(pass.getInfos());
          }
        }
        catch (ProcessCanceledException e) {
          LOG.debug("Canceled: " + progress);
          throw e;
        }
      }
    }
    finally {
      stopProcess(true, "re-enable background daemon after main passes run");
    }

    return result;
  }

  private volatile boolean mustWaitForSmartMode = true;
  @TestOnly
  public void mustWaitForSmartMode(final boolean mustWait, @NotNull Disposable parent) {
    final boolean old = mustWaitForSmartMode;
    mustWaitForSmartMode = mustWait;
    Disposer.register(parent, () -> mustWaitForSmartMode = old);
  }

  @TestOnly
  public void runPasses(@NotNull PsiFile file,
                        @NotNull Document document,
                        @NotNull List<? extends TextEditor> textEditors,
                        @NotNull int[] toIgnore,
                        boolean canChangeDocument,
                        @Nullable final Runnable callbackWhileWaiting) throws ProcessCanceledException {
    assert myInitialized;
    assert !myDisposed;
    Application application = ApplicationManager.getApplication();
    application.assertIsDispatchThread();
    if (application.isWriteAccessAllowed()) {
      throw new AssertionError("Must not start highlighting from within write action, or deadlock is imminent");
    }
    DaemonProgressIndicator.setDebug(!ApplicationInfoImpl.isInStressTest());
    ((FileTypeManagerImpl)FileTypeManager.getInstance()).drainReDetectQueue();
    // pump first so that queued event do not interfere
    UIUtil.dispatchAllInvocationEvents();

    // refresh will fire write actions interfering with highlighting
    while (RefreshQueueImpl.isRefreshInProgress() || HeavyProcessLatch.INSTANCE.isRunning()) {
      UIUtil.dispatchAllInvocationEvents();
    }
    long dstart = System.currentTimeMillis();
    while (mustWaitForSmartMode && DumbService.getInstance(myProject).isDumb()) {
      if (System.currentTimeMillis() > dstart + 100000) {
        throw new IllegalStateException("Timeout waiting for smart mode. If you absolutely want to be dumb, please use DaemonCodeAnalyzerImpl.mustWaitForSmartMode(false).");
      }
      UIUtil.dispatchAllInvocationEvents();
    }

    UIUtil.dispatchAllInvocationEvents();

    FileStatusMap fileStatusMap = getFileStatusMap();

    Map<FileEditor, HighlightingPass[]> map = new HashMap<>();

    NonBlockingReadActionImpl.waitForAsyncTaskCompletion(); // wait for async editor loading
    for (TextEditor textEditor : textEditors) {
      TextEditorBackgroundHighlighter highlighter = (TextEditorBackgroundHighlighter)textEditor.getBackgroundHighlighter();
      if (highlighter == null) {
        Editor editor = textEditor.getEditor();
        throw new RuntimeException("Null highlighter from " + textEditor + "; loaded: " + AsyncEditorLoader.isEditorLoaded(editor));
      }
      final List<TextEditorHighlightingPass> passes = highlighter.getPasses(toIgnore);
      HighlightingPass[] array = passes.toArray(HighlightingPass.EMPTY_ARRAY);
      assert array.length != 0 : "Highlighting is disabled for the file " + file;
      map.put(textEditor, array);
    }
    for (int ignoreId : toIgnore) {
      fileStatusMap.markFileUpToDate(document, ignoreId);
    }

    myUpdateRunnableFuture.cancel(false);

    final DaemonProgressIndicator progress = createUpdateProgress(map.keySet());
    myPassExecutorService.submitPasses(map, progress);
    try {
      fileStatusMap.allowDirt(canChangeDocument);
      long start = System.currentTimeMillis();
      while (progress.isRunning() && System.currentTimeMillis() < start + 10*60*1000) {
        wrap(() -> {
          progress.checkCanceled();
          if (callbackWhileWaiting != null) {
            callbackWhileWaiting.run();
          }
          waitInOtherThread(50, canChangeDocument);
          UIUtil.dispatchAllInvocationEvents();
          Throwable savedException = PassExecutorService.getSavedException(progress);
          if (savedException != null) throw savedException;
        });
      }
      if (progress.isRunning() && !progress.isCanceled()) {
        throw new RuntimeException("Highlighting still running after " +(System.currentTimeMillis()-start)/1000 + " seconds." +
                                   " Still submitted passes: "+myPassExecutorService.getAllSubmittedPasses()+
                                   " ForkJoinPool.commonPool(): "+ForkJoinPool.commonPool()+"\n"+
                                   ", ForkJoinPool.commonPool() active thread count: "+ ForkJoinPool.commonPool().getActiveThreadCount()+
                                   ", ForkJoinPool.commonPool() has queued submissions: "+ ForkJoinPool.commonPool().hasQueuedSubmissions()+
                                   "\n"+ ThreadDumper.dumpThreadsToString());
      }

      HighlightingSessionImpl session = (HighlightingSessionImpl)HighlightingSessionImpl.getOrCreateHighlightingSession(file, progress, null);
      wrap(() -> {
        if (!waitInOtherThread(60000, canChangeDocument)) {
          throw new TimeoutException("Unable to complete in 60s");
        }
        session.waitForHighlightInfosApplied();
      });
      UIUtil.dispatchAllInvocationEvents();
      UIUtil.dispatchAllInvocationEvents();
      assert progress.isCanceled() && progress.isDisposed();
    }
    finally {
      DaemonProgressIndicator.setDebug(false);
      fileStatusMap.allowDirt(true);
      waitForTermination();
    }
  }

  @TestOnly
  private boolean waitInOtherThread(int millis, boolean canChangeDocument) throws Throwable {
    Disposable disposable = Disposer.newDisposable();
    // last hope protection against PsiModificationTrackerImpl.incCounter() craziness (yes, Kotlin)
    myProject.getMessageBus().connect(disposable).subscribe(PsiModificationTracker.TOPIC,
      () -> {
        throw new IllegalStateException("You must not perform PSI modifications from inside highlighting");
      });
    if (!canChangeDocument) {
      myProject.getMessageBus().connect(disposable).subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, new DaemonListener() {
        @Override
        public void daemonCancelEventOccurred(@NotNull String reason) {
          throw new IllegalStateException("You must not cancel daemon inside highlighting test: "+reason);
        }
      });
    }

    try {
      Future<Boolean> future = ApplicationManager.getApplication().executeOnPooledThread(() -> {
        try {
          return myPassExecutorService.waitFor(millis);
        }
        catch (Throwable e) {
          throw new RuntimeException(e);
        }
      });
      return future.get();
    }
    finally {
      Disposer.dispose(disposable);
    }
  }

  @TestOnly
  public void prepareForTest() {
    setUpdateByTimerEnabled(false);
    waitForTermination();
    clearReferences();
  }

  @TestOnly
  public void cleanupAfterTest() {
    if (myProject.isOpen()) {
      prepareForTest();
    }
  }

  @TestOnly
  public void waitForTermination() {
    myPassExecutorService.cancelAll(true);
  }

  @Override
  public void settingsChanged() {
    DaemonCodeAnalyzerSettings settings = DaemonCodeAnalyzerSettings.getInstance();
    if (settings.isCodeHighlightingChanged(myLastSettings)) {
      restart();
    }
    myLastSettings = ((DaemonCodeAnalyzerSettingsImpl)settings).clone();
  }

  @Override
  public void updateVisibleHighlighters(@NotNull Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    DeprecatedMethodException.report("Please remove usages of this method deprecated eons ago");
    // no need, will not work anyway
  }

  @Override
  public synchronized void setUpdateByTimerEnabled(boolean value) {
    myUpdateByTimerEnabled = value;
    stopProcess(value, "Update by timer change");
  }

  private final AtomicInteger myDisableCount = new AtomicInteger();

  @Override
  public void disableUpdateByTimer(@NotNull Disposable parentDisposable) {
    setUpdateByTimerEnabled(false);
    myDisableCount.incrementAndGet();
    ApplicationManager.getApplication().assertIsDispatchThread();

    Disposer.register(parentDisposable, () -> {
      if (myDisableCount.decrementAndGet() == 0) {
        setUpdateByTimerEnabled(true);
      }
    });
  }

  synchronized boolean isUpdateByTimerEnabled() {
    return myUpdateByTimerEnabled;
  }

  @Override
  public void setImportHintsEnabled(@NotNull PsiFile file, boolean value) {
    VirtualFile vFile = file.getVirtualFile();
    if (value) {
      myDisabledHintsFiles.remove(vFile);
      stopProcess(true, "Import hints change");
    }
    else {
      myDisabledHintsFiles.add(vFile);
      HintManager.getInstance().hideAllHints();
    }
  }

  @Override
  public void resetImportHintsEnabledForProject() {
    myDisabledHintsFiles.clear();
  }

  @Override
  public void setHighlightingEnabled(@NotNull PsiFile file, boolean value) {
    VirtualFile virtualFile = PsiUtilCore.getVirtualFile(file);
    if (value) {
      myDisabledHighlightingFiles.remove(virtualFile);
    }
    else {
      myDisabledHighlightingFiles.add(virtualFile);
    }
  }

  @Override
  public boolean isHighlightingAvailable(@Nullable PsiFile file) {
    if (file == null || !file.isPhysical()) return false;
    if (myDisabledHighlightingFiles.contains(PsiUtilCore.getVirtualFile(file))) return false;

    if (file instanceof PsiCompiledElement) return false;
    final FileType fileType = file.getFileType();

    // To enable T.O.D.O. highlighting
    return !fileType.isBinary();
  }

  @Override
  public boolean isImportHintsEnabled(@NotNull PsiFile file) {
    return isAutohintsAvailable(file) && !myDisabledHintsFiles.contains(file.getVirtualFile());
  }

  @Override
  public boolean isAutohintsAvailable(PsiFile file) {
    return isHighlightingAvailable(file) && !(file instanceof PsiCompiledElement);
  }

  @Override
  public void restart() {
    doRestart();
  }

  // return true if the progress was really canceled
  boolean doRestart() {
    myFileStatusMap.markAllFilesDirty("Global restart");
    return stopProcess(true, "Global restart");
  }

  @Override
  public void restart(@NotNull PsiFile file) {
    Document document = myPsiDocumentManager.getCachedDocument(file);
    if (document == null) return;
    String reason = "Psi file restart: " + file.getName();
    myFileStatusMap.markFileScopeDirty(document, new TextRange(0, document.getTextLength()), file.getTextLength(), reason);
    stopProcess(true, reason);
  }

  @NotNull
  public List<TextEditorHighlightingPass> getPassesToShowProgressFor(Document document) {
    List<TextEditorHighlightingPass> allPasses = myPassExecutorService.getAllSubmittedPasses();
    List<TextEditorHighlightingPass> result = new ArrayList<>(allPasses.size());
    for (TextEditorHighlightingPass pass : allPasses) {
      if (pass.getDocument() == document || pass.getDocument() == null) {
        result.add(pass);
      }
    }
    return result;
  }

  boolean isAllAnalysisFinished(@NotNull PsiFile file) {
    if (myDisposed) return false;
    Document document = myPsiDocumentManager.getCachedDocument(file);
    return document != null &&
           document.getModificationStamp() == file.getViewProvider().getModificationStamp() &&
           myFileStatusMap.allDirtyScopesAreNull(document);
  }

  @Override
  public boolean isErrorAnalyzingFinished(@NotNull PsiFile file) {
    if (myDisposed) return false;
    Document document = myPsiDocumentManager.getCachedDocument(file);
    return document != null &&
           document.getModificationStamp() == file.getViewProvider().getModificationStamp() &&
           myFileStatusMap.getFileDirtyScope(document, Pass.UPDATE_ALL) == null;
  }

  @Override
  @NotNull
  public FileStatusMap getFileStatusMap() {
    return myFileStatusMap;
  }

  public synchronized boolean isRunning() {
    return !myUpdateProgress.isCanceled();
  }

  @TestOnly
  public boolean isRunningOrPending() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return isRunning() || !myUpdateRunnableFuture.isDone() || GeneralHighlightingPass.isRestartPending();
  }

  // return true if the progress really was canceled
  synchronized boolean stopProcess(boolean toRestartAlarm, @NotNull @NonNls String reason) {
    boolean canceled = cancelUpdateProgress(toRestartAlarm, reason);
    // optimisation: this check is to avoid too many re-schedules in case of thousands of events spikes
    boolean restart = toRestartAlarm && !myDisposed && myInitialized;

    if (restart && myUpdateRunnableFuture.isDone()) {
      myUpdateRunnableFuture = myAlarm.schedule(myUpdateRunnable, mySettings.getAutoReparseDelay(), TimeUnit.MILLISECONDS);
    }

    return canceled;
  }

  // return true if the progress really was canceled
  private synchronized boolean cancelUpdateProgress(boolean toRestartAlarm, @NotNull @NonNls String reason) {
    DaemonProgressIndicator updateProgress = myUpdateProgress;
    if (myDisposed) return false;
    boolean wasCanceled = updateProgress.isCanceled();
    myPassExecutorService.cancelAll(false);
    if (!wasCanceled) {
      PassExecutorService.log(updateProgress, null, "Cancel", reason, toRestartAlarm);
      updateProgress.cancel();
      return true;
    }
    return false;
  }


  static boolean processHighlightsNearOffset(@NotNull Document document,
                                             @NotNull Project project,
                                             @NotNull final HighlightSeverity minSeverity,
                                             final int offset,
                                             final boolean includeFixRange,
                                             @NotNull final Processor<? super HighlightInfo> processor) {
    return processHighlights(document, project, null, 0, document.getTextLength(), info -> {
      if (!isOffsetInsideHighlightInfo(offset, info, includeFixRange)) return true;

      int compare = info.getSeverity().compareTo(minSeverity);
      return compare < 0 || processor.process(info);
    });
  }

  @Nullable
  public HighlightInfo findHighlightByOffset(@NotNull Document document, final int offset, final boolean includeFixRange) {
    return findHighlightByOffset(document, offset, includeFixRange, HighlightSeverity.INFORMATION);
  }

  @Nullable
  HighlightInfo findHighlightByOffset(@NotNull Document document,
                                      final int offset,
                                      final boolean includeFixRange,
                                      @NotNull HighlightSeverity minSeverity) {
    final List<HighlightInfo> foundInfoList = new SmartList<>();
    processHighlightsNearOffset(document, myProject, minSeverity, offset, includeFixRange,
        info -> {
          if (info.getSeverity() == HighlightInfoType.ELEMENT_UNDER_CARET_SEVERITY) {
            return true;
          }
          if (!foundInfoList.isEmpty()) {
            HighlightInfo foundInfo = foundInfoList.get(0);
            int compare = foundInfo.getSeverity().compareTo(info.getSeverity());
            if (compare < 0) {
              foundInfoList.clear();
            }
            else if (compare > 0) {
              return true;
            }
          }
          foundInfoList.add(info);
          return true;
        });

    if (foundInfoList.isEmpty()) return null;
    if (foundInfoList.size() == 1) return foundInfoList.get(0);
    return HighlightInfoComposite.create(foundInfoList);
  }

  private static boolean isOffsetInsideHighlightInfo(int offset, @NotNull HighlightInfo info, boolean includeFixRange) {
    RangeHighlighterEx highlighter = info.getHighlighter();
    if (highlighter == null || !highlighter.isValid()) return false;
    int startOffset = highlighter.getStartOffset();
    int endOffset = highlighter.getEndOffset();
    if (startOffset <= offset && offset <= endOffset) {
      return true;
    }
    if (!includeFixRange) return false;
    RangeMarker fixMarker = info.fixMarker;
    if (fixMarker != null) {  // null means its range is the same as highlighter
      if (!fixMarker.isValid()) return false;
      startOffset = fixMarker.getStartOffset();
      endOffset = fixMarker.getEndOffset();
      return startOffset <= offset && offset <= endOffset;
    }
    return false;
  }

  @NotNull
  public static List<LineMarkerInfo> getLineMarkers(@NotNull Document document, @NotNull Project project) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    List<LineMarkerInfo> result = new ArrayList<>();
    LineMarkersUtil.processLineMarkers(project, document, new TextRange(0, document.getTextLength()), -1,
                                       new CommonProcessors.CollectProcessor<>(result));
    return result;
  }

  @Nullable
  public IntentionHintComponent getLastIntentionHint() {
    return ((IntentionsUIImpl)IntentionsUI.getInstance(myProject)).getLastIntentionHint();
  }

  @Nullable
  @Override
  public Element getState() {
    Element state = new Element("state");
    if (myDisabledHintsFiles.isEmpty()) {
      return state;
    }

    List<String> array = new SmartList<>();
    for (VirtualFile file : myDisabledHintsFiles) {
      if (file.isValid()) {
        array.add(file.getUrl());
      }
    }

    if (!array.isEmpty()) {
      Collections.sort(array);

      Element disableHintsElement = new Element(DISABLE_HINTS_TAG);
      state.addContent(disableHintsElement);
      for (String url : array) {
        disableHintsElement.addContent(new Element(FILE_TAG).setAttribute(URL_ATT, url));
      }
    }
    return state;
  }

  @Override
  public void loadState(@NotNull Element state) {
    myDisabledHintsFiles.clear();

    Element element = state.getChild(DISABLE_HINTS_TAG);
    if (element != null) {
      for (Element e : element.getChildren(FILE_TAG)) {
        String url = e.getAttributeValue(URL_ATT);
        if (url != null) {
          VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
          if (file != null) {
            myDisabledHintsFiles.add(file);
          }
        }
      }
    }
  }

  // made this class static and fields cleareable to avoid leaks when this object stuck in invokeLater queue
  private static class UpdateRunnable implements Runnable {
    private Project myProject;
    private UpdateRunnable(@NotNull Project project) {
      myProject = project;
    }

    @Override
    public void run() {
      ApplicationManager.getApplication().assertIsDispatchThread();
      Project project = myProject;
      DaemonCodeAnalyzerImpl dca;
      if (project == null ||
          !project.isInitialized() ||
          project.isDisposed() ||
          PowerSaveMode.isEnabled() ||
          (dca = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project)).myDisposed) {
        return;
      }

      final Collection<FileEditor> activeEditors = dca.getSelectedEditors();
      boolean updateByTimerEnabled = dca.isUpdateByTimerEnabled();
      PassExecutorService.log(dca.getUpdateProgress(), null, "Update Runnable. myUpdateByTimerEnabled:",
                              updateByTimerEnabled, " something disposed:",
                              PowerSaveMode.isEnabled() || !myProject.isInitialized(), " activeEditors:",
                              activeEditors);
      if (!updateByTimerEnabled) return;

      if (activeEditors.isEmpty()) return;

      if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
        // makes no sense to start from within write action, will cancel anyway
        // we'll restart when the write action finish
        return;
      }
      if (dca.myPsiDocumentManager.hasUncommitedDocuments()) {
        // restart when everything committed
        dca.myPsiDocumentManager.performLaterWhenAllCommitted(this);
        return;
      }
      if (RefResolveService.ENABLED &&
          !RefResolveService.getInstance(myProject).isUpToDate() &&
          RefResolveService.getInstance(myProject).getQueueSize() == 1) {
        return; // if the user have just typed in something, wait until the file is re-resolved
        // (or else it will blink like crazy since unused symbols calculation depends on resolve service)
      }

      Map<FileEditor, HighlightingPass[]> passes = new THashMap<>(activeEditors.size());
      for (FileEditor fileEditor : activeEditors) {
        BackgroundEditorHighlighter highlighter = fileEditor.getBackgroundHighlighter();
        if (highlighter != null) {
          HighlightingPass[] highlightingPasses = highlighter.createPassesForEditor();
          passes.put(fileEditor, highlightingPasses);
        }
      }

      // wait for heavy processing to stop, re-schedule daemon but not too soon
      if (HeavyProcessLatch.INSTANCE.isRunning()) {
        boolean hasPasses = false;
        for (Map.Entry<FileEditor, HighlightingPass[]> entry : passes.entrySet()) {
          HighlightingPass[] filtered = Arrays.stream(entry.getValue()).filter(DumbService::isDumbAware).toArray(HighlightingPass[]::new);
          entry.setValue(filtered);
          hasPasses |= filtered.length != 0;
        }
        if (!hasPasses) {
          HeavyProcessLatch.INSTANCE.executeOutOfHeavyProcess(() ->
            dca.stopProcess(true, "re-scheduled to execute after heavy processing finished"));
          return;
        }
      }

      // cancel all after calling createPasses() since there are perverts {@link com.intellij.util.xml.ui.DomUIFactoryImpl} who are changing PSI there
      dca.cancelUpdateProgress(true, "Cancel by alarm");
      dca.myUpdateRunnableFuture.cancel(false);
      DaemonProgressIndicator progress = dca.createUpdateProgress(passes.keySet());
      dca.myPassExecutorService.submitPasses(passes, progress);
    }

    private void clearFieldsOnDispose() {
      myProject = null;
    }
  }

  @NotNull
  private synchronized DaemonProgressIndicator createUpdateProgress(@NotNull Collection<FileEditor> fileEditors) {
    DaemonProgressIndicator old = myUpdateProgress;
    if (!old.isCanceled()) {
      old.cancel();
    }
    DaemonProgressIndicator progress = new MyDaemonProgressIndicator(myProject, fileEditors);
    progress.setModalityProgress(null);
    progress.start();
    myProject.getMessageBus().syncPublisher(DAEMON_EVENT_TOPIC).daemonStarting(fileEditors);
    myUpdateProgress = progress;
    return progress;
  }

  private static class MyDaemonProgressIndicator extends DaemonProgressIndicator {
    private final Project myProject;
    private Collection<FileEditor> myFileEditors;

    MyDaemonProgressIndicator(@NotNull Project project, @NotNull Collection<FileEditor> fileEditors) {
      myFileEditors = fileEditors;
      myProject = project;
    }

    @Override
    public void stopIfRunning() {
      super.stopIfRunning();
      myProject.getMessageBus().syncPublisher(DAEMON_EVENT_TOPIC).daemonFinished(myFileEditors);
      myFileEditors = null;
      HighlightingSessionImpl.clearProgressIndicator(this);
    }
  }


  @Override
  public void autoImportReferenceAtCursor(@NotNull Editor editor, @NotNull PsiFile file) {
    for (ReferenceImporter importer : ReferenceImporter.EP_NAME.getExtensionList()) {
      if (importer.autoImportReferenceAtCursor(editor, file)) break;
    }
  }

  @TestOnly
  @NotNull
  public synchronized DaemonProgressIndicator getUpdateProgress() {
    return myUpdateProgress;
  }

  @NotNull
  private Collection<FileEditor> getSelectedEditors() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    // Editors in modal context
    List<Editor> editors = myEditorTracker.getActiveEditors();
    Collection<FileEditor> activeTextEditors = new THashSet<>(editors.size());
    for (Editor editor : editors) {
      if (editor.isDisposed()) continue;
      TextEditor textEditor = TextEditorProvider.getInstance().getTextEditor(editor);
      activeTextEditors.add(textEditor);
    }
    if (ApplicationManager.getApplication().getCurrentModalityState() != ModalityState.NON_MODAL) {
      return activeTextEditors;
    }

    Collection<FileEditor> result = new THashSet<>();
    Collection<VirtualFile> files = new THashSet<>(activeTextEditors.size());
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      // Editors in tabs.
      final FileEditor[] tabEditors = FileEditorManager.getInstance(myProject).getSelectedEditors();
      for (FileEditor tabEditor : tabEditors) {
        if (!tabEditor.isValid()) continue;
        VirtualFile file = ((FileEditorManagerEx)FileEditorManager.getInstance(myProject)).getFile(tabEditor);
        if (file != null) {
          files.add(file);
        }
        result.add(tabEditor);
      }
    }

    // do not duplicate documents
    for (FileEditor fileEditor : activeTextEditors) {
      VirtualFile file = ((FileEditorManagerEx)FileEditorManager.getInstance(myProject)).getFile(fileEditor);
      if (file != null && files.contains(file)) continue;
      result.add(fileEditor);
    }
    return result;
  }

  @TestOnly
  private static void wrap(@NotNull ThrowableRunnable runnable) {
    try {
      runnable.run();
    }
    catch (RuntimeException | Error e) {
      throw e;
    }
    catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
