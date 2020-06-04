// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ex;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisUIOptions;
import com.intellij.analysis.PerformAnalysisInBackgroundOption;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.daemon.ProblemHighlightFilter;
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator;
import com.intellij.codeInsight.daemon.impl.HighlightInfoProcessor;
import com.intellij.codeInsight.daemon.impl.LocalInspectionsPass;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.actions.CleanupInspectionUtil;
import com.intellij.codeInspection.lang.GlobalInspectionContextExtension;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.reference.RefGraphAnnotator;
import com.intellij.codeInspection.reference.RefManagerImpl;
import com.intellij.codeInspection.ui.DefaultInspectionToolPresentation;
import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.codeInspection.ui.InspectionToolPresentation;
import com.intellij.concurrency.JobLauncher;
import com.intellij.concurrency.JobLauncherImpl;
import com.intellij.concurrency.SensitiveProgressWrapper;
import com.intellij.diagnostic.ThreadDumper;
import com.intellij.lang.LangBundle;
import com.intellij.lang.annotation.ProblemGroup;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.*;
import com.intellij.openapi.roots.FileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class GlobalInspectionContextImpl extends GlobalInspectionContextEx {
  private static final boolean INSPECT_INJECTED_PSI = SystemProperties.getBooleanProperty("idea.batch.inspections.inspect.injected.psi", true);
  private static final Logger LOG = Logger.getInstance(GlobalInspectionContextImpl.class);
  @SuppressWarnings("StaticNonFinalField")
  @TestOnly
  public static volatile boolean TESTING_VIEW;
  public static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.toolWindowGroup("Inspection Results", ToolWindowId.INSPECTION);

  private final NotNullLazyValue<? extends ContentManager> myContentManager;
  private volatile InspectionResultsView myView;
  private Content myContent;
  private volatile boolean myViewClosed = true;
  private long myInspectionStartedTimestamp;
  private final ConcurrentMap<InspectionToolWrapper<?, ?>, InspectionToolPresentation> myPresentationMap = new ConcurrentHashMap<>();

  public GlobalInspectionContextImpl(@NotNull Project project, @NotNull NotNullLazyValue<? extends ContentManager> contentManager) {
    super(project);
    myContentManager = contentManager;
  }

  private @NotNull ContentManager getContentManager() {
    return myContentManager.getValue();
  }

  public void addView(@NotNull InspectionResultsView view,
                      @NotNull String title,
                      boolean isOffline) {
    LOG.assertTrue(myContent == null, "GlobalInspectionContext is busy under other view now");
    myView = view;
    if (!isOffline) {
      myView.setUpdating(true);
    }
    myContent = ContentFactory.SERVICE.getInstance().createContent(view, title, false);
    myContent.setHelpId(InspectionResultsView.HELP_ID);
    myContent.setDisposer(myView);
    Disposer.register(myContent, () -> {
      if (myView != null) {
        close(false);
      }
      myContent = null;
    });
    RefManagerImpl.EP_NAME.addExtensionPointListener(new ExtensionPointListener<RefGraphAnnotator>() {
      @Override
      public void extensionRemoved(@NotNull RefGraphAnnotator graphAnnotator, @NotNull PluginDescriptor pluginDescriptor) {
        ((RefManagerImpl)getRefManager()).unregisterAnnotator(graphAnnotator);
      }
    }, myContent);

    ContentManager contentManager = getContentManager();
    contentManager.addContent(myContent);
    contentManager.setSelectedContent(myContent);

    ToolWindowManager.getInstance(getProject()).getToolWindow(ToolWindowId.INSPECTION).activate(null);
  }

  public void addView(@NotNull InspectionResultsView view) {
    addView(view, InspectionsBundle.message(view.isSingleInspectionRun() ?
                                            "inspection.results.for.inspection.toolwindow.title" :
                                            "inspection.results.for.profile.toolwindow.title",
                                            view.getCurrentProfileName(), getCurrentScope().getShortenName()), false);

  }

  @Override
  public void doInspections(final @NotNull AnalysisScope scope) {
    if (myContent != null) {
      getContentManager().removeContent(myContent, true);
    }
    super.doInspections(scope);
  }

  public void resolveElement(@NotNull InspectionProfileEntry tool, @NotNull PsiElement element) {
    final RefElement refElement = getRefManager().getReference(element);
    if (refElement == null) return;
    final Tools tools = getTools().get(tool.getShortName());
    if (tools != null){
      for (ScopeToolState state : tools.getTools()) {
        InspectionToolWrapper<?, ?> toolWrapper = state.getTool();
        InspectionToolResultExporter presentation = getPresentationOrNull(toolWrapper);
        if (presentation != null) {
          resolveElementRecursively(presentation, refElement);
        }
      }
    }
  }

  public InspectionResultsView getView() {
    return myView;
  }

  private static void resolveElementRecursively(@NotNull InspectionToolResultExporter presentation, @NotNull RefEntity refElement) {
    presentation.suppressProblem(refElement);
    final List<RefEntity> children = refElement.getChildren();
    for (RefEntity child : children) {
      resolveElementRecursively(presentation, child);
    }
  }

  public @NotNull AnalysisUIOptions getUIOptions() {
    return AnalysisUIOptions.getInstance(getProject());
  }

  public void setSplitterProportion(final float proportion) {
    getUIOptions().SPLITTER_PROPORTION = proportion;
  }

  public @NotNull ToggleAction createToggleAutoscrollAction() {
    return getUIOptions().getAutoScrollToSourceHandler().createToggleAction();
  }

  @Override
  protected void launchInspections(final @NotNull AnalysisScope scope) {
    myViewClosed = false;
    super.launchInspections(scope);
  }

  @Override
  protected @NotNull PerformInBackgroundOption createOption() {
    return new PerformAnalysisInBackgroundOption(getProject());
  }

  @Override
  protected void notifyInspectionsFinished(final @NotNull AnalysisScope scope) {
    //noinspection TestOnlyProblems
    if (ApplicationManager.getApplication().isUnitTestMode() && !TESTING_VIEW) return;
    LOG.assertTrue(ApplicationManager.getApplication().isDispatchThread());
    long elapsed = System.currentTimeMillis() - myInspectionStartedTimestamp;
    LOG.info("Code inspection finished. Took " + elapsed + "ms");
    if (getProject().isDisposed()) return;

    InspectionResultsView newView = myView == null ? new InspectionResultsView(this, new InspectionRVContentProviderImpl()) : null;
    if (!(myView == null ? newView : myView).hasProblems()) {
      int totalFiles = getStdJobDescriptors().BUILD_GRAPH.getTotalAmount(); // do not use invalidated scope
      NOTIFICATION_GROUP.createNotification(InspectionsBundle.message("inspection.no.problems.message",
                                                                      totalFiles,
                                                                      scope.getShortenName()),
                                            MessageType.INFO).notify(getProject());
      close(true);
      if (newView != null) {
        Disposer.dispose(newView);
      }
    }
    else if (newView != null && !newView.isDisposed() && getCurrentScope() != null) {
      addView(newView);
      newView.update();
    }
    if (myView != null) {
      myView.setUpdating(false);
    }
  }

  @Override
  protected void runTools(final @NotNull AnalysisScope scope, boolean runGlobalToolsOnly, boolean isOfflineInspections) {
    myInspectionStartedTimestamp = System.currentTimeMillis();
    final ProgressIndicator progressIndicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
    if (progressIndicator == null) {
      throw new IncorrectOperationException("Must be run under progress");
    }
    if (!isOfflineInspections && ApplicationManager.getApplication().isDispatchThread()) {
      throw new IncorrectOperationException("Must not start inspections from within EDT");
    }
    if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
      throw new IncorrectOperationException("Must not start inspections from within write action");
    }
    // in offline inspection application we don't care about global read action
    if (!isOfflineInspections && ApplicationManager.getApplication().isReadAccessAllowed()) {
      throw new IncorrectOperationException("Must not start inspections from within global read action");
    }
    final InspectionManager inspectionManager = InspectionManager.getInstance(getProject());
    ((RefManagerImpl)getRefManager()).initializeAnnotators();
    final List<Tools> globalTools = new ArrayList<>();
    final List<Tools> localTools = new ArrayList<>();
    final List<Tools> globalSimpleTools = new ArrayList<>();
    initializeTools(globalTools, localTools, globalSimpleTools);
    appendPairedInspectionsForUnfairTools(globalTools, globalSimpleTools, localTools);
    runGlobalTools(scope, inspectionManager, globalTools, isOfflineInspections);

    if (runGlobalToolsOnly || localTools.isEmpty() && globalSimpleTools.isEmpty()) return;

    SearchScope searchScope = ReadAction.compute(scope::toSearchScope);
    final Set<VirtualFile> localScopeFiles = searchScope instanceof LocalSearchScope ? new THashSet<>() : null;
    for (Tools tools : globalSimpleTools) {
      GlobalInspectionToolWrapper toolWrapper = (GlobalInspectionToolWrapper)tools.getTool();
      GlobalSimpleInspectionTool tool = (GlobalSimpleInspectionTool)toolWrapper.getTool();
      tool.inspectionStarted(inspectionManager, this, getPresentation(toolWrapper));
    }

    final boolean headlessEnvironment = ApplicationManager.getApplication().isHeadlessEnvironment();
    final Map<String, InspectionToolWrapper<?, ?>> map = getInspectionWrappersMap(localTools);

    final BlockingQueue<VirtualFile> filesToInspect = new ArrayBlockingQueue<>(1000);
    // use original progress indicator here since we don't want it to cancel on write action start
    ProgressIndicator iteratingIndicator = new SensitiveProgressWrapper(progressIndicator);
    Future<?> future = startIterateScopeInBackground(scope, localScopeFiles, headlessEnvironment, filesToInspect, iteratingIndicator);

    PsiManager psiManager = PsiManager.getInstance(getProject());
    Processor<VirtualFile> processor = virtualFile -> {
      ProgressManager.checkCanceled();
      Boolean readActionSuccess = DumbService.getInstance(getProject()).tryRunReadActionInSmartMode(() -> {
        long start = myProfile == null ? 0 : System.currentTimeMillis();
        PsiFile file = psiManager.findFile(virtualFile);
        if (file == null) {
          return true;
        }
        if (!scope.contains(virtualFile)) {
          LOG.info(file.getName() + "; scope: " + scope + "; " + virtualFile);
          return true;
        }
        boolean includeDoNotShow = includeDoNotShow(getCurrentProfile());
        inspectFile(file, getEffectiveRange(searchScope, file), inspectionManager, map,
                    getWrappersFromTools(globalSimpleTools, file, includeDoNotShow,
                                         wrapper -> !(wrapper.getTool() instanceof ExternalAnnotatorBatchInspection)),
                    getWrappersFromTools(localTools, file, includeDoNotShow,
                                         wrapper -> !(wrapper.getTool() instanceof ExternalAnnotatorBatchInspection)));
        if (start != 0) {
          updateProfile(virtualFile, System.currentTimeMillis() - start);
        }
        return true;
      }, "Inspect code is not available until indices are ready");
      if (readActionSuccess == null || !readActionSuccess) {
        throw new ProcessCanceledException();
      }

      PsiFile file = ReadAction.compute(() -> psiManager.findFile(virtualFile));
      if (file == null) {
        return true;
      }

      boolean includeDoNotShow = includeDoNotShow(getCurrentProfile());
      List<InspectionToolWrapper<?, ?>> externalAnnotatable = ContainerUtil.concat(
        getWrappersFromTools(localTools, file, includeDoNotShow, wrapper -> wrapper.getTool() instanceof ExternalAnnotatorBatchInspection),
        getWrappersFromTools(globalSimpleTools, file, includeDoNotShow, wrapper -> wrapper.getTool() instanceof ExternalAnnotatorBatchInspection));
      externalAnnotatable.forEach(wrapper -> {
          ProblemDescriptor[] descriptors = ((ExternalAnnotatorBatchInspection)wrapper.getTool()).checkFile(file, this, inspectionManager);
          InspectionToolResultExporter toolPresentation = getPresentation(wrapper);
          ReadAction.run(() -> BatchModeDescriptorsUtil
            .addProblemDescriptors(Arrays.asList(descriptors), false, this, null, CONVERT, toolPresentation));
        });

      return true;
    };
    try {
      final Queue<VirtualFile> filesFailedToInspect = new LinkedBlockingQueue<>();
      while (true) {
        Disposable disposable = Disposer.newDisposable();
        ProgressIndicator wrapper = new DaemonProgressIndicator();
        dependentIndicators.add(wrapper);
        try {
          // avoid "attach listener"/"write action" race
          ReadAction.run(() -> {
            wrapper.start();
            ProgressIndicatorUtils.forceWriteActionPriority(wrapper, disposable);
            // there is a chance we are racing with write action, in which case just registered listener might not be called, retry.
            if (ApplicationManagerEx.getApplicationEx().isWriteActionPending()) {
              throw new ProcessCanceledException();
            }
          });
          // use wrapper here to cancel early when write action start but do not affect the original indicator
          ((JobLauncherImpl)JobLauncher.getInstance()).processQueue(filesToInspect, filesFailedToInspect, wrapper, TOMBSTONE, processor);
          break;
        }
        catch (ProcessCanceledException e) {
          progressIndicator.checkCanceled();
          // PCE may be thrown from inside wrapper when write action started
          // go on with the write and then resume processing the rest of the queue
          assert isOfflineInspections || !ApplicationManager.getApplication().isReadAccessAllowed()
            : "Must be outside read action. PCE=\n" + ExceptionUtil.getThrowableText(e);
          assert isOfflineInspections || !ApplicationManager.getApplication().isDispatchThread()
            : "Must be outside EDT. PCE=\n" + ExceptionUtil.getThrowableText(e);

          // wait for write action to complete
          ApplicationManager.getApplication().runReadAction(EmptyRunnable.getInstance());
        }
        finally {
          dependentIndicators.remove(wrapper);
          Disposer.dispose(disposable);
        }
      }
    }
    finally {
      iteratingIndicator.cancel(); // tell file scanning thread to stop
      filesToInspect.clear(); // let file scanning thread a chance to put TOMBSTONE and complete
      try {
        future.get(30, TimeUnit.SECONDS);
      }
      catch (Exception e) {
        LOG.error("Thread dump: \n"+ThreadDumper.dumpThreadsToString(), e);
      }
    }

    ProgressManager.checkCanceled();

    for (Tools tools : globalSimpleTools) {
      GlobalInspectionToolWrapper toolWrapper = (GlobalInspectionToolWrapper)tools.getTool();
      GlobalSimpleInspectionTool tool = (GlobalSimpleInspectionTool)toolWrapper.getTool();
      ProblemDescriptionsProcessor problemDescriptionProcessor = getProblemDescriptionProcessor(toolWrapper, map);
      tool.inspectionFinished(inspectionManager, this, problemDescriptionProcessor);

    }

    addProblemsToView(globalSimpleTools);
  }

  private static TextRange getEffectiveRange(SearchScope searchScope, PsiFile file) {
    if (searchScope instanceof LocalSearchScope) {
      List<PsiElement> scopeFileElements = ContainerUtil.filter(((LocalSearchScope)searchScope).getScope(), e -> e.getContainingFile() == file);
      if (!scopeFileElements.isEmpty()) {
        int start = -1;
        int end = -1;
        for (PsiElement scopeElement : scopeFileElements) {
          TextRange elementRange = scopeElement.getTextRange();
          start = start == -1 ? elementRange.getStartOffset() : Math.min(elementRange.getStartOffset(), start);
          end = end == -1 ? elementRange.getEndOffset() : Math.max(elementRange.getEndOffset(), end);
        }
        return new TextRange(start, end);
      }
    }
    return new TextRange(0, file.getTextLength());
  }

  // indicators which should be canceled once the main indicator myProgressIndicator is
  private final List<ProgressIndicator> dependentIndicators = ContainerUtil.createLockFreeCopyOnWriteList();

  @Override
  protected void canceled() {
    super.canceled();
    dependentIndicators.forEach(ProgressIndicator::cancel);
  }

  private void inspectFile(final @NotNull PsiFile file,
                           final @NotNull TextRange range,
                           final @NotNull InspectionManager inspectionManager,
                           final @NotNull Map<String, InspectionToolWrapper<?, ?>> wrappersMap,
                           @NotNull List<? extends GlobalInspectionToolWrapper> globalSimpleTools,
                           @NotNull List<? extends LocalInspectionToolWrapper> localTools) {
    Document document = PsiDocumentManager.getInstance(getProject()).getDocument(file);
    if (document == null) return;

    try {
      file.putUserData(InspectionProfileWrapper.CUSTOMIZATION_KEY, p -> new InspectionProfileWrapper(getCurrentProfile()));
      LocalInspectionsPass pass = new LocalInspectionsPass(file, document, range.getStartOffset(),
                                                           range.getEndOffset(), LocalInspectionsPass.EMPTY_PRIORITY_RANGE, true,
                                                           HighlightInfoProcessor.getEmpty(), INSPECT_INJECTED_PSI);
      pass.doInspectInBatch(this, inspectionManager, localTools);

      assertUnderDaemonProgress();

      JobLauncher.getInstance()
        .invokeConcurrentlyUnderProgress(globalSimpleTools, ProgressIndicatorProvider.getGlobalProgressIndicator(), toolWrapper -> {
          GlobalSimpleInspectionTool tool = (GlobalSimpleInspectionTool)toolWrapper.getTool();
          ProblemsHolder holder = new ProblemsHolder(inspectionManager, file, false);
          ProblemDescriptionsProcessor problemDescriptionProcessor = getProblemDescriptionProcessor(toolWrapper, wrappersMap);
          tool.checkFile(file, inspectionManager, holder, this, problemDescriptionProcessor);
          InspectionToolResultExporter toolPresentation = getPresentation(toolWrapper);
          BatchModeDescriptorsUtil.addProblemDescriptors(holder.getResults(), false, this, null, CONVERT, toolPresentation);
          return true;
        });
      VirtualFile virtualFile = file.getVirtualFile();
      String displayUrl = ProjectUtilCore.displayUrlRelativeToProject(virtualFile, virtualFile.getPresentableUrl(), getProject(), true, false);
      incrementJobDoneAmount(getStdJobDescriptors().LOCAL_ANALYSIS, displayUrl);
    }
    catch (ProcessCanceledException | IndexNotReadyException e) {
      throw e;
    }
    catch (Throwable e) {
      LOG.error("In file: " + file.getViewProvider().getVirtualFile().getPath(), e);
    }
    finally {
      file.putUserData(InspectionProfileWrapper.CUSTOMIZATION_KEY, null);
      InjectedLanguageManager.getInstance(getProject()).dropFileCaches(file);
    }
  }

  protected boolean includeDoNotShow(final InspectionProfile profile) {
    return profile.getSingleTool() != null;
  }

  private static final VirtualFile TOMBSTONE = new LightVirtualFile("TOMBSTONE");

  private @NotNull Future<?> startIterateScopeInBackground(final @NotNull AnalysisScope scope,
                                                           final @Nullable Collection<? super VirtualFile> localScopeFiles,
                                                           final boolean headlessEnvironment,
                                                           final @NotNull BlockingQueue<? super VirtualFile> outFilesToInspect,
                                                           final @NotNull ProgressIndicator progressIndicator) {
    Task.Backgroundable task = new Task.Backgroundable(getProject(), InspectionsBundle.message("scanning.files.to.inspect.progress.text")) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          final FileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
          scope.accept(file -> {
            ProgressManager.checkCanceled();
            if (ProjectUtil.isProjectOrWorkspaceFile(file) || !fileIndex.isInContent(file)) return true;

            PsiFile psiFile = ReadAction.compute(() -> {
              if (getProject().isDisposed()) throw new ProcessCanceledException();
              PsiFile psi = PsiManager.getInstance(getProject()).findFile(file);
              Document document = psi == null ? null : shouldProcess(psi, headlessEnvironment, localScopeFiles);
              if (document != null) {
                return psi;
              }
              return null;
            });
            // do not inspect binary files
            if (psiFile != null) {
              try {
                if (ApplicationManager.getApplication().isReadAccessAllowed()) {
                  throw new IllegalStateException("Must not have read action");
                }
                outFilesToInspect.put(file);
              }
              catch (InterruptedException e) {
                LOG.error(e);
              }
            }
            ProgressManager.checkCanceled();
            return true;
          });
        }
        catch (ProcessCanceledException e) {
          // ignore, but put tombstone
        }
        finally {
          try {
            outFilesToInspect.put(TOMBSTONE);
          }
          catch (InterruptedException e) {
            LOG.error(e);
          }
        }
      }
    };
    return ((CoreProgressManager)ProgressManager.getInstance()).runProcessWithProgressAsynchronously(task, progressIndicator, null);
  }

  private Document shouldProcess(@NotNull PsiFile file, boolean headlessEnvironment, @Nullable Collection<? super VirtualFile> localScopeFiles) {
    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return null;
    if (isBinary(file)) return null; //do not inspect binary files

    if (myViewClosed && !headlessEnvironment) {
      throw new ProcessCanceledException();
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Running local inspections on " + virtualFile.getPath());
    }

    if (SingleRootFileViewProvider.isTooLargeForIntelligence(virtualFile)) return null;
    if (localScopeFiles != null && !localScopeFiles.add(virtualFile)) return null;
    if (!ProblemHighlightFilter.shouldProcessFileInBatch(file)) return null;

    return PsiDocumentManager.getInstance(getProject()).getDocument(file);
  }

  private void runGlobalTools(final @NotNull AnalysisScope scope,
                              final @NotNull InspectionManager inspectionManager,
                              @NotNull List<? extends Tools> globalTools,
                              boolean isOfflineInspections) {
    LOG.assertTrue(!ApplicationManager.getApplication().isReadAccessAllowed() || isOfflineInspections, "Must not run under read action, too unresponsive");
    final List<InspectionToolWrapper<?, ?>> needRepeatSearchRequest = new ArrayList<>();

    SearchScope initialSearchScope = ReadAction.compute(scope::toSearchScope);
    final boolean canBeExternalUsages = !(scope.getScopeType() == AnalysisScope.PROJECT && scope.isIncludeTestSource());
    for (Tools tools : globalTools) {
      for (ScopeToolState state : tools.getTools()) {
        if (!state.isEnabled()) continue;
        NamedScope stateScope = state.getScope(getProject());
        if (stateScope == null) continue;

        AnalysisScope scopeForState = new AnalysisScope(GlobalSearchScopesCore.filterScope(getProject(), stateScope)
                                                          .intersectWith(initialSearchScope), getProject());
        final InspectionToolWrapper<?, ?> toolWrapper = state.getTool();
        final GlobalInspectionTool tool = (GlobalInspectionTool)toolWrapper.getTool();
        final InspectionToolResultExporter toolPresentation = getPresentation(toolWrapper);
        try {
          if (tool.isGraphNeeded()) {
            try {
              ((RefManagerImpl)getRefManager()).findAllDeclarations();
            }
            catch (Throwable e) {
              getStdJobDescriptors().BUILD_GRAPH.setDoneAmount(0);
              throw e;
            }
          }
          ThrowableRunnable<RuntimeException> runnable = () -> {
            tool.runInspection(scopeForState, inspectionManager, this, toolPresentation);
            //skip phase when we are sure that scope already contains everything, unused declaration though needs to proceed with its suspicious code
            if ((canBeExternalUsages || tool.getAdditionalJobs(this) != null) &&
                tool.queryExternalUsagesRequests(inspectionManager, this, toolPresentation)) {
              needRepeatSearchRequest.add(toolWrapper);
            }
          };
          if (tool.isReadActionNeeded()) {
            ReadAction.run(runnable);
          }
          else {
            runnable.run();
          }
        }
        catch (ProcessCanceledException | IndexNotReadyException e) {
          throw e;
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }
    }

    for (GlobalInspectionContextExtension<?> extension : myExtensions.values()) {
      try {
        extension.performPostRunActivities(needRepeatSearchRequest, this);
      }
      catch (ProcessCanceledException | IndexNotReadyException e) {
        throw e;
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }

    addProblemsToView(globalTools);
  }

  public ActionCallback initializeViewIfNeeded() {
    if (myView != null) {
      return ActionCallback.DONE;
    }
    return ApplicationManager.getApplication().getInvokator().invokeLater(() -> {
      if (getCurrentScope() == null) {
        return;
      }
      InspectionResultsView view = getView();
      if (view == null) {
        view = new InspectionResultsView(this, new InspectionRVContentProviderImpl());
        addView(view);
      }
    }, x -> getCurrentScope() == null);
  }

  private void appendPairedInspectionsForUnfairTools(@NotNull List<? super Tools> globalTools,
                                                     @NotNull List<? super Tools> globalSimpleTools,
                                                     @NotNull List<Tools> localTools) {
    Tools[] lArray = localTools.toArray(new Tools[0]);
    for (Tools tool : lArray) {
      LocalInspectionToolWrapper toolWrapper = (LocalInspectionToolWrapper)tool.getTool();
      LocalInspectionTool localTool = toolWrapper.getTool();
      if (localTool instanceof PairedUnfairLocalInspectionTool) {
        String batchShortName = ((PairedUnfairLocalInspectionTool)localTool).getInspectionForBatchShortName();
        InspectionProfile currentProfile = getCurrentProfile();
        InspectionToolWrapper<?, ?> batchInspection;
        final InspectionToolWrapper<?, ?> pairedWrapper = currentProfile.getInspectionTool(batchShortName, getProject());
        batchInspection = pairedWrapper != null ? pairedWrapper.createCopy() : null;
        if (batchInspection != null && !getTools().containsKey(batchShortName)) {
          // add to existing inspections to run
          InspectionProfileEntry batchTool = batchInspection.getTool();
          final ScopeToolState defaultState = tool.getDefaultState();
          ToolsImpl newTool = new ToolsImpl(batchInspection, defaultState.getLevel(), true, defaultState.isEnabled());
          for (ScopeToolState state : tool.getTools()) {
            final NamedScope scope = state.getScope(getProject());
            if (scope != null) {
              newTool.addTool(scope, batchInspection, state.isEnabled(), state.getLevel());
            }
          }
          if (batchTool instanceof LocalInspectionTool) localTools.add(newTool);
          else if (batchTool instanceof GlobalSimpleInspectionTool) globalSimpleTools.add(newTool);
          else if (batchTool instanceof GlobalInspectionTool) globalTools.add(newTool);
          else throw new AssertionError(batchTool);
          myTools.put(batchShortName, newTool);
          batchInspection.initialize(this);
        }
      }
    }
  }

  private static @NotNull <T extends InspectionToolWrapper<?, ?>> List<T> getWrappersFromTools(@NotNull List<? extends Tools> localTools,
                                                                                               @NotNull PsiFile file,
                                                                                               boolean includeDoNotShow,
                                                                                               @NotNull Predicate<? super T> filter) {
    return ContainerUtil.mapNotNull(localTools, tool -> {
      //noinspection unchecked
      T unwrapped = (T)tool.getEnabledTool(file, includeDoNotShow);
      if (unwrapped == null) return null;
      return filter.test(unwrapped) ? unwrapped : null;
    });
  }

  private @NotNull ProblemDescriptionsProcessor getProblemDescriptionProcessor(final @NotNull GlobalInspectionToolWrapper toolWrapper,
                                                                               final @NotNull Map<String, InspectionToolWrapper<?, ?>> wrappersMap) {
    return new ProblemDescriptionsProcessor() {
      @Override
      public void addProblemElement(@Nullable RefEntity refEntity, CommonProblemDescriptor @NotNull ... commonProblemDescriptors) {
        for (CommonProblemDescriptor problemDescriptor : commonProblemDescriptors) {
          if (!(problemDescriptor instanceof ProblemDescriptor)) {
            continue;
          }
          if (SuppressionUtil.inspectionResultSuppressed(((ProblemDescriptor)problemDescriptor).getPsiElement(), toolWrapper.getTool())) {
            continue;
          }
          ProblemGroup problemGroup = ((ProblemDescriptor)problemDescriptor).getProblemGroup();

          InspectionToolWrapper<?, ?> targetWrapper = problemGroup == null ? toolWrapper : wrappersMap.get(problemGroup.getProblemName());
          if (targetWrapper != null) { // Else it's switched off
            InspectionToolResultExporter toolPresentation = getPresentation(targetWrapper);
            toolPresentation.addProblemElement(refEntity, problemDescriptor);
          }
        }
      }
    };
  }

  private static @NotNull Map<String, InspectionToolWrapper<?, ?>> getInspectionWrappersMap(@NotNull List<? extends Tools> tools) {
    Map<String, InspectionToolWrapper<?, ?>> name2Inspection = new HashMap<>(tools.size());
    for (Tools tool : tools) {
      InspectionToolWrapper<?, ?> toolWrapper = tool.getTool();
      name2Inspection.put(toolWrapper.getShortName(), toolWrapper);
    }

    return name2Inspection;
  }

  private static final TripleFunction<LocalInspectionTool,PsiElement,GlobalInspectionContext,RefElement> CONVERT =
    (tool, elt, context) -> {
      PsiNamedElement problemElement = PsiTreeUtil.getNonStrictParentOfType(elt, PsiFile.class);

      RefElement refElement = context.getRefManager().getReference(problemElement);
      if (refElement == null && problemElement != null) {  // no need to lose collected results
        refElement = GlobalInspectionContextUtil.retrieveRefElement(elt, context);
      }
      return refElement;
    };


  @Override
  public void close(boolean noSuspiciousCodeFound) {
    if (!noSuspiciousCodeFound) {
      if (myView.isRerun()) {
        myViewClosed = true;
        myView = null;
      }
      if (myView == null) {
        return;
      }
    }
    if (myContent != null) {
      final ContentManager contentManager = getContentManager();
      contentManager.removeContent(myContent, true);
    }
    myViewClosed = true;
    myView = null;
    ((InspectionManagerEx)InspectionManager.getInstance(getProject())).closeRunningContext(this);
    myPresentationMap.clear();
    super.close(noSuspiciousCodeFound);
  }

  @Override
  public void cleanup() {
    if (myView != null) {
      myView.setUpdating(false);
    }
    else {
      myPresentationMap.clear();
      super.cleanup();
    }
  }

  public void refreshViews() {
    if (myView != null) {
      myView.getTree().getInspectionTreeModel().reload();
    }
  }

  private @Nullable InspectionToolResultExporter getPresentationOrNull(@NotNull InspectionToolWrapper<?, ?> toolWrapper) {
    return myPresentationMap.get(toolWrapper);
  }

  @Override
  public void codeCleanup(final @NotNull AnalysisScope scope,
                          final @NotNull InspectionProfile profile,
                          final @Nullable String commandName,
                          final @Nullable Runnable postRunnable,
                          final boolean modal,
                          @NotNull Predicate<? super ProblemDescriptor> shouldApplyFix) {
    String title = LangBundle.message("progress.title.inspect.code");
    Task task = modal ? new Task.Modal(getProject(), title, true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        cleanup(scope, profile, postRunnable, commandName, indicator, shouldApplyFix);
      }
    } : new Task.Backgroundable(getProject(), title, true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        cleanup(scope, profile, postRunnable, commandName, indicator, shouldApplyFix);
      }
    };
    ProgressManager.getInstance().run(task);
  }

  private void cleanup(@NotNull AnalysisScope scope,
                       @NotNull InspectionProfile profile,
                       @Nullable Runnable postRunnable,
                       @Nullable String commandName,
                       @NotNull ProgressIndicator progressIndicator,
                       @NotNull Predicate<? super ProblemDescriptor> shouldApplyFix) {
    setCurrentScope(scope);
    final int fileCount = scope.getFileCount();
    progressIndicator.setIndeterminate(false);
    final SearchScope searchScope = ReadAction.compute(scope::toSearchScope);
    final TextRange range;
    if (searchScope instanceof LocalSearchScope) {
      final PsiElement[] elements = ((LocalSearchScope)searchScope).getScope();
      range = elements.length == 1 ? ReadAction.compute(elements[0]::getTextRange) : null;
    }
    else {
      range = null;
    }
    Iterable<Tools> inspectionTools = ContainerUtil.filter(profile.getAllEnabledInspectionTools(getProject()), tools -> {
      assert tools != null;
      return tools.getTool().isCleanupTool();
    });
    boolean includeDoNotShow = includeDoNotShow(profile);
    final RefManagerImpl refManager = (RefManagerImpl)getRefManager();
    refManager.inspectionReadActionStarted();
    List<ProblemDescriptor> descriptors = new ArrayList<>();
    Set<PsiFile> files = new HashSet<>();
    try {
      scope.accept(new PsiElementVisitor() {
        private int myCount;
        @Override
        public void visitFile(@NotNull PsiFile file) {
          progressIndicator.setFraction((double)++myCount / fileCount);
          if (isBinary(file)) return;
          final List<LocalInspectionToolWrapper> lTools = new ArrayList<>();
          for (final Tools tools : inspectionTools) {
            InspectionToolWrapper<?, ?> tool = tools.getEnabledTool(file, includeDoNotShow);
            if (tool instanceof GlobalInspectionToolWrapper) {
              tool = ((GlobalInspectionToolWrapper)tool).getSharedLocalInspectionToolWrapper();
            }
            if (tool != null) {
              lTools.add((LocalInspectionToolWrapper)tool);
              tool.initialize(GlobalInspectionContextImpl.this);
            }
          }

          if (!lTools.isEmpty()) {
            try {
              file.putUserData(InspectionProfileWrapper.CUSTOMIZATION_KEY, p -> new InspectionProfileWrapper(profile, p.getProfileManager()));
              LocalInspectionsPass pass = new LocalInspectionsPass(file, file.getViewProvider().getDocument(), range != null ? range.getStartOffset() : 0,
                                                                   range != null ? range.getEndOffset() : file.getTextLength(), LocalInspectionsPass.EMPTY_PRIORITY_RANGE, true,
                                                                   HighlightInfoProcessor.getEmpty(), true);
              Runnable runnable = () -> pass.doInspectInBatch(GlobalInspectionContextImpl.this, InspectionManager.getInstance(getProject()), lTools);
              ApplicationManager.getApplication().runReadAction(runnable);

              final Set<ProblemDescriptor> localDescriptors = new TreeSet<>(CommonProblemDescriptor.DESCRIPTOR_COMPARATOR);
              for (LocalInspectionToolWrapper tool : lTools) {
                InspectionToolResultExporter toolPresentation = getPresentation(tool);
                for (CommonProblemDescriptor descriptor : toolPresentation.getProblemDescriptors()) {
                  if (descriptor instanceof ProblemDescriptor) {
                    localDescriptors.add((ProblemDescriptor)descriptor);
                  }
                }
              }

              if (searchScope instanceof LocalSearchScope) {
                for (Iterator<ProblemDescriptor> iterator = localDescriptors.iterator(); iterator.hasNext(); ) {
                  final ProblemDescriptor descriptor = iterator.next();
                  final TextRange infoRange = descriptor instanceof ProblemDescriptorBase ? ((ProblemDescriptorBase)descriptor).getTextRange() : null;
                  if (infoRange != null && !((LocalSearchScope)searchScope).containsRange(file, infoRange)) {
                    iterator.remove();
                  }
                }
              }
              if (!localDescriptors.isEmpty()) {
                for (ProblemDescriptor descriptor : localDescriptors) {
                  if (shouldApplyFix.test(descriptor)) {
                    descriptors.add(descriptor);
                  }
                }
                files.add(file);
              }
            }
            finally {
              file.putUserData(InspectionProfileWrapper.CUSTOMIZATION_KEY, null);
              myPresentationMap.clear();
            }
          }
        }
      });
    }
    finally {
      refManager.inspectionReadActionFinished();
    }

    if (files.isEmpty()) {
      GuiUtils.invokeLaterIfNeeded(() -> {
        if (commandName != null) {
          NOTIFICATION_GROUP.createNotification(InspectionsBundle.message("inspection.no.problems.message", scope.getFileCount(), scope.getDisplayName()), MessageType.INFO).notify(getProject());
        }
        if (postRunnable != null) {
          postRunnable.run();
        }
      }, ModalityState.defaultModalityState());
      return;
    }

    Runnable runnable = () -> {
      if (!FileModificationService.getInstance().preparePsiElementsForWrite(files)) return;
      CleanupInspectionUtil.getInstance().applyFixesNoSort(getProject(), "Code Cleanup", descriptors, null, false, searchScope instanceof GlobalSearchScope);
      if (postRunnable != null) {
        postRunnable.run();
      }
    };
    TransactionGuard.submitTransaction(getProject(), runnable);
  }

  private static boolean isBinary(@NotNull PsiFile file) {
    return file instanceof PsiBinaryFile || file.getFileType().isBinary();
  }

  public boolean isViewClosed() {
    return myViewClosed;
  }

  private void addProblemsToView(List<? extends Tools> tools) {
    //noinspection TestOnlyProblems
    if (ApplicationManager.getApplication().isHeadlessEnvironment() && !TESTING_VIEW) {
      return;
    }
    if (myView == null && !ReadAction.compute(() -> InspectionResultsView.hasProblems(tools, this, new InspectionRVContentProviderImpl()))) {
      return;
    }
    initializeViewIfNeeded().doWhenDone(() -> myView.addTools(tools));
  }

  @Override
  public @NotNull InspectionToolPresentation getPresentation(@NotNull InspectionToolWrapper toolWrapper) {
    return myPresentationMap.computeIfAbsent(toolWrapper, __ -> {
      String presentationClass = toolWrapper.myEP == null ? null : toolWrapper.myEP.presentation;
      if (StringUtil.isEmpty(presentationClass)) {
        presentationClass = DefaultInspectionToolPresentation.class.getName();
      }
      try {
        InspectionEP extension = toolWrapper.getExtension();
        ClassLoader classLoader = extension == null ? getClass().getClassLoader() : extension.getPluginDescriptor().getPluginClassLoader();
        Constructor<?> constructor = Class.forName(presentationClass, true, classLoader).getConstructor(InspectionToolWrapper.class, GlobalInspectionContextImpl.class);
        constructor.setAccessible(true);
        return (InspectionToolPresentation)constructor.newInstance(toolWrapper, this);
      }
      catch (Exception e) {
        LOG.error(e);
        throw new RuntimeException(e);
      }
    });
  }
}
