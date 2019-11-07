// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring;

import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter;
import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import com.intellij.ide.DataManager;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.undo.BasicUndoableAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.UnloadedModuleDescription;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.impl.status.StatusBarUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.listeners.RefactoringEventData;
import com.intellij.refactoring.listeners.RefactoringEventListener;
import com.intellij.refactoring.listeners.RefactoringListenerManager;
import com.intellij.refactoring.listeners.impl.RefactoringListenerManagerImpl;
import com.intellij.refactoring.listeners.impl.RefactoringTransaction;
import com.intellij.refactoring.ui.ConflictsDialog;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.GuiUtils;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.usages.*;
import com.intellij.usages.impl.UnknownUsagesInUnloadedModules;
import com.intellij.usages.impl.UsageViewImpl;
import com.intellij.usages.rules.PsiElementUsage;
import com.intellij.util.Processor;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class BaseRefactoringProcessor implements Runnable {
  private static final Logger LOG = Logger.getInstance(BaseRefactoringProcessor.class);
  private static boolean PREVIEW_IN_TESTS = true;

  protected final Project myProject;
  protected final @NotNull SearchScope myRefactoringScope;

  private RefactoringTransaction myTransaction;
  private boolean myIsPreviewUsages;
  protected Runnable myPrepareSuccessfulSwingThreadCallback;
  private UsageView myUsageView;

  protected BaseRefactoringProcessor(@NotNull Project project) {
    this(project, null);
  }

  protected BaseRefactoringProcessor(@NotNull Project project, @Nullable Runnable prepareSuccessfulCallback) {
    this(project, GlobalSearchScope.projectScope(project), prepareSuccessfulCallback);
  }

  protected BaseRefactoringProcessor(@NotNull Project project,
                                     @NotNull SearchScope refactoringScope,
                                     @Nullable Runnable prepareSuccessfulCallback) {
    myProject = project;
    myPrepareSuccessfulSwingThreadCallback = prepareSuccessfulCallback;
    myRefactoringScope = refactoringScope;
  }

  @NotNull
  protected abstract UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages);

  /**
   * Is called inside atomic action.
   */
  @NotNull
  protected abstract UsageInfo[] findUsages();

  /**
   * is called when usage search is re-run.
   *
   * @param elements - refreshed elements that are returned by UsageViewDescriptor.getElements()
   */
  protected void refreshElements(@NotNull PsiElement[] elements) {}

  /**
   * Is called inside atomic action.
   *
   * @param refUsages usages to be filtered
   * @return true if preprocessed successfully
   */
  protected boolean preprocessUsages(@NotNull Ref<UsageInfo[]> refUsages) {
    prepareSuccessful();
    return true;
  }

  /**
   * Is called inside atomic action.
   */
  protected boolean isPreviewUsages(@NotNull UsageInfo[] usages) {
    return myIsPreviewUsages;
  }

  protected boolean isPreviewUsages() {
    return myIsPreviewUsages;
  }

  private Set<UnloadedModuleDescription> computeUnloadedModulesFromUseScope(UsageViewDescriptor descriptor) {
    if (ModuleManager.getInstance(myProject).getUnloadedModuleDescriptions().isEmpty()) {
      //optimization
      return Collections.emptySet();
    }

    Set<UnloadedModuleDescription> unloadedModulesInUseScope = new LinkedHashSet<>();
    for (PsiElement element : descriptor.getElements()) {
      SearchScope useScope = element.getUseScope();
      if (useScope instanceof GlobalSearchScope) {
        unloadedModulesInUseScope.addAll(((GlobalSearchScope)useScope).getUnloadedModulesBelongingToScope());
      }
    }
    return unloadedModulesInUseScope;
  }


  public void setPreviewUsages(boolean isPreviewUsages) {
    myIsPreviewUsages = isPreviewUsages;
  }

  public void setPrepareSuccessfulSwingThreadCallback(Runnable prepareSuccessfulSwingThreadCallback) {
    myPrepareSuccessfulSwingThreadCallback = prepareSuccessfulSwingThreadCallback;
  }

  protected RefactoringTransaction getTransaction() {
    return myTransaction;
  }

  /**
   * Is called in a command and inside atomic action.
   */
  protected abstract void performRefactoring(@NotNull UsageInfo[] usages);

  @NotNull
  protected abstract String getCommandName();

  protected void doRun() {
    if (!PsiDocumentManager.getInstance(myProject).commitAllDocumentsUnderProgress()) {
      return;
    }
    final Ref<UsageInfo[]> refUsages = new Ref<>();
    final Ref<Language> refErrorLanguage = new Ref<>();
    final Ref<Boolean> refProcessCanceled = new Ref<>();
    final Ref<Boolean> anyException = new Ref<>();

    DumbService.getInstance(myProject).completeJustSubmittedTasks();

    final Runnable findUsagesRunnable = () -> {
      try {
        refUsages.set(ReadAction.compute(this::findUsages));
      }
      catch (UnknownReferenceTypeException e) {
        refErrorLanguage.set(e.getElementLanguage());
      }
      catch (ProcessCanceledException e) {
        refProcessCanceled.set(Boolean.TRUE);
      }
      catch (Throwable e) {
        anyException.set(Boolean.TRUE);
        LOG.error(e);
      }
    };

    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(findUsagesRunnable, RefactoringBundle.message("progress.text"),
                                                                           true, myProject)) {
      return;
    }

    if (!refErrorLanguage.isNull()) {
      Messages.showErrorDialog(myProject, RefactoringBundle.message("unsupported.refs.found", refErrorLanguage.get().getDisplayName()), RefactoringBundle.message("error.title"));
      return;
    }
    if (DumbService.isDumb(myProject)) {
      DumbService.getInstance(myProject).showDumbModeNotification("Refactoring is not available until indices are ready");
      return;
    }
    if (!refProcessCanceled.isNull()) {
      Messages.showErrorDialog(myProject, "Index corruption detected. Please retry the refactoring - indexes will be rebuilt automatically", RefactoringBundle.message("error.title"));
      return;
    }

    if (!anyException.isNull()) {
      //do not proceed if find usages fails
      return;
    }
    assert !refUsages.isNull(): "Null usages from processor " + this;
    if (!preprocessUsages(refUsages)) return;
    final UsageInfo[] usages = refUsages.get();
    assert usages != null;
    UsageViewDescriptor descriptor = createUsageViewDescriptor(usages);
    boolean isPreview = isPreviewUsages(usages) || !computeUnloadedModulesFromUseScope(descriptor).isEmpty();
    if (!isPreview) {
      isPreview = !ensureElementsWritable(usages, descriptor) || UsageViewUtil.hasReadOnlyUsages(usages);
      if (isPreview) {
        StatusBarUtil.setStatusBarInfo(myProject, RefactoringBundle.message("readonly.occurences.found"));
      }
    }
    if (isPreview) {
      for (UsageInfo usage : usages) {
        LOG.assertTrue(usage != null, getClass());
      }
      previewRefactoring(usages);
    }
    else {
      execute(usages);
    }
  }

  @TestOnly
  public static <T extends Throwable> void runWithDisabledPreview(ThrowableRunnable<T> runnable) throws T {
    PREVIEW_IN_TESTS = false;
    try {
      runnable.run();
    }
    finally {
      PREVIEW_IN_TESTS = true;
    }
  }

  protected void previewRefactoring(@NotNull UsageInfo[] usages) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      if (!PREVIEW_IN_TESTS) throw new RuntimeException("Unexpected preview in tests: " + StringUtil.join(usages, UsageInfo::toString, ", "));
      ensureElementsWritable(usages, createUsageViewDescriptor(usages));
      execute(usages);
      return;
    }
    final UsageViewDescriptor viewDescriptor = createUsageViewDescriptor(usages);
    final PsiElement[] elements = viewDescriptor.getElements();
    final PsiElement2UsageTargetAdapter[] targets = PsiElement2UsageTargetAdapter.convert(elements);
    Factory<UsageSearcher> factory = () -> new UsageInfoSearcherAdapter() {
      @Override
      public void generate(@NotNull final Processor<Usage> processor) {
        ApplicationManager.getApplication().runReadAction(() -> {
          for (int i = 0; i < elements.length; i++) {
            elements[i] = targets[i].getElement();
          }
          refreshElements(elements);
        });
        processUsages(processor, myProject);
      }

      @NotNull
      @Override
      protected UsageInfo[] findUsages() {
        return BaseRefactoringProcessor.this.findUsages();
      }
    };

    showUsageView(viewDescriptor, factory, usages);
  }

  protected boolean skipNonCodeUsages() {
    return false;
  }

  private boolean ensureElementsWritable(@NotNull final UsageInfo[] usages, @NotNull UsageViewDescriptor descriptor) {
    Set<PsiElement> elements = ContainerUtil.newIdentityTroveSet(); // protect against poorly implemented equality
    for (UsageInfo usage : usages) {
      assert usage != null: "Found null element in usages array";
      if (skipNonCodeUsages() && usage.isNonCodeUsage()) continue;
      PsiElement element = usage.getElement();
      if (element != null) elements.add(element);
    }
    elements.addAll(getElementsToWrite(descriptor));
    return ensureFilesWritable(myProject, elements);
  }

  private static boolean ensureFilesWritable(@NotNull Project project, @NotNull Collection<? extends PsiElement> elements) {
    PsiElement[] psiElements = PsiUtilCore.toPsiElementArray(elements);
    return CommonRefactoringUtil.checkReadOnlyStatus(project, psiElements);
  }

  protected void execute(@NotNull final UsageInfo[] usages) {
    CommandProcessor.getInstance().executeCommand(myProject, () -> {
      Collection<UsageInfo> usageInfos = new LinkedHashSet<>(Arrays.asList(usages));
      doRefactoring(usageInfos);
      if (isGlobalUndoAction()) CommandProcessor.getInstance().markCurrentCommandAsGlobal(myProject);
    }, getCommandName(), null, getUndoConfirmationPolicy());
  }

  protected boolean isGlobalUndoAction() {
    return CommonDataKeys.EDITOR.getData(DataManager.getInstance().getDataContext()) == null;
  }

  @NotNull
  protected UndoConfirmationPolicy getUndoConfirmationPolicy() {
    return UndoConfirmationPolicy.DEFAULT;
  }

  @NotNull
  private static UsageViewPresentation createPresentation(@NotNull UsageViewDescriptor descriptor, @NotNull Usage[] usages) {
    UsageViewPresentation presentation = new UsageViewPresentation();
    presentation.setTabText(RefactoringBundle.message("usageView.tabText"));
    presentation.setTargetsNodeText(descriptor.getProcessedElementsHeader());
    presentation.setShowReadOnlyStatusAsRed(true);
    presentation.setShowCancelButton(true);
    presentation.setUsagesString(RefactoringBundle.message("usageView.usagesText"));
    int codeUsageCount = 0;
    int nonCodeUsageCount = 0;
    int dynamicUsagesCount = 0;
    Set<PsiFile> codeFiles = new HashSet<>();
    Set<PsiFile> nonCodeFiles = new HashSet<>();
    Set<PsiFile> dynamicUsagesCodeFiles = new HashSet<>();

    for (Usage usage : usages) {
      if (usage instanceof PsiElementUsage) {
        final PsiElementUsage elementUsage = (PsiElementUsage)usage;
        final PsiElement element = elementUsage.getElement();
        if (element == null) continue;
        final PsiFile containingFile = element.getContainingFile();
        if (usage instanceof UsageInfo2UsageAdapter && ((UsageInfo2UsageAdapter)usage).getUsageInfo().isDynamicUsage()) {
          dynamicUsagesCount++;
          dynamicUsagesCodeFiles.add(containingFile);
        }
        else if (elementUsage.isNonCodeUsage()) {
          nonCodeUsageCount++;
          nonCodeFiles.add(containingFile);
        }
        else {
          codeUsageCount++;
          codeFiles.add(containingFile);
        }
      }
    }
    codeFiles.remove(null);
    nonCodeFiles.remove(null);
    dynamicUsagesCodeFiles.remove(null);

    String codeReferencesText = descriptor.getCodeReferencesText(codeUsageCount, codeFiles.size());
    presentation.setCodeUsagesString(codeReferencesText);
    final String commentReferencesText = descriptor.getCommentReferencesText(nonCodeUsageCount, nonCodeFiles.size());
    if (commentReferencesText != null) {
      presentation.setNonCodeUsagesString(commentReferencesText);
    }
    presentation.setDynamicUsagesString("Dynamic " + StringUtil.decapitalize(descriptor.getCodeReferencesText(dynamicUsagesCount, dynamicUsagesCodeFiles.size())));
    String generatedCodeString;
    if (codeReferencesText.contains("in code")) {
      generatedCodeString = StringUtil.replace(codeReferencesText, "in code", "in generated code");
    }
    else {
      generatedCodeString = codeReferencesText + " in generated code";
    }
    presentation.setUsagesInGeneratedCodeString(generatedCodeString);
    return presentation;
  }

  /**
   * Processes conflicts (possibly shows UI). In case we're running in unit test mode this method will
   * throw {@link BaseRefactoringProcessor.ConflictsInTestsException} that can be handled inside a test.
   * Thrown exception would contain conflicts' messages.
   *
   * @param project   project
   * @param conflicts map with conflict messages and locations
   * @return true if refactoring could proceed or false if refactoring should be cancelled
   */
  public static boolean processConflicts(@NotNull Project project, @NotNull MultiMap<PsiElement, String> conflicts) {
    if (conflicts.isEmpty()) return true;

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      if (BaseRefactoringProcessor.ConflictsInTestsException.isTestIgnore()) return true;
      throw new BaseRefactoringProcessor.ConflictsInTestsException(conflicts.values());
    }

    ConflictsDialog conflictsDialog = new ConflictsDialog(project, conflicts);
    return conflictsDialog.showAndGet();
  }

  private void showUsageView(@NotNull UsageViewDescriptor viewDescriptor,
                             @NotNull Factory<UsageSearcher> factory,
                             @NotNull UsageInfo[] usageInfos) {
    UsageViewManager viewManager = UsageViewManager.getInstance(myProject);

    final PsiElement[] initialElements = viewDescriptor.getElements();
    final UsageTarget[] targets = PsiElement2UsageTargetAdapter.convert(initialElements);
    final Ref<Usage[]> convertUsagesRef = new Ref<>();
    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
      () -> ApplicationManager.getApplication().runReadAction(
        () -> convertUsagesRef.set(UsageInfo2UsageAdapter.convert(usageInfos))), "Preprocess Usages", true, myProject)) return;

    if (convertUsagesRef.isNull()) return;

    final Usage[] usages = convertUsagesRef.get();

    final UsageViewPresentation presentation = createPresentation(viewDescriptor, usages);
    if (myUsageView == null) {
      myUsageView = viewManager.showUsages(targets, usages, presentation, factory);
      customizeUsagesView(viewDescriptor, myUsageView);
    } else {
      myUsageView.removeUsagesBulk(myUsageView.getUsages());
      ((UsageViewImpl)myUsageView).appendUsagesInBulk(Arrays.asList(usages));
    }
    Set<UnloadedModuleDescription> unloadedModules = computeUnloadedModulesFromUseScope(viewDescriptor);
    if (!unloadedModules.isEmpty()) {
      myUsageView.appendUsage(new UnknownUsagesInUnloadedModules(unloadedModules));
    }
  }

  protected void customizeUsagesView(@NotNull final UsageViewDescriptor viewDescriptor, @NotNull final UsageView usageView) {
    Runnable refactoringRunnable = () -> {
      Set<UsageInfo> usagesToRefactor = UsageViewUtil.getNotExcludedUsageInfos(usageView);
      final UsageInfo[] infos = usagesToRefactor.toArray(UsageInfo.EMPTY_ARRAY);
      TransactionGuard.getInstance().submitTransactionAndWait(() -> {
        if (ensureElementsWritable(infos, viewDescriptor)) {
          execute(infos);
        }
      });
    };

    String canNotMakeString = RefactoringBundle.message("usageView.need.reRun");

    addDoRefactoringAction(usageView, refactoringRunnable, canNotMakeString);
    usageView.setRerunAction(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doRun();
      }
    });
  }

  private void addDoRefactoringAction(@NotNull UsageView usageView, @NotNull Runnable refactoringRunnable, @NotNull String canNotMakeString) {
    usageView.addPerformOperationAction(refactoringRunnable, getCommandName(), canNotMakeString,
                                        RefactoringBundle.message("usageView.doAction"), false);
  }

  private void doRefactoring(@NotNull final Collection<UsageInfo> usageInfoSet) {
   for (Iterator<UsageInfo> iterator = usageInfoSet.iterator(); iterator.hasNext();) {
      UsageInfo usageInfo = iterator.next();
      final PsiElement element = usageInfo.getElement();
      if (element == null || !isToBeChanged(usageInfo)) {
        iterator.remove();
      }
    }

    String commandName = getCommandName();
    LocalHistoryAction action = LocalHistory.getInstance().startAction(commandName);

    final UsageInfo[] writableUsageInfos = usageInfoSet.toArray(UsageInfo.EMPTY_ARRAY);
    try {
      PsiDocumentManager.getInstance(myProject).commitAllDocuments();
      RefactoringListenerManagerImpl listenerManager = (RefactoringListenerManagerImpl)RefactoringListenerManager.getInstance(myProject);
      myTransaction = listenerManager.startTransaction();
      final Map<RefactoringHelper, Object> preparedData = new LinkedHashMap<>();
      final Runnable prepareHelpersRunnable = () -> {
        for (final RefactoringHelper helper : RefactoringHelper.EP_NAME.getExtensionList()) {
          Object operation = ReadAction.compute(() -> helper.prepareOperation(writableUsageInfos));
          preparedData.put(helper, operation);
        }
      };

      ProgressManager.getInstance().runProcessWithProgressSynchronously(prepareHelpersRunnable, "Prepare ...", false, myProject);

      Runnable performRefactoringRunnable = () -> {
        final String refactoringId = getRefactoringId();
        if (refactoringId != null) {
          RefactoringEventData data = getBeforeData();
          if (data != null) {
            data.addUsages(usageInfoSet);
          }
          myProject.getMessageBus().syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).refactoringStarted(refactoringId, data);
        }

        try {
          if (refactoringId != null) {
            UndoableAction action1 = new UndoRefactoringAction(myProject, refactoringId);
            UndoManager.getInstance(myProject).undoableActionPerformed(action1);
          }

          performRefactoring(writableUsageInfos);
        }
        finally {
          if (refactoringId != null) {
            myProject.getMessageBus()
              .syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).refactoringDone(refactoringId, getAfterData(writableUsageInfos));
          }
        }
      };
      ApplicationEx app = ApplicationManagerEx.getApplicationEx();
      if (Registry.is("run.refactorings.under.progress")) {
        app.runWriteActionWithNonCancellableProgressInDispatchThread(commandName, myProject, null, indicator -> performRefactoringRunnable.run());
      }
      else {
        app.runWriteAction(performRefactoringRunnable);
      }

      DumbService.getInstance(myProject).completeJustSubmittedTasks();

      for(Map.Entry<RefactoringHelper, Object> e: preparedData.entrySet()) {
        //noinspection unchecked
        e.getKey().performOperation(myProject, e.getValue());
      }
      myTransaction.commit();
      if (Registry.is("run.refactorings.under.progress")) {
        app.runWriteActionWithNonCancellableProgressInDispatchThread(commandName, myProject, null, indicator -> performPsiSpoilingRefactoring());
      }
      else {
        app.runWriteAction(this::performPsiSpoilingRefactoring);
      }
    }
    finally {
      action.finish();
    }

    int count = writableUsageInfos.length;
    if (count > 0) {
      StatusBarUtil.setStatusBarInfo(myProject, RefactoringBundle.message("statusBar.refactoring.result", count));
    }
    else {
      if (!isPreviewUsages(writableUsageInfos)) {
        StatusBarUtil.setStatusBarInfo(myProject, RefactoringBundle.message("statusBar.noUsages"));
      }
    }
  }

  protected boolean isToBeChanged(@NotNull UsageInfo usageInfo) {
    return usageInfo.isWritable();
  }

  /**
   * Refactorings that spoil PSI (write something directly to documents etc.) should
   * do that in this method.<br>
   * This method is called immediately after
   * <code>{@link #performRefactoring(UsageInfo[])}</code>.
   */
  protected void performPsiSpoilingRefactoring() {
  }

  protected void prepareSuccessful() {
    if (myPrepareSuccessfulSwingThreadCallback != null) {
      // make sure that dialog is closed in swing thread
      try {
        GuiUtils.runOrInvokeAndWait(myPrepareSuccessfulSwingThreadCallback);
      }
      catch (InterruptedException | InvocationTargetException e) {
        LOG.error(e);
      }
    }
  }

  @Override
  public final void run() {
    Runnable runnable = this::doRun;
    if (shouldDisableAccessChecks()) {
      runnable = () -> NonProjectFileWritingAccessProvider.disableChecksDuring(this::doRun);
    }
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      ApplicationManager.getApplication().assertIsDispatchThread();
      runnable.run();
      return;
    }
    if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
      LOG.error("Refactorings should not be started inside write action\n because they start progress inside and any read action from the progress task would cause the deadlock", new Exception());
      DumbService.getInstance(myProject).smartInvokeLater(runnable);
    }
    else {
      runnable.run();
    }
  }

  protected boolean shouldDisableAccessChecks() {
    return false;
  }

  public static class ConflictsInTestsException extends RuntimeException {
    private final Collection<? extends String> messages;

    private static boolean myTestIgnore;

    public ConflictsInTestsException(@NotNull Collection<? extends String> messages) {
      this.messages = messages;
    }

    public static boolean isTestIgnore() {
      return myTestIgnore;
    }

    @TestOnly
    public static <T extends Throwable> void withIgnoredConflicts(@NotNull ThrowableRunnable<T> r) throws T {
      try {
        myTestIgnore = true;
        r.run();
      }
      finally {
        myTestIgnore = false;
      }
    }

    @NotNull
    public Collection<String> getMessages() {
        List<String> result = new ArrayList<>(messages);
        for (int i = 0; i < messages.size(); i++) {
          result.set(i, result.get(i).replaceAll("<[^>]+>", ""));
        }
        return result;
      }

    @Override
    public String getMessage() {
      List<String> result = new ArrayList<>(messages);
      Collections.sort(result);
      return StringUtil.join(result, "\n");
    }
  }

  /**
   * @deprecated use {@link #showConflicts(MultiMap, UsageInfo[])}
   */
  @Deprecated
  protected boolean showConflicts(@NotNull MultiMap<PsiElement, String> conflicts) {
    return showConflicts(conflicts, null);
  }

  protected boolean showConflicts(@NotNull MultiMap<PsiElement, String> conflicts, @Nullable final UsageInfo[] usages) {
    if (!conflicts.isEmpty() && ApplicationManager.getApplication().isUnitTestMode()) {
      if (!ConflictsInTestsException.isTestIgnore()) throw new ConflictsInTestsException(conflicts.values());
      return true;
    }

    if (myPrepareSuccessfulSwingThreadCallback != null && !conflicts.isEmpty()) {
      final String refactoringId = getRefactoringId();
      if (refactoringId != null) {
        RefactoringEventData conflictUsages = new RefactoringEventData();
        conflictUsages.putUserData(RefactoringEventData.CONFLICTS_KEY, conflicts.values());
        myProject.getMessageBus().syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
          .conflictsDetected(refactoringId, conflictUsages);
      }
      final ConflictsDialog conflictsDialog = prepareConflictsDialog(conflicts, usages);
      if (!conflictsDialog.showAndGet()) {
        if (conflictsDialog.isShowConflicts()) prepareSuccessful();
        return false;
      }
    }

    prepareSuccessful();
    return true;
  }

  @NotNull
  protected ConflictsDialog prepareConflictsDialog(@NotNull MultiMap<PsiElement, String> conflicts, @Nullable final UsageInfo[] usages) {
    final ConflictsDialog conflictsDialog = createConflictsDialog(conflicts, usages);
    conflictsDialog.setCommandName(getCommandName());
    return conflictsDialog;
  }

  @Nullable
  protected RefactoringEventData getBeforeData() {
    return null;
  }

  @Nullable
  protected RefactoringEventData getAfterData(@NotNull UsageInfo[] usages) {
    return null;
  }

  @Nullable
  protected String getRefactoringId() {
    return null;
  }

  @NotNull
  protected ConflictsDialog createConflictsDialog(@NotNull MultiMap<PsiElement, String> conflicts, @Nullable final UsageInfo[] usages) {
    return new ConflictsDialog(myProject, conflicts, usages == null ? null : (Runnable)() -> execute(usages), false, true);
  }

  @NotNull
  protected Collection<? extends PsiElement> getElementsToWrite(@NotNull UsageViewDescriptor descriptor) {
    return Arrays.asList(descriptor.getElements());
  }

  public static class UnknownReferenceTypeException extends RuntimeException {
    private final Language myElementLanguage;

    public UnknownReferenceTypeException(@NotNull Language elementLanguage) {
      myElementLanguage = elementLanguage;
    }

    @NotNull
    Language getElementLanguage() {
      return myElementLanguage;
    }
  }

  private static class UndoRefactoringAction extends BasicUndoableAction {
    private final Project myProject;
    private final String myRefactoringId;

    UndoRefactoringAction(@NotNull Project project, @NotNull String refactoringId) {
      myProject = project;
      myRefactoringId = refactoringId;
    }

    @Override
    public void undo() {
      myProject.getMessageBus().syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).undoRefactoring(myRefactoringId);
    }

    @Override
    public void redo() {
    }
  }
}
