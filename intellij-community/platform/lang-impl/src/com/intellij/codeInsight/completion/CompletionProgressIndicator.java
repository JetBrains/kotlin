// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.completion.impl.CompletionSorterImpl;
import com.intellij.codeInsight.editorActions.CompletionAutoPopupHandler;
import com.intellij.codeInsight.hint.EditorHintListener;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.lookup.*;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.concurrency.JobScheduler;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.icons.AllIcons;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ReferenceRange;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.testFramework.TestModeFlags;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.LightweightHint;
import com.intellij.util.Alarm;
import com.intellij.util.ObjectUtils;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Please don't use this class directly from plugins.
 */
@ApiStatus.Internal
public class CompletionProgressIndicator extends ProgressIndicatorBase implements CompletionProcessEx, Disposable {
  private static final Logger LOG = Logger.getInstance(CompletionProgressIndicator.class);
  private final Editor myEditor;
  @NotNull
  private final Caret myCaret;
  @Nullable private CompletionParameters myParameters;
  private final CodeCompletionHandlerBase myHandler;
  private final CompletionLookupArrangerImpl myArranger;
  private final CompletionType myCompletionType;
  private final int myInvocationCount;
  private OffsetsInFile myHostOffsets;
  private final LookupImpl myLookup;
  private final Alarm mySuppressTimeoutAlarm = new Alarm(this);
  private final MergingUpdateQueue myQueue;
  private final Update myUpdate = new Update("update") {
    @Override
    public void run() {
      updateLookup(myIsUpdateSuppressed);
      myQueue.setMergingTimeSpan(ourShowPopupGroupingTime);
    }
  };
  private final Semaphore myFreezeSemaphore = new Semaphore(1);
  private final Semaphore myFinishSemaphore = new Semaphore(1);
  @NotNull private final OffsetMap myOffsetMap;
  private final Set<Pair<Integer, ElementPattern<String>>> myRestartingPrefixConditions = ContainerUtil.newConcurrentSet();
  private final LookupListener myLookupListener = new LookupListener() {
    @Override
    public void lookupCanceled(@NotNull final LookupEvent event) {
      finishCompletionProcess(true);
    }
  };

  private volatile boolean myIsUpdateSuppressed = false;
  private static int ourInsertSingleItemTimeSpan = 300;

  //temp external setters to make Rider autopopup more reactive
  private static int ourShowPopupGroupingTime = 300;
  private static int ourShowPopupAfterFirstItemGroupingTime = 100;

  private volatile int myCount;
  private volatile boolean myHasPsiElements;
  private boolean myLookupUpdated;
  private final PropertyChangeListener myLookupManagerListener;
  private final Queue<Runnable> myAdvertiserChanges = new ConcurrentLinkedQueue<>();
  private final List<CompletionResult> myDelayedMiddleMatches = new ArrayList<>();
  private final int myStartCaret;
  private final CompletionThreadingBase myThreading;
  private final Object myLock = ObjectUtils.sentinel("CompletionProgressIndicator");

  CompletionProgressIndicator(Editor editor, @NotNull Caret caret, int invocationCount,
                              CodeCompletionHandlerBase handler, @NotNull OffsetMap offsetMap, @NotNull OffsetsInFile hostOffsets,
                              boolean hasModifiers, @NotNull LookupImpl lookup) {
    myEditor = editor;
    myCaret = caret;
    myHandler = handler;
    myCompletionType = handler.completionType;
    myInvocationCount = invocationCount;
    myOffsetMap = offsetMap;
    myHostOffsets = hostOffsets;
    myLookup = lookup;
    myStartCaret = myEditor.getCaretModel().getOffset();
    myThreading = ApplicationManager.getApplication().isWriteAccessAllowed() || myHandler.isTestingCompletionQualityMode() ? new SyncCompletion() : new AsyncCompletion();

    myAdvertiserChanges.offer(() -> myLookup.getAdvertiser().clearAdvertisements());

    myArranger = new CompletionLookupArrangerImpl(this);
    myLookup.setArranger(myArranger);

    myLookup.addLookupListener(myLookupListener);
    myLookup.setCalculating(true);

    myLookupManagerListener = evt -> {
      if (evt.getNewValue() != null) {
        LOG.error("An attempt to change the lookup during completion, phase = " + CompletionServiceImpl.getCompletionPhase());
      }
    };
    LookupManager.getInstance(getProject()).addPropertyChangeListener(myLookupManagerListener);

    myQueue = new MergingUpdateQueue("completion lookup progress", ourShowPopupAfterFirstItemGroupingTime, true, myEditor.getContentComponent());

    ApplicationManager.getApplication().assertIsDispatchThread();

    if (hasModifiers && !ApplicationManager.getApplication().isUnitTestMode()) {
      trackModifiers();
    }
  }

  @Override
  public void itemSelected(@Nullable LookupElement lookupItem, char completionChar) {
    boolean dispose = lookupItem == null;
    finishCompletionProcess(dispose);
    if (dispose) return;

    setMergeCommand();

    myHandler.lookupItemSelected(this, lookupItem, completionChar, myLookup.getItems());
  }

  @Override
  @NotNull
  @SuppressWarnings("WeakerAccess")
  public OffsetMap getOffsetMap() {
    return myOffsetMap;
  }

  @NotNull
  @Override
  public OffsetsInFile getHostOffsets() {
    return myHostOffsets;
  }

  void duringCompletion(CompletionInitializationContext initContext, CompletionParameters parameters) {
    PsiUtilCore.ensureValid(parameters.getPosition());
    if (isAutopopupCompletion() && shouldPreselectFirstSuggestion(parameters)) {
      myLookup.setLookupFocusDegree(CodeInsightSettings.getInstance().isSelectAutopopupSuggestionsByChars()
                                    ? LookupFocusDegree.FOCUSED
                                    : LookupFocusDegree.SEMI_FOCUSED);
    }
    addDefaultAdvertisements(parameters);

    ProgressManager.checkCanceled();

    Document document = initContext.getEditor().getDocument();
    if (!initContext.getOffsetMap().wasModified(CompletionInitializationContext.IDENTIFIER_END_OFFSET)) {
      try {
        final int selectionEndOffset = initContext.getSelectionEndOffset();
        final PsiReference reference = TargetElementUtil.findReference(myEditor, selectionEndOffset);
        if (reference != null) {
          final int replacementOffset = findReplacementOffset(selectionEndOffset, reference);
          if (replacementOffset > document.getTextLength()) {
            LOG.error("Invalid replacementOffset: " + replacementOffset + " returned by reference " + reference + " of " + reference.getClass() +
                      "; doc=" + document +
                      "; doc actual=" + (document == initContext.getFile().getViewProvider().getDocument()) +
                      "; doc committed=" + PsiDocumentManager.getInstance(getProject()).isCommitted(document));
          } else {
            initContext.setReplacementOffset(replacementOffset);
          }
        }
      }
      catch (IndexNotReadyException ignored) {
      }
    }

    for (CompletionContributor contributor : CompletionContributor.forLanguageHonorDumbness(initContext.getPositionLanguage(), initContext.getProject())) {
      ProgressManager.checkCanceled();
      contributor.duringCompletion(initContext);
    }
    if (document instanceof DocumentWindow) {
      myHostOffsets = new OffsetsInFile(initContext.getFile(), initContext.getOffsetMap()).toTopLevelFile();
    }
  }


  private void addDefaultAdvertisements(CompletionParameters parameters) {
    if (DumbService.isDumb(getProject())) {
      addAdvertisement("Results might be incomplete while indexing is in progress", AllIcons.General.Warning);
      return;
    }

    String enterShortcut = CompletionUtil.getActionShortcut(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM);
    String tabShortcut = CompletionUtil.getActionShortcut(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_REPLACE);
    addAdvertisement("Press " + enterShortcut + " to insert, " + tabShortcut + " to replace", null);

    advertiseTabReplacement(parameters);
    if (isAutopopupCompletion()) {
      if (shouldPreselectFirstSuggestion(parameters) && !CodeInsightSettings.getInstance().isSelectAutopopupSuggestionsByChars()) {
        advertiseCtrlDot();
      }
      advertiseCtrlArrows();
    }
  }

  private void advertiseTabReplacement(CompletionParameters parameters) {
    if (CompletionUtil.shouldShowFeature(parameters, CodeCompletionFeatures.EDITING_COMPLETION_REPLACE) &&
      myOffsetMap.getOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET) != myOffsetMap.getOffset(CompletionInitializationContext.SELECTION_END_OFFSET)) {
      String shortcut = CompletionUtil.getActionShortcut(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_REPLACE);
      if (StringUtil.isNotEmpty(shortcut)) {
        addAdvertisement("Use " + shortcut + " to overwrite the current identifier with the chosen variant", null);
      }
    }
  }

  private void advertiseCtrlDot() {
    if (FeatureUsageTracker
      .getInstance().isToBeAdvertisedInLookup(CodeCompletionFeatures.EDITING_COMPLETION_FINISH_BY_CONTROL_DOT, getProject())) {
      String dotShortcut = CompletionUtil.getActionShortcut(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_DOT);
      if (StringUtil.isNotEmpty(dotShortcut)) {
        addAdvertisement("Press " + dotShortcut + " to choose the selected (or first) suggestion and insert a dot afterwards", null);
      }
    }
  }

  private void advertiseCtrlArrows() {
    if (!myEditor.isOneLineMode() &&
        FeatureUsageTracker.getInstance()
          .isToBeAdvertisedInLookup(CodeCompletionFeatures.EDITING_COMPLETION_CONTROL_ARROWS, getProject())) {
      String downShortcut = CompletionUtil.getActionShortcut(IdeActions.ACTION_LOOKUP_DOWN);
      String upShortcut = CompletionUtil.getActionShortcut(IdeActions.ACTION_LOOKUP_UP);
      if (StringUtil.isNotEmpty(downShortcut) && StringUtil.isNotEmpty(upShortcut)) {
        addAdvertisement(downShortcut + " and " + upShortcut + " will move caret down and up in the editor", null);
      }
    }
  }

  @Override
  public void dispose() {
  }

  private static int findReplacementOffset(int selectionEndOffset, PsiReference reference) {
    final List<TextRange> ranges = ReferenceRange.getAbsoluteRanges(reference);
    for (TextRange range : ranges) {
      if (range.contains(selectionEndOffset)) {
        return range.getEndOffset();
      }
    }

    return selectionEndOffset;
  }


  void scheduleAdvertising(CompletionParameters parameters) {
    if (myLookup.isAvailableToUser()) {
      return;
    }
    for (CompletionContributor contributor : CompletionContributor.forParameters(parameters)) {
      if (!myLookup.isCalculating() && !myLookup.isVisible()) return;

      String s = contributor.advertise(parameters);
      if (s != null) {
        addAdvertisement(s, null);
      }
    }
  }

  private boolean isOutdated() {
    return CompletionServiceImpl.getCompletionPhase().indicator != this;
  }

  private void trackModifiers() {
    assert !isAutopopupCompletion();

    final JComponent contentComponent = myEditor.getContentComponent();
    contentComponent.addKeyListener(new ModifierTracker(contentComponent));
  }

  void setMergeCommand() {
    CommandProcessor.getInstance().setCurrentCommandGroupId(getCompletionCommandName());
  }

  private String getCompletionCommandName() {
    return "Completion" + hashCode();
  }

  void showLookup() {
    updateLookup(myIsUpdateSuppressed);
  }

  // non-null when running generators and adding elements to lookup
  @Override
  @Nullable
  public CompletionParameters getParameters() {
    return myParameters;
  }

  @Override
  public void setParameters(@NotNull CompletionParameters parameters) {
    myParameters = parameters;
  }

  @Override
  @NotNull
  public LookupImpl getLookup() {
    return myLookup;
  }

  void withSingleUpdate(Runnable action) {
    try {
      myIsUpdateSuppressed = true;
      action.run();
    } finally {
      myIsUpdateSuppressed = false;
      myQueue.queue(myUpdate);
    }
  }

  private void updateLookup(boolean isUpdateSuppressed) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (isOutdated() || !shouldShowLookup() || isUpdateSuppressed) return;

    while (true) {
      Runnable action = myAdvertiserChanges.poll();
      if (action == null) break;
      action.run();
    }

    if (!myLookupUpdated) {
      if (myLookup.getAdvertisements().isEmpty() && !isAutopopupCompletion() && !DumbService.isDumb(getProject())) {
        DefaultCompletionContributor.addDefaultAdvertisements(myLookup, myHasPsiElements);
      }
      myLookup.getAdvertiser().showRandomText();
    }

    boolean justShown = false;
    if (!myLookup.isShown()) {
      if (hideAutopopupIfMeaningless()) {
        return;
      }

      if (!myLookup.showLookup()) {
        return;
      }
      justShown = true;
    }
    myLookupUpdated = true;
    myLookup.refreshUi(true, justShown);
    hideAutopopupIfMeaningless();
    if (justShown) {
      myLookup.ensureSelectionVisible(true);
    }
  }

  private boolean shouldShowLookup() {
    if (isAutopopupCompletion()) {
      if (myCount == 0) {
        return false;
      }
      if (myLookup.isCalculating() && Registry.is("ide.completion.delay.autopopup.until.completed")) {
        return false;
      }
    }
    return true;
  }

  void addItem(CompletionResult item) {
    if (!isRunning()) return;
    ProgressManager.checkCanceled();

    if (!myHandler.isTestingMode()) {
      LOG.assertTrue(!ApplicationManager.getApplication().isDispatchThread());
    }

    LookupElement lookupElement = item.getLookupElement();
    if (!myHasPsiElements && lookupElement.getPsiElement() != null) {
      myHasPsiElements = true;
    }

    boolean forceMiddleMatch = lookupElement.getUserData(CompletionLookupArrangerImpl.FORCE_MIDDLE_MATCH) != null;
    if (forceMiddleMatch) {
      myArranger.associateSorter(lookupElement, (CompletionSorterImpl)item.getSorter());
      addItemToLookup(item);
      return;
    }

    boolean allowMiddleMatches = myCount > CompletionLookupArrangerImpl.MAX_PREFERRED_COUNT * 2;
    if (allowMiddleMatches) {
      addDelayedMiddleMatches();
    }

    myArranger.associateSorter(lookupElement, (CompletionSorterImpl)item.getSorter());
    if (item.isStartMatch() || allowMiddleMatches) {
      addItemToLookup(item);
    } else {
      synchronized (myDelayedMiddleMatches) {
        myDelayedMiddleMatches.add(item);
      }
    }
  }

  private void addItemToLookup(CompletionResult item) {
    if (!myLookup.addItem(item.getLookupElement(), item.getPrefixMatcher())) {
      return;
    }

    myArranger.setLastLookupPrefix(myLookup.getAdditionalPrefix());

    //noinspection NonAtomicOperationOnVolatileField
    myCount++; // invoked from a single thread

    if (myCount == 1) {
      JobScheduler.getScheduler().schedule(myFreezeSemaphore::up, ourInsertSingleItemTimeSpan, TimeUnit.MILLISECONDS);
    }
    myQueue.queue(myUpdate);
  }

  void addDelayedMiddleMatches() {
    ArrayList<CompletionResult> delayed;
    synchronized (myDelayedMiddleMatches) {
      if (myDelayedMiddleMatches.isEmpty()) return;
      delayed = new ArrayList<>(myDelayedMiddleMatches);
      myDelayedMiddleMatches.clear();
    }
    for (CompletionResult item : delayed) {
      ProgressManager.checkCanceled();
      addItemToLookup(item);
    }
  }

  public void closeAndFinish(boolean hideLookup) {
    if (!myLookup.isLookupDisposed()) {
      Lookup lookup = LookupManager.getActiveLookup(myEditor);
      LOG.assertTrue(lookup == myLookup, "lookup changed: " + lookup + "; " + this);
    }
    myLookup.removeLookupListener(myLookupListener);
    finishCompletionProcess(true);
    CompletionServiceImpl.assertPhase(CompletionPhase.NoCompletion.getClass());

    if (hideLookup) {
      myLookup.hideLookup(true);
    }
  }

  private void finishCompletionProcess(boolean disposeOffsetMap) {
    cancel();

    ApplicationManager.getApplication().assertIsDispatchThread();
    Disposer.dispose(myQueue);
    LookupManager.getInstance(getProject()).removePropertyChangeListener(myLookupManagerListener);

    CompletionServiceImpl
      .assertPhase(CompletionPhase.BgCalculation.class, CompletionPhase.ItemsCalculated.class, CompletionPhase.Synchronous.class,
                   CompletionPhase.CommittingDocuments.class);

    CompletionProgressIndicator currentCompletion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
    LOG.assertTrue(currentCompletion == this, currentCompletion + "!=" + this);

    CompletionPhase oldPhase = CompletionServiceImpl.getCompletionPhase();
    if (oldPhase instanceof CompletionPhase.CommittingDocuments) {
      LOG.assertTrue(((CompletionPhase.CommittingDocuments)oldPhase).indicator != null, oldPhase);
      ((CompletionPhase.CommittingDocuments)oldPhase).replaced = true;
    }
    CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
    if (disposeOffsetMap) {
      disposeIndicator();
    }
  }

  void disposeIndicator() {
    synchronized (myLock) {
      Disposer.dispose(this);
    }
  }

  @Override
  public void registerChildDisposable(@NotNull Supplier<? extends Disposable> child) {
    synchronized (myLock) {
      // avoid registering stuff on an indicator being disposed concurrently
      checkCanceled();
      Disposer.register(this, child.get());
    }
  }

  @TestOnly
  public static void cleanupForNextTest() {
    CompletionService completionService = ServiceManager.getServiceIfCreated(CompletionService.class);
    if (!(completionService instanceof CompletionServiceImpl)) {
      return;
    }

    CompletionProgressIndicator currentCompletion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
    if (currentCompletion != null) {
      currentCompletion.finishCompletionProcess(true);
      CompletionServiceImpl.assertPhase(CompletionPhase.NoCompletion.getClass());
    }
    else {
      CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
    }
    StatisticsUpdate.cancelLastCompletionStatisticsUpdate();
  }

  boolean blockingWaitForFinish(int timeoutMs) {
    if (myHandler.isTestingMode() && !TestModeFlags.is(CompletionAutoPopupHandler.ourTestingAutopopup)) {
      if (!myFinishSemaphore.waitFor(100 * 1000)) {
        throw new AssertionError("Too long completion");
      }
      return true;
    }
    if (myFreezeSemaphore.waitFor(timeoutMs)) {
      // the completion is really finished, now we may auto-insert or show lookup
      return !isRunning() && !isCanceled();
    }
    return false;
  }

  @Override
  public void stop() {
    super.stop();

    myQueue.cancelAllUpdates();
    myFreezeSemaphore.up();
    myFinishSemaphore.up();

    GuiUtils.invokeLaterIfNeeded(() -> {
      final CompletionPhase phase = CompletionServiceImpl.getCompletionPhase();
      if (!(phase instanceof CompletionPhase.BgCalculation) || phase.indicator != this) return;

      LOG.assertTrue(!getProject().isDisposed(), "project disposed");

      if (myEditor.isDisposed()) {
        myLookup.hideLookup(false);
        CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
        return;
      }

      if (myEditor instanceof EditorWindow) {
        LOG.assertTrue(((EditorWindow)myEditor).getInjectedFile().isValid(), "injected file !valid");
        LOG.assertTrue(((DocumentWindow)myEditor.getDocument()).isValid(), "docWindow !valid");
      }
      PsiFile file = myLookup.getPsiFile();
      LOG.assertTrue(file == null || file.isValid(), "file !valid");

      myLookup.setCalculating(false);

      if (myCount == 0) {
        myLookup.hideLookup(false);
        if (!isAutopopupCompletion()) {
          final CompletionProgressIndicator current = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
          LOG.assertTrue(current == null, current + "!=" + this);

          handleEmptyLookup(!((CompletionPhase.BgCalculation)phase).modifiersChanged);
        }
      }
      else {
        updateLookup(myIsUpdateSuppressed);
        if (CompletionServiceImpl.getCompletionPhase() != CompletionPhase.NoCompletion) {
          CompletionServiceImpl.setCompletionPhase(new CompletionPhase.ItemsCalculated(this));
        }
      }
    }, myQueue.getModalityState());
  }

  private boolean hideAutopopupIfMeaningless() {
    if (!myLookup.isLookupDisposed() && isAutopopupCompletion() && !myLookup.isSelectionTouched() && !myLookup.isCalculating()) {
      myLookup.refreshUi(true, false);
      final List<LookupElement> items = myLookup.getItems();

      for (LookupElement item : items) {
        if (!isAlreadyInTheEditor(item)) {
          return false;
        }

        if (item.isValid() && item.isWorthShowingInAutoPopup()) {
          return false;
        }
      }

      myLookup.hideLookup(false);
      LOG.assertTrue(CompletionServiceImpl.getCompletionService().getCurrentCompletion() == null);
      CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
      return true;
    }
    return false;
  }

  private boolean isAlreadyInTheEditor(LookupElement item) {
    Editor editor = myLookup.getEditor();
    int start = editor.getCaretModel().getOffset() - myLookup.itemPattern(item).length();
    Document document = editor.getDocument();
    return start >= 0 && StringUtil.startsWith(document.getImmutableCharSequence().subSequence(start, document.getTextLength()),
                                               item.getLookupString());
  }

  void restorePrefix(@NotNull Runnable customRestore) {
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      setMergeCommand();
      customRestore.run();
    });
  }

  int nextInvocationCount(int invocation, boolean reused) {
    return reused ? Math.max(myInvocationCount + 1, 2) : invocation;
  }

  @Override
  @NotNull
  public Editor getEditor() {
    return myEditor;
  }

  @Override
  @NotNull
  public Caret getCaret() {
    return myCaret;
  }

  boolean isRepeatedInvocation(CompletionType completionType, Editor editor) {
    if (completionType != myCompletionType || editor != myEditor) {
      return false;
    }

    if (isAutopopupCompletion() && !myLookup.mayBeNoticed()) {
      return false;
    }

    return true;
  }

  @Override
  public boolean isAutopopupCompletion() {
    return myInvocationCount == 0;
  }

  int getInvocationCount() {
    return myInvocationCount;
  }

  @Override
  @NotNull
  public Project getProject() {
    return ObjectUtils.assertNotNull(myEditor.getProject());
  }

  @Override
  public void addWatchedPrefix(int startOffset, ElementPattern<String> restartCondition) {
    myRestartingPrefixConditions.add(Pair.create(startOffset, restartCondition));
  }

  @Override
  public void prefixUpdated() {
    final int caretOffset = myEditor.getCaretModel().getOffset();
    if (caretOffset < myStartCaret) {
      scheduleRestart();
      myRestartingPrefixConditions.clear();
      return;
    }

    final CharSequence text = myEditor.getDocument().getCharsSequence();
    for (Pair<Integer, ElementPattern<String>> pair : myRestartingPrefixConditions) {
      int start = pair.first;
      if (caretOffset >= start && start >= 0 && caretOffset <= text.length()) {
        final String newPrefix = text.subSequence(start, caretOffset).toString();
        if (pair.second.accepts(newPrefix)) {
          scheduleRestart();
          myRestartingPrefixConditions.clear();
          return;
        }
      }
    }

    hideAutopopupIfMeaningless();
  }

  @Override
  public void scheduleRestart() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (myHandler.isTestingMode() && !TestModeFlags.is(CompletionAutoPopupHandler.ourTestingAutopopup)) {
      closeAndFinish(false);
      PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
      new CodeCompletionHandlerBase(myCompletionType, false, false, true).invokeCompletion(getProject(), myEditor, myInvocationCount);
      return;
    }

    cancel();

    final CompletionProgressIndicator current = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
    if (this != current) {
      LOG.error(current + "!=" + this);
    }

    hideAutopopupIfMeaningless();

    CompletionPhase oldPhase = CompletionServiceImpl.getCompletionPhase();
    if (oldPhase instanceof CompletionPhase.CommittingDocuments) {
      ((CompletionPhase.CommittingDocuments)oldPhase).replaced = true;
    }

    CompletionPhase.CommittingDocuments.scheduleAsyncCompletion(myEditor, myCompletionType, null, getProject(), this);
  }

  @Override
  public String toString() {
    return "CompletionProgressIndicator[count=" +
           myCount +
           ",phase=" +
           CompletionServiceImpl.getCompletionPhase() +
           "]@" +
           System.identityHashCode(this);
  }

  void handleEmptyLookup(boolean awaitSecondInvocation) {
    if (isAutopopupCompletion() && ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    LOG.assertTrue(!isAutopopupCompletion());

    CompletionParameters parameters = getParameters();
    if (myHandler.invokedExplicitly && parameters != null) {
      LightweightHint hint = showErrorHint(getProject(), getEditor(), getNoSuggestionsMessage(parameters));
      if (awaitSecondInvocation) {
        CompletionServiceImpl.setCompletionPhase(new CompletionPhase.NoSuggestionsHint(hint, this));
        return;
      }
    }
    CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
  }

  private String getNoSuggestionsMessage(CompletionParameters parameters) {
    String text = CompletionContributor.forParameters(parameters)
                                       .stream()
                                       .map(c -> c.handleEmptyLookup(parameters, getEditor()))
                                       .filter(StringUtil::isNotEmpty)
                                       .findFirst()
                                       .orElse(LangBundle.message("completion.no.suggestions"));
    return DumbService.isDumb(getProject()) ? text + "; results might be incomplete while indexing is in progress" : text;
  }

  private static LightweightHint showErrorHint(Project project, Editor editor, String text) {
    final LightweightHint[] result = {null};
    final EditorHintListener listener = (project1, hint, flags) -> result[0] = hint;
    final MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(EditorHintListener.TOPIC, listener);
    assert text != null;
    HintManager.getInstance().showInformationHint(editor, StringUtil.escapeXmlEntities(text), HintManager.UNDER);
    connection.disconnect();
    return result[0];
  }

  private static boolean shouldPreselectFirstSuggestion(CompletionParameters parameters) {
    if (Registry.is("ide.completion.lookup.element.preselect.depends.on.context")) {
      for (CompletionPreselectionBehaviourProvider provider : CompletionPreselectionBehaviourProvider.EP_NAME.getExtensionList()) {
        if (!provider.shouldPreselectFirstSuggestion(parameters)) {
          return false;
        }
      }
    }

    return true;
  }

  void runContributors(CompletionInitializationContext initContext) {
    CompletionParameters parameters = Objects.requireNonNull(myParameters);
    myThreading.startThread(ProgressWrapper.wrap(this), ()-> AsyncCompletion.tryReadOrCancel(this, () -> scheduleAdvertising(parameters)));
    WeighingDelegate weigher = myThreading.delegateWeighing(this);

    try {
      calculateItems(initContext, weigher, parameters);
    }
    catch (ProcessCanceledException ignore) {
      cancel(); // some contributor may just throw PCE; if indicator is not canceled everything will hang
    }
    catch (Throwable t) {
      cancel();
      LOG.error(t);
    }
  }

  private void calculateItems(CompletionInitializationContext initContext, WeighingDelegate weigher, CompletionParameters parameters) {
    duringCompletion(initContext, parameters);
    ProgressManager.checkCanceled();

    CompletionService.getCompletionService().performCompletion(parameters, weigher);
    ProgressManager.checkCanceled();

    weigher.waitFor();
    ProgressManager.checkCanceled();
  }

  @NotNull
  CompletionThreadingBase getCompletionThreading() {
    return myThreading;
  }

  @Override
  public void addAdvertisement(@NotNull String text, @Nullable Icon icon) {
    myAdvertiserChanges.offer(() -> myLookup.addAdvertisement(text, icon));

    myQueue.queue(myUpdate);
  }

  @SuppressWarnings("unused") // for Rider
  @TestOnly
  public static void setGroupingTimeSpan(int timeSpan) {
    ourInsertSingleItemTimeSpan = timeSpan;
  }

  @Deprecated
  public static void setAutopopupTriggerTime(int timeSpan) {
    ourShowPopupGroupingTime = timeSpan;
    ourShowPopupAfterFirstItemGroupingTime = timeSpan;
  }

  void makeSureLookupIsShown(int timeout) {
    mySuppressTimeoutAlarm.addRequest(this::showIfSuppressed, timeout);
  }

  private void showIfSuppressed() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    if(myLookup.isShown())
      return;

    updateLookup(false);
  }

  private static class ModifierTracker extends KeyAdapter {
    private final JComponent myContentComponent;

    ModifierTracker(JComponent contentComponent) {
      myContentComponent = contentComponent;
    }

    @Override
    public void keyPressed(KeyEvent e) {
      processModifier(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
      processModifier(e);
    }

    private void processModifier(KeyEvent e) {
      final int code = e.getKeyCode();
      if (code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_META || code == KeyEvent.VK_ALT || code == KeyEvent.VK_SHIFT) {
        myContentComponent.removeKeyListener(this);
        final CompletionPhase phase = CompletionServiceImpl.getCompletionPhase();
        if (phase instanceof CompletionPhase.BgCalculation) {
          ((CompletionPhase.BgCalculation)phase).modifiersChanged = true;
        }
        else if (phase instanceof CompletionPhase.InsertedSingleItem) {
          CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
        }
      }
    }
  }
}
