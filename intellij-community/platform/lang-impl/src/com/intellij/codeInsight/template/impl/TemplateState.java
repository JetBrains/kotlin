// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.lookup.*;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.codeInsight.template.*;
import com.intellij.codeInsight.template.macro.TemplateCompletionProcessor;
import com.intellij.diagnostic.AttachmentFactory;
import com.intellij.idea.ActionsBundle;
import com.intellij.lang.LangBundle;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.BasicUndoableAction;
import com.intellij.openapi.command.undo.DocumentReference;
import com.intellij.openapi.command.undo.DocumentReferenceManager;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.ImaginaryEditor;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.impl.text.AsyncEditorLoader;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.source.codeStyle.CodeStyleManagerImpl;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.rename.inplace.InplaceRefactoring;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class TemplateState implements Disposable {
  private static final Logger LOG = Logger.getInstance(TemplateState.class);
  private Project myProject;
  private Editor myEditor;

  private TemplateImpl myTemplate;
  private TemplateImpl myPrevTemplate;
  private TemplateSegments mySegments;
  private Map<String, String> myPredefinedVariableValues;

  private RangeMarker myTemplateRange;
  private final List<RangeHighlighter> myTabStopHighlighters = new ArrayList<>();
  private int myCurrentVariableNumber = -1;
  private int myCurrentSegmentNumber = -1;
  private boolean ourLookupShown;

  private boolean myDocumentChangesTerminateTemplate = true;
  private boolean myDocumentChanged;

  @Nullable private LookupListener myLookupListener;

  private final List<TemplateEditingListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private DocumentListener myEditorDocumentListener;
  private final Map myProperties = new HashMap();
  private boolean myTemplateIndented;
  private Document myDocument;
  @Nullable private PairProcessor<? super String, ? super String> myProcessor;
  private boolean mySelectionCalculated;
  private boolean myStarted;

  TemplateState(@NotNull Project project, @NotNull final Editor editor) {
    myProject = project;
    myEditor = editor;
    myDocument = myEditor.getDocument();
  }

  private void initListeners() {
    if (isDisposed()) return;
    myEditorDocumentListener = new DocumentListener() {
      @Override
      public void beforeDocumentChange(@NotNull DocumentEvent e) {
        if (CommandProcessor.getInstance().getCurrentCommand() != null && !UndoManager.getInstance(myProject).isUndoOrRedoInProgress()) {
          myDocumentChanged = true;
        }
      }
    };
    myLookupListener = new LookupListener() {
      @Override
      public void itemSelected(@NotNull LookupEvent event) {
        if (isCaretOutsideCurrentSegment(null)) {
          if (isCaretInsideOrBeforeNextVariable()) {
            nextTab();
          }
          else {
            gotoEnd(true);
          }
        }
      }
    };
    LookupManager.getInstance(myProject).addPropertyChangeListener(evt -> {
      if (LookupManager.PROP_ACTIVE_LOOKUP.equals(evt.getPropertyName())) {
        Lookup lookup = (Lookup)evt.getNewValue();
        if (lookup != null) {
          lookup.addLookupListener(myLookupListener);
        }
      }
    }, this);

    if (myEditor != null) {
      installCaretListener(myEditor);
    }
    myDocument.addDocumentListener(myEditorDocumentListener, this);
    myProject.getMessageBus().connect(this).subscribe(CommandListener.TOPIC, new CommandListener() {
      boolean started;

      @Override
      public void commandStarted(@NotNull CommandEvent event) {
        if (!UndoManager.getInstance(myProject).isUndoOrRedoInProgress()) {
          myDocumentChangesTerminateTemplate = isCaretOutsideCurrentSegment(event.getCommandName());
          started = true;
        }
      }

      @Override
      public void beforeCommandFinished(@NotNull CommandEvent event) {
        if (started && !isDisposed() && !UndoManager.getInstance(myProject).isUndoOrRedoInProgress()) {
          Runnable runnable = () -> afterChangedUpdate();
          final LookupImpl lookup = myEditor != null ? (LookupImpl)LookupManager.getActiveLookup(myEditor) : null;
          if (lookup != null) {
            lookup.performGuardedChange(runnable);
          }
          else {
            runnable.run();
          }
        }
      }
    });
  }

  private void installCaretListener(@NotNull Editor editor) {
    CaretListener listener = new CaretListener() {
      @Override
      public void caretAdded(@NotNull CaretEvent e) {
        if (!isInteractiveModeSupported()) {
          finishTemplate(false);
        }
      }

      @Override
      public void caretRemoved(@NotNull CaretEvent e) {
        if (!isInteractiveModeSupported()) {
          finishTemplate(false);
        }
      }
    };

    editor.getCaretModel().addCaretListener(listener, this);
  }

  private boolean isCaretInsideOrBeforeNextVariable() {
    if (myEditor != null && myCurrentVariableNumber >= 0) {
      int nextVar = getNextVariableNumber(myCurrentVariableNumber);
      TextRange nextVarRange = nextVar < 0 ? null : getVariableRange(myTemplate.getVariableNameAt(nextVar));
      if (nextVarRange == null) return false;

      int caretOffset = myEditor.getCaretModel().getOffset();
      if (nextVarRange.containsOffset(caretOffset)) return true;

      TextRange currentVarRange = getVariableRange(myTemplate.getVariableNameAt(myCurrentVariableNumber));
      return currentVarRange != null && currentVarRange.getEndOffset() < caretOffset && caretOffset < nextVarRange.getStartOffset();
    }
    return false;
  }

  private boolean isCaretOutsideCurrentSegment(String commandName) {
    if (myEditor != null && myCurrentSegmentNumber >= 0) {
      final int offset = myEditor.getCaretModel().getOffset();
      boolean hasSelection = myEditor.getSelectionModel().hasSelection();

      final int segmentStart = mySegments.getSegmentStart(myCurrentSegmentNumber);
      if (offset < segmentStart ||
          !hasSelection && offset == segmentStart && ActionsBundle.actionText(IdeActions.ACTION_EDITOR_BACKSPACE).equals(commandName)) return true;

      final int segmentEnd = mySegments.getSegmentEnd(myCurrentSegmentNumber);
      if (offset > segmentEnd ||
          !hasSelection && offset == segmentEnd && ActionsBundle.actionText(IdeActions.ACTION_EDITOR_DELETE).equals(commandName)) return true;
    }
    return false;
  }

  private boolean isInteractiveModeSupported() {
    return myEditor != null && myEditor.getCaretModel().getCaretCount() <= 1 && !(myEditor instanceof ImaginaryEditor);
  }

  @Override
  public synchronized void dispose() {
    if (myLookupListener != null) {
      final LookupImpl lookup = myEditor != null ? (LookupImpl)LookupManager.getActiveLookup(myEditor) : null;
      if (lookup != null) {
        lookup.removeLookupListener(myLookupListener);
      }
      myLookupListener = null;
    }

    myEditorDocumentListener = null;

    myProcessor = null;
    myProperties.clear();
    myListeners.clear();

    //Avoid the leak of the editor
    releaseAll();
    myDocument = null;
  }

  public boolean isToProcessTab() {
    if (isCaretOutsideCurrentSegment(null)) {
      return false;
    }
    if (ourLookupShown) {
      final LookupImpl lookup = (LookupImpl)LookupManager.getActiveLookup(myEditor);
      if (lookup != null && !lookup.isFocused()) {
        return true;
      }
    }

    return !ourLookupShown;
  }

  private void setCurrentVariableNumber(int variableNumber) {
    myCurrentVariableNumber = variableNumber;
    final boolean isFinished = isFinished();
    if (myDocument != null) {
      ((DocumentEx)myDocument).setStripTrailingSpacesEnabled(isFinished);
    }
    myCurrentSegmentNumber = isFinished ? -1 : getCurrentSegmentNumber();
  }

  @Nullable
  public TextResult getVariableValue(@NotNull String variableName) {
    if (variableName.equals(TemplateImpl.SELECTION)) {
      return new TextResult(StringUtil.notNullize(getSelectionBeforeTemplate()));
    }
    if (variableName.equals(TemplateImpl.END)) {
      return new TextResult("");
    }
    if (myPredefinedVariableValues != null) {
      String text = myPredefinedVariableValues.get(variableName);
      if (text != null) {
        return new TextResult(text);
      }
    }
    int segmentNumber = myTemplate.getVariableSegmentNumber(variableName);
    if (segmentNumber < 0 || mySegments.getSegmentsCount() <= segmentNumber) {
      return null;
    }
    CharSequence text = myDocument.getImmutableCharSequence();
    int start = mySegments.getSegmentStart(segmentNumber);
    int end = mySegments.getSegmentEnd(segmentNumber);
    int length = text.length();
    if (start > length || end > length) {
      return null;
    }
    return new TextResult(text.subSequence(start, end).toString());
  }

  @Nullable
  private String getSelectionBeforeTemplate() {
    return (String)getProperties().get(ExpressionContext.SELECTION);
  }

  @Nullable
  public TextRange getCurrentVariableRange() {
    int number = getCurrentSegmentNumber();
    if (number == -1) return null;
    return new TextRange(mySegments.getSegmentStart(number), mySegments.getSegmentEnd(number));
  }

  @Nullable
  public TextRange getVariableRange(String variableName) {
    int segment = myTemplate.getVariableSegmentNumber(variableName);
    if (segment < 0) return null;

    return new TextRange(mySegments.getSegmentStart(segment), mySegments.getSegmentEnd(segment));
  }

  public int getSegmentsCount() {
    return mySegments.getSegmentsCount();
  }

  public TextRange getSegmentRange(int segment) {
    return new TextRange(mySegments.getSegmentStart(segment), mySegments.getSegmentEnd(segment));
  }

  public boolean isFinished() {
    return myCurrentVariableNumber < 0;
  }

  private void releaseAll() {
    if (mySegments != null) {
      mySegments.removeAll();
      mySegments = null;
    }
    if (myTemplateRange != null) {
      myTemplateRange.dispose();
      myTemplateRange = null;
    }
    myPrevTemplate = myTemplate;
    myTemplate = null;
    myProject = null;
    releaseEditor();
  }

  private void releaseEditor() {
    if (myEditor != null) {
      for (RangeHighlighter segmentHighlighter : myTabStopHighlighters) {
        segmentHighlighter.dispose();
      }
      myTabStopHighlighters.clear();
      myEditor = null;
    }
  }

  void start(@NotNull TemplateImpl template,
             @Nullable PairProcessor<? super String, ? super String> processor,
             @Nullable Map<String, String> predefinedVarValues) {
    LOG.assertTrue(!myStarted, "Already started");
    myStarted = true;

    PsiFile file = getPsiFile();
    myTemplate = template;
    myProcessor = processor;

    MyBasicUndoableAction undoableAction = new MyBasicUndoableAction(this, myDocument);
    UndoManager.getInstance(myProject).undoableActionPerformed(undoableAction);
    Disposer.register(this, undoableAction);

    myTemplateIndented = false;
    myCurrentVariableNumber = -1;
    mySegments = new TemplateSegments(myEditor);
    myPrevTemplate = myTemplate;

    myPredefinedVariableValues = predefinedVarValues;

    if (myTemplate.isInline()) {
      int caretOffset = myEditor.getCaretModel().getOffset();
      myTemplateRange = myDocument.createRangeMarker(caretOffset, caretOffset + myTemplate.getTemplateText().length());
    }
    else {
      preprocessTemplate(file, myEditor.getCaretModel().getOffset(), myTemplate.getTemplateText());
      int caretOffset = myEditor.getCaretModel().getOffset();
      myTemplateRange = myDocument.createRangeMarker(caretOffset, caretOffset);
    }
    myTemplateRange.setGreedyToLeft(true);
    myTemplateRange.setGreedyToRight(true);

    LiveTemplateRunLogger.log(myProject, template, file.getLanguage());

    processAllExpressions(myTemplate);
  }

  private void preprocessTemplate(final PsiFile file, int caretOffset, final String textToInsert) {
    for (TemplatePreprocessor preprocessor : TemplatePreprocessor.EP_NAME.getExtensionList()) {
      preprocessor.preprocessTemplate(myEditor, file, caretOffset, textToInsert, myTemplate.getTemplateText());
    }
  }

  private void processAllExpressions(@NotNull final TemplateImpl template) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      if (!template.isInline()) myDocument.insertString(myTemplateRange.getStartOffset(), template.getTemplateText());
      for (int i = 0; i < template.getSegmentsCount(); i++) {
        int segmentOffset = myTemplateRange.getStartOffset() + template.getSegmentOffset(i);
        mySegments.addSegment(segmentOffset, segmentOffset);
      }

      LOG.assertTrue(myTemplateRange.isValid(), getRangesDebugInfo());
      calcResults(false);
      LOG.assertTrue(myTemplateRange.isValid(), getRangesDebugInfo());
      calcResults(false);  //Fixed SCR #[vk500] : all variables should be recalced twice on start.
      LOG.assertTrue(myTemplateRange.isValid(), getRangesDebugInfo());
      doReformat();

      int nextVariableNumber = getNextVariableNumber(-1);

      if (nextVariableNumber >= 0) {
        fireWaitingForInput();
      }

      if (nextVariableNumber == -1) {
        finishTemplate(false);
      }
      else {
        setCurrentVariableNumber(nextVariableNumber);
        if (isInteractiveModeSupported()) {
          initTabStopHighlighters();
          initListeners();
        }
        focusCurrentExpression();
        fireCurrentVariableChanged(-1);
        if (!isInteractiveModeSupported()) {
          finishTemplate(false);
        }
      }
    });
  }

  private String getRangesDebugInfo() {
    return myTemplateRange + "\ntemplateKey: " + myTemplate.getKey() + "\ntemplateText: " + myTemplate.getTemplateText() +
           "\ntemplateString: " + myTemplate;
  }

  private void doReformat() {
    final Runnable action = () -> {
      IntArrayList indices = initEmptyVariables();
      mySegments.setSegmentsGreedy(false);
      LOG.assertTrue(myTemplateRange.isValid(),
                     "template key: " + myTemplate.getKey() + "; " +
                     "template text" + myTemplate.getTemplateText() + "; " +
                     "variable number: " + getCurrentVariableNumber());
      reformat();
      mySegments.setSegmentsGreedy(true);
      restoreEmptyVariables(indices);
    };
    ApplicationManager.getApplication().runWriteAction(action);
  }

  public void setSegmentsGreedy(boolean greedy) {
    mySegments.setSegmentsGreedy(greedy);
  }

  public void setTabStopHighlightersGreedy(boolean greedy) {
    for (RangeHighlighter highlighter : myTabStopHighlighters) {
      highlighter.setGreedyToLeft(greedy);
      highlighter.setGreedyToRight(greedy);
    }
  }

  private void shortenReferences() {
    ApplicationManager.getApplication().runWriteAction(() -> {
      final PsiFile file = getPsiFile();
      if (file != null) {
        IntArrayList indices = initEmptyVariables();
        mySegments.setSegmentsGreedy(false);
        for (TemplateOptionalProcessor processor : TemplateOptionalProcessor.EP_NAME.getExtensionList()) {
          processor.processText(myProject, myTemplate, myDocument, myTemplateRange, myEditor);
        }
        mySegments.setSegmentsGreedy(true);
        restoreEmptyVariables(indices);
      }
    });
  }

  private void afterChangedUpdate() {
    if (isFinished()) return;
    LOG.assertTrue(myTemplate != null, presentTemplate(myPrevTemplate));
    if (myDocumentChanged) {
      if (myDocumentChangesTerminateTemplate || mySegments.isInvalid()) {
        cancelTemplate();
      }
      else {
        calcResults(true);
      }
      myDocumentChanged = false;
    }
  }

  private static String presentTemplate(@Nullable TemplateImpl template) {
    if (template == null) {
      return "no template";
    }

    String message = StringUtil.notNullize(template.getKey());
    message += "\n\nTemplate#name: " + StringUtil.notNullize(template.toString());
    message += "\n\nTemplate#string: " + StringUtil.notNullize(template.getString());
    message += "\n\nTemplate#text: " + StringUtil.notNullize(template.getTemplateText());
    return message;
  }

  private String getExpressionString(int index) {
    CharSequence text = myDocument.getCharsSequence();

    if (!mySegments.isValid(index)) return "";

    int start = mySegments.getSegmentStart(index);
    int end = mySegments.getSegmentEnd(index);

    return text.subSequence(start, end).toString();
  }

  private int getCurrentSegmentNumber() {
    int varNumber = myCurrentVariableNumber;
    if (varNumber == -1) {
      return -1;
    }
    String variableName = myTemplate.getVariableNameAt(varNumber);
    int segmentNumber = myTemplate.getVariableSegmentNumber(variableName);
    if (segmentNumber < 0) {
      Throwable trace = myTemplate.getBuildingTemplateTrace();
      LOG.error("No segment for variable: var=" + varNumber + "; name=" + variableName + "; " + presentTemplate(myTemplate) +
                "; offset: " + myEditor.getCaretModel().getOffset(), AttachmentFactory.createAttachment(myDocument),
                new Attachment("trace.txt", trace != null ? ExceptionUtil.getThrowableText(trace) : "<empty>"));
    }
    return segmentNumber;
  }

  private void focusCurrentExpression() {
    if (isFinished() || isDisposed()) {
      return;
    }

    PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);

    final int currentSegmentNumber = getCurrentSegmentNumber();

    lockSegmentAtTheSameOffsetIfAny();

    if (currentSegmentNumber < 0) return;
    final int start = mySegments.getSegmentStart(currentSegmentNumber);
    final int end = mySegments.getSegmentEnd(currentSegmentNumber);
    if (end >= 0) {
      myEditor.getCaretModel().moveToOffset(end);
      myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
      myEditor.getSelectionModel().removeSelection();
      myEditor.getSelectionModel().setSelection(start, end);
    }

    DumbService.getInstance(myProject).withAlternativeResolveEnabled(() -> {
      Expression expressionNode = getCurrentExpression();
      List<TemplateExpressionLookupElement> lookupItems = getCurrentExpressionLookupItems();
      final PsiFile psiFile = getPsiFile();
      if (!lookupItems.isEmpty()) {
        if (((TemplateManagerImpl)TemplateManager.getInstance(myProject)).shouldSkipInTests()) {
          insertSingleItem(lookupItems);
        }
        else {
          for (LookupElement lookupItem : lookupItems) {
            assert lookupItem != null : expressionNode;
          }

          AsyncEditorLoader.performWhenLoaded(myEditor, () ->
            runLookup(lookupItems, expressionNode.getAdvertisingText(), expressionNode.getLookupFocusDegree()));
        }
      }
      else {
        try {
          Result result = expressionNode.calculateResult(getCurrentExpressionContext());
          if (result != null) {
            result.handleFocused(psiFile, myDocument, mySegments.getSegmentStart(currentSegmentNumber),
                                 mySegments.getSegmentEnd(currentSegmentNumber));
          }
        }
        catch (IndexNotReadyException ignore) {
        }
      }
    });
    focusCurrentHighlighter(true);
  }

  @Nullable
  PsiFile getPsiFile() {
    return !isDisposed() ? PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument) : null;
  }

  private void insertSingleItem(List<TemplateExpressionLookupElement> lookupItems) {
    TemplateExpressionLookupElement first = lookupItems.get(0);
    EditorModificationUtil.insertStringAtCaret(myEditor, first.getLookupString());
    first.handleTemplateInsert(lookupItems, Lookup.AUTO_INSERT_SELECT_CHAR);
  }

  @NotNull
  private List<TemplateExpressionLookupElement> getCurrentExpressionLookupItems() {
    LookupElement[] elements = null;
    try {
      elements = getCurrentExpression().calculateLookupItems(getCurrentExpressionContext());
    }
    catch (IndexNotReadyException ignored) { }
    if (elements == null) return Collections.emptyList();

    List<TemplateExpressionLookupElement> result = new ArrayList<>();
    for (int i = 0; i < elements.length; i++) {
      result.add(new TemplateExpressionLookupElement(this, elements[i], i));
    }
    return result;
  }

  public ExpressionContext getCurrentExpressionContext() {
    return createExpressionContext(mySegments.getSegmentStart(getCurrentSegmentNumber()));
  }

  public ExpressionContext getExpressionContextForSegment(int segmentNumber) {
    return createExpressionContext(mySegments.getSegmentStart(segmentNumber));
  }

  @NotNull
  private Expression getCurrentExpression() {
    return myTemplate.getExpressionAt(myCurrentVariableNumber);
  }

  private void runLookup(final List<TemplateExpressionLookupElement> lookupItems,
                         @Nullable String advertisingText,
                         @NotNull LookupFocusDegree lookupFocusDegree) {
    if (isDisposed()) return;

    final LookupManager lookupManager = LookupManager.getInstance(myProject);

    final LookupImpl lookup = (LookupImpl)lookupManager.showLookup(myEditor, lookupItems.toArray(LookupElement.EMPTY_ARRAY));
    if (lookup == null) return;

    if (CodeInsightSettings.getInstance().AUTO_POPUP_COMPLETION_LOOKUP && myEditor.getUserData(InplaceRefactoring.INPLACE_RENAMER) == null) {
      lookup.setStartCompletionWhenNothingMatches(true);
    }

    if (advertisingText != null) {
      lookup.addAdvertisement(advertisingText, null);
    }
    else {
      ActionManager am = ActionManager.getInstance();
      String enterShortcut = KeymapUtil.getFirstKeyboardShortcutText(am.getAction(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM));
      String tabShortcut = KeymapUtil.getFirstKeyboardShortcutText(am.getAction(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_REPLACE));
      lookup.addAdvertisement(LangBundle.message("popup.advertisement.press.or.to.replace", enterShortcut, tabShortcut), null);
    }
    lookup.setLookupFocusDegree(lookupFocusDegree);
    lookup.refreshUi(true, true);
    ourLookupShown = true;
    lookup.addLookupListener(new LookupListener() {
      @Override
      public void lookupCanceled(@NotNull LookupEvent event) {
        lookup.removeLookupListener(this);
        ourLookupShown = false;
      }

      @Override
      public void itemSelected(@NotNull LookupEvent event) {
        lookup.removeLookupListener(this);
        if (isFinished()) return;
        ourLookupShown = false;

        LookupElement item = event.getItem();
        if (item instanceof TemplateExpressionLookupElement) {
          ((TemplateExpressionLookupElement)item).handleTemplateInsert(lookupItems, event.getCompletionChar());
        }
      }
    });
  }

  private void unblockDocument() {
    PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
    PsiDocumentManager.getInstance(myProject).doPostponedOperationsAndUnblockDocument(myDocument);
  }

  // Hours spent fixing code : 3.5
  void calcResults(final boolean isQuick) {
    if (mySegments.isInvalid()) {
      gotoEnd(true);
    }

    if (myProcessor != null && myCurrentVariableNumber >= 0) {
      final String variableName = myTemplate.getVariableNameAt(myCurrentVariableNumber);
      final TextResult value = getVariableValue(variableName);
      if (value != null && !value.getText().isEmpty()) {
        if (!myProcessor.process(variableName, value.getText())) {
          finishTemplate(false); // nextTab(); ?
          return;
        }
      }
    }

    fixOverlappedSegments(myCurrentSegmentNumber);

    WriteCommandAction.runWriteCommandAction(myProject, null, null, () -> {
      if (isDisposed()) {
        return;
      }
      BitSet calcedSegments = new BitSet();
      int maxAttempts = (myTemplate.getVariableCount() + 1) * 3;

      do {
        maxAttempts--;
        calcedSegments.clear();
        for (int i = myCurrentVariableNumber + 1; i < myTemplate.getVariableCount(); i++) {
          String variableName = myTemplate.getVariableNameAt(i);
          final int segmentNumber = myTemplate.getVariableSegmentNumber(variableName);
          if (segmentNumber < 0) continue;
          final Expression expression = myTemplate.getExpressionAt(i);
          final Expression defaultValue = myTemplate.getDefaultValueAt(i);
          String oldValue = getVariableValueText(variableName);
          DumbService.getInstance(myProject).withAlternativeResolveEnabled(
            () -> recalcSegment(segmentNumber, isQuick, expression, defaultValue));
          final TextResult value = getVariableValue(variableName);
          assert value != null : "name=" + variableName + "\ntext=" + myTemplate.getTemplateText();
          String newValue = value.getText();
          if (!newValue.equals(oldValue)) {
            calcedSegments.set(segmentNumber);
          }
        }

        List<TemplateDocumentChange> changes = new ArrayList<>();
        boolean selectionCalculated = false;
        for (int i = 0; i < myTemplate.getSegmentsCount(); i++) {
          if (!calcedSegments.get(i)) {
            String variableName = myTemplate.getSegmentName(i);
            if (variableName.equals(TemplateImpl.SELECTION)) {
              if (mySelectionCalculated) {
                continue;
              }
              selectionCalculated = true;
            }
            if (TemplateImpl.END.equals(variableName)) continue; // No need to update end since it can be placed over some other variable
            String newValue = getVariableValueText(variableName);
            int start = mySegments.getSegmentStart(i);
            int end = mySegments.getSegmentEnd(i);
            changes.add(new TemplateDocumentChange(newValue, start, end, i));
          }
        }
        executeChanges(changes);
        if (selectionCalculated) {
          mySelectionCalculated = true;
        }
      }
      while (!calcedSegments.isEmpty() && maxAttempts >= 0);
    });
  }

  private static final class TemplateDocumentChange {
    public final String newValue;
    public final int startOffset;
    public final int endOffset;
    public final int segmentNumber;

    private TemplateDocumentChange(String newValue, int startOffset, int endOffset, int segmentNumber) {
      this.newValue = newValue;
      this.startOffset = startOffset;
      this.endOffset = endOffset;
      this.segmentNumber = segmentNumber;
    }
  }

  private void executeChanges(@NotNull List<TemplateDocumentChange> changes) {
    if (isDisposed() || changes.isEmpty()) {
      return;
    }
    if (changes.size() > 1) {
      ContainerUtil.sort(changes, (o1, o2) -> {
        int startDiff = o2.startOffset - o1.startOffset;
        return startDiff != 0 ? startDiff : o2.segmentNumber - o1.segmentNumber;
      });
    }
    DocumentUtil.executeInBulk(myDocument, true, () -> {
      for (TemplateDocumentChange change : changes) {
        replaceString(change.newValue, change.startOffset, change.endOffset, change.segmentNumber);
      }
    });
  }

  /**
   * Must be invoked on every segment change in order to avoid overlapping editing segment with its neighbours
   */
  private void fixOverlappedSegments(int currentSegment) {
    if (currentSegment >= 0) {
      int currentSegmentStart = mySegments.getSegmentStart(currentSegment);
      int currentSegmentEnd = mySegments.getSegmentEnd(currentSegment);
      for (int i = 0; i < mySegments.getSegmentsCount(); i++) {
        if (i > currentSegment) {
          final int startOffset = mySegments.getSegmentStart(i);
          if (currentSegmentStart <= startOffset && startOffset < currentSegmentEnd) {
            mySegments.replaceSegmentAt(i, currentSegmentEnd, Math.max(mySegments.getSegmentEnd(i), currentSegmentEnd), true);
          }
        }
        else if (i < currentSegment) {
          final int endOffset = mySegments.getSegmentEnd(i);
          if (currentSegmentStart < endOffset && endOffset <= currentSegmentEnd) {
            mySegments.replaceSegmentAt(i, Math.min(mySegments.getSegmentStart(i), currentSegmentStart), currentSegmentStart, true);
          }
        }
      }
    }
  }

  @NotNull
  private String getVariableValueText(String variableName) {
    TextResult value = getVariableValue(variableName);
    return value != null ? value.getText() : "";
  }

  private void recalcSegment(int segmentNumber, boolean isQuick, Expression expressionNode, Expression defaultValue) {
    if (isDisposed()) return;
    String oldValue = getExpressionString(segmentNumber);
    int start = mySegments.getSegmentStart(segmentNumber);
    int end = mySegments.getSegmentEnd(segmentNumber);
    boolean commitDocument = !isQuick || expressionNode.requiresCommittedPSI();

    if(commitDocument) {
      PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
    }
    PsiFile psiFile = getPsiFile();
    PsiElement element = psiFile != null ? psiFile.findElementAt(start) : null;
    if (element != null && commitDocument) {
      PsiUtilCore.ensureValid(element);
    }

    ExpressionContext context = createExpressionContext(start);
    Result result = isQuick ? expressionNode.calculateQuickResult(context) : expressionNode.calculateResult(context);
    if (isQuick && result == null) {
      if (!oldValue.isEmpty()) {
        return;
      }
    }

    final boolean resultIsNullOrEmpty = result == null || result.equalsToText("", element);

    // do not update default value of neighbour segment
    if (resultIsNullOrEmpty && myCurrentSegmentNumber >= 0 &&
        (mySegments.getSegmentStart(segmentNumber) == mySegments.getSegmentEnd(myCurrentSegmentNumber) ||
         mySegments.getSegmentEnd(segmentNumber) == mySegments.getSegmentStart(myCurrentSegmentNumber))) {
      return;
    }
    if (defaultValue != null && resultIsNullOrEmpty) {
      result = defaultValue.calculateResult(context);
    }
    if (element != null && commitDocument) {
      PsiUtilCore.ensureValid(element);
    }
    if (result == null || result.equalsToText(oldValue, element)) return;

    replaceString(StringUtil.notNullize(result.toString()), start, end, segmentNumber);

    if (result instanceof RecalculatableResult) {
      IntArrayList indices = initEmptyVariables();
      shortenReferences();
      PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
      ((RecalculatableResult)result)
        .handleRecalc(psiFile, myDocument, mySegments.getSegmentStart(segmentNumber), mySegments.getSegmentEnd(segmentNumber));
      restoreEmptyVariables(indices);
    }
  }

  private void replaceString(String newValue, int start, int end, int segmentNumber) {
    TextRange range = TextRange.create(start, end);
    if (!TextRange.from(0, myDocument.getCharsSequence().length()).contains(range)) {
      LOG.error("Diagnostic for EA-54980. Can't extract " + range + " range. " + presentTemplate(myTemplate),
                AttachmentFactory.createAttachment(myDocument));
    }
    String oldText = range.subSequence(myDocument.getCharsSequence()).toString();

    if (!oldText.equals(newValue)) {
      mySegments.setNeighboursGreedy(segmentNumber, false);
      myDocument.replaceString(start, end, newValue);
      int newEnd = start + newValue.length();
      mySegments.replaceSegmentAt(segmentNumber, start, newEnd);
      mySegments.setNeighboursGreedy(segmentNumber, true);
      fixOverlappedSegments(segmentNumber);
    }
  }

  public int getCurrentVariableNumber() {
    return myCurrentVariableNumber;
  }

  public void previousTab() {
    if (isFinished()) {
      return;
    }

    myDocumentChangesTerminateTemplate = false;

    final int oldVar = myCurrentVariableNumber;
    int previousVariableNumber = getPreviousVariableNumber(oldVar);
    if (previousVariableNumber >= 0) {
      focusCurrentHighlighter(false);
      calcResults(false);
      doReformat();
      setCurrentVariableNumber(previousVariableNumber);
      focusCurrentExpression();
      fireCurrentVariableChanged(oldVar);
    }
  }

  public void nextTab() {
    if (isFinished()) {
      return;
    }

    //some psi operations may block the document, unblock here
    unblockDocument();

    myDocumentChangesTerminateTemplate = false;

    final int oldVar = myCurrentVariableNumber;
    int nextVariableNumber = getNextVariableNumber(oldVar);
    if (nextVariableNumber == -1) {
      calcResults(false);
      ApplicationManager.getApplication().runWriteAction(() -> reformat());
      finishTemplate(false);
      return;
    }
    focusCurrentHighlighter(false);
    calcResults(false);
    doReformat();
    setCurrentVariableNumber(nextVariableNumber);
    focusCurrentExpression();
    fireCurrentVariableChanged(oldVar);
  }

  public void considerNextTabOnLookupItemSelected(LookupElement item) {
    if (isFinished()) {
      return;
    }
    if (item != null) {
      ExpressionContext context = getCurrentExpressionContext();
      for (TemplateCompletionProcessor processor : TemplateCompletionProcessor.EP_NAME.getExtensionList()) {
        if (!processor.nextTabOnItemSelected(context, item)) {
          return;
        }
      }
    }
    TextRange range = getCurrentVariableRange();
    if (range != null && range.getLength() > 0) {
      int caret = myEditor.getCaretModel().getOffset();
      if (caret == range.getEndOffset() || isCaretInsideOrBeforeNextVariable()) {
        nextTab();
      }
      else if (caret > range.getEndOffset()) {
        gotoEnd(true);
      }
    }
  }

  private void lockSegmentAtTheSameOffsetIfAny() {
    mySegments.lockSegmentAtTheSameOffsetIfAny(getCurrentSegmentNumber());
  }

  private ExpressionContext createExpressionContext(final int start) {
    return new ExpressionContext() {
      @Override
      public Project getProject() {
        return myProject;
      }

      @Override
      public Editor getEditor() {
        return myEditor;
      }

      @Override
      public int getStartOffset() {
        return start;
      }

      @Override
      public int getTemplateStartOffset() {
        if (myTemplateRange == null) {
          return -1;
        }
        return myTemplateRange.getStartOffset();
      }

      @Override
      public int getTemplateEndOffset() {
        if (myTemplateRange == null) {
          return -1;
        }
        return myTemplateRange.getEndOffset();
      }

      @Override
      public <T> T getProperty(Key<T> key) {
        //noinspection unchecked
        return (T)myProperties.get(key);
      }

      @Nullable
      @Override
      public PsiElement getPsiElementAtStartOffset() {
        Project project = getProject();
        int templateStartOffset = getTemplateStartOffset();
        int offset = templateStartOffset > 0 ? getTemplateStartOffset() - 1 : getTemplateStartOffset();

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        Editor editor = getEditor();
        if (editor == null) {
          return null;
        }
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        return file == null ? null : file.findElementAt(offset);
      }
    };
  }

  public void gotoEnd(boolean brokenOff) {
    if (isDisposed()) return;
    if (!mySegments.isInvalid()) {
      calcResults(false);
    }
    if (!brokenOff) {
      doReformat();
    }
    finishTemplate(brokenOff);
  }

  public void gotoEnd() {
    gotoEnd(true);
  }

  private void finishTemplate(boolean broken) {
    if (isDisposed()) return;
    Editor editor = myEditor;
    LookupManager.getInstance(myProject).hideActiveLookup();
    setFinalEditorState(broken);

    try {
      fireBeforeTemplateFinished(broken);
    }
    finally {
      try {
        cleanupTemplateState();
        TemplateManagerImpl.clearTemplateState(editor);
        fireTemplateFinished(broken);
      }
      finally {
        Disposer.dispose(this);
      }
    }
  }

  private void setFinalEditorState(boolean brokenOff) {
    if (isDisposed()) return;
    myEditor.getSelectionModel().removeSelection();
    if (brokenOff && !((TemplateManagerImpl)TemplateManager.getInstance(myProject)).shouldSkipInTests()) return;

    int endSegmentNumber = getFinalSegmentNumber();
    int offset = -1;
    if (endSegmentNumber >= 0) {
      offset = mySegments.getSegmentStart(endSegmentNumber);
    }
    else {
      if (!myTemplate.isSelectionTemplate() && !myTemplate.isInline()) { //do not move caret to the end of range for selection templates
        offset = myTemplateRange.getEndOffset();
      }
    }

    if (!isInteractiveModeSupported() && getCurrentVariableNumber() > -1) {
      offset = -1; //do not move caret in multicaret mode if at least one tab had been made already
    }

    if (offset >= 0) {
      myEditor.getCaretModel().moveToOffset(offset);
      myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    }

    int selStart = myTemplate.getSelectionStartSegmentNumber();
    int selEnd = myTemplate.getSelectionEndSegmentNumber();
    if (selStart >= 0 && selEnd >= 0) {
      myEditor.getSelectionModel().setSelection(mySegments.getSegmentStart(selStart), mySegments.getSegmentStart(selEnd));
    }
  }

  private void cancelTemplate() {
    if (isDisposed()) return;
    try {
      fireTemplateCancelled();
      cleanupTemplateState();
    }
    finally {
      Disposer.dispose(this);
    }
  }

  private void cleanupTemplateState() {
    int oldVar = myCurrentVariableNumber;
    setCurrentVariableNumber(-1);
    fireCurrentVariableChanged(oldVar);
  }

  boolean isDisposed() {
    return myDocument == null;
  }

  public boolean isLastVariable() {
    return getNextVariableNumber(getCurrentVariableNumber()) < 0;
  }

  private int getNextVariableNumber(int currentVariableNumber) {
    for (int i = currentVariableNumber + 1; i < myTemplate.getVariableCount(); i++) {
      if (checkIfTabStop(i)) {
        return i;
      }
    }
    return -1;
  }

  private int getPreviousVariableNumber(int currentVariableNumber) {
    for (int i = currentVariableNumber - 1; i >= 0; i--) {
      if (checkIfTabStop(i)) {
        return i;
      }
    }
    return -1;
  }

  private boolean checkIfTabStop(int currentVariableNumber) {
    Expression expression = myTemplate.getExpressionAt(currentVariableNumber);
    if (myCurrentVariableNumber == -1) {
      if (myTemplate.skipOnStart(currentVariableNumber)) return false;
    }
    String variableName = myTemplate.getVariableNameAt(currentVariableNumber);
    if (myPredefinedVariableValues == null || !myPredefinedVariableValues.containsKey(variableName)) {
      if (myTemplate.isAlwaysStopAt(currentVariableNumber)) {
        return true;
      }
    }
    int segmentNumber = myTemplate.getVariableSegmentNumber(variableName);
    if (segmentNumber < 0) return false;
    int start = mySegments.getSegmentStart(segmentNumber);
    ExpressionContext context = createExpressionContext(start);
    Result result = expression.calculateResult(context);
    if (result == null) {
      return true;
    }
    LookupElement[] items = expression.calculateLookupItems(context);
    return items != null && items.length > 1;
  }

  private IntArrayList initEmptyVariables() {
    int endSegmentNumber = myTemplate.getEndSegmentNumber();
    int selStart = myTemplate.getSelectionStartSegmentNumber();
    int selEnd = myTemplate.getSelectionEndSegmentNumber();
    IntArrayList indices = new IntArrayList();
    List<TemplateDocumentChange> changes = new ArrayList<>();
    for (int i = 0; i < myTemplate.getSegmentsCount(); i++) {
      int length = mySegments.getSegmentEnd(i) - mySegments.getSegmentStart(i);
      if (length != 0) continue;
      if (i == endSegmentNumber || i == selStart || i == selEnd) continue;

      String name = myTemplate.getSegmentName(i);
      for (int j = 0; j < myTemplate.getVariableCount(); j++) {
        if (myTemplate.getVariableNameAt(j).equals(name)) {
          Expression e = myTemplate.getExpressionAt(j);
          String marker = "a";
          if (e instanceof MacroCallNode) {
            marker = ((MacroCallNode)e).getMacro().getDefaultValue();
          }
          changes.add(new TemplateDocumentChange(marker, mySegments.getSegmentStart(i), mySegments.getSegmentEnd(i), i));
          indices.add(i);
          break;
        }
      }
    }
    executeChanges(changes);
    return indices;
  }

  private void restoreEmptyVariables(IntArrayList indices) {
    List<TextRange> rangesToRemove = new ArrayList<>();
    for (int i = 0; i < indices.size(); i++) {
      int index = indices.getInt(i);
      rangesToRemove.add(TextRange.create(mySegments.getSegmentStart(index), mySegments.getSegmentEnd(index)));
    }
    rangesToRemove.sort((o1, o2) -> {
      int startDiff = o2.getStartOffset() - o1.getStartOffset();
      return startDiff != 0 ? startDiff : o2.getEndOffset() - o1.getEndOffset();
    });
    DocumentUtil.executeInBulk(myDocument, true, () -> {
      if (isDisposed()) {
        return;
      }
      for (TextRange range : rangesToRemove) {
        myDocument.deleteString(range.getStartOffset(), range.getEndOffset());
      }
    });
  }

  private void initTabStopHighlighters() {
    final Set<String> vars = new HashSet<>();
    for (Variable variable : myTemplate.getVariables()) {
      String variableName = variable.getName();
      if (!vars.add(variableName)) continue;
      int segmentNumber = myTemplate.getVariableSegmentNumber(variableName);
      if (segmentNumber < 0) continue;
      RangeHighlighter segmentHighlighter = getSegmentHighlighter(segmentNumber, variable, false, false);
      myTabStopHighlighters.add(segmentHighlighter);
    }

    int endSegmentNumber = myTemplate.getEndSegmentNumber();
    if (endSegmentNumber >= 0) {
      RangeHighlighter segmentHighlighter = getSegmentHighlighter(endSegmentNumber, null, false, true);
      myTabStopHighlighters.add(segmentHighlighter);
    }
  }

  private RangeHighlighter getSegmentHighlighter(int segmentNumber, @Nullable Variable var, boolean isSelected, boolean isEnd) {
    boolean newStyle = Registry.is("live.templates.highlight.all.variables");
    TextAttributesKey attributesKey = isEnd ? null :
                                      isSelected ? EditorColors.LIVE_TEMPLATE_ATTRIBUTES :
                                      newStyle && mightStopAtVariable(var, segmentNumber) ? EditorColors.LIVE_TEMPLATE_INACTIVE_SEGMENT :
                                      null;

    int start = mySegments.getSegmentStart(segmentNumber);
    int end = mySegments.getSegmentEnd(segmentNumber);
    RangeHighlighterEx segmentHighlighter = (RangeHighlighterEx)myEditor.getMarkupModel()
      .addRangeHighlighter(attributesKey, start, end, HighlighterLayer.ELEMENT_UNDER_CARET - 1, HighlighterTargetArea.EXACT_RANGE);
    segmentHighlighter.setGreedyToLeft(true);
    segmentHighlighter.setGreedyToRight(true);

    EditorColorsScheme scheme = myEditor.getColorsScheme();
    TextAttributes attributes = segmentHighlighter.getTextAttributes(scheme);
    if (attributes != null && attributes.getEffectType() == EffectType.BOXED && newStyle) {
      TextAttributes clone = attributes.clone();
      clone.setEffectType(EffectType.SLIGHTLY_WIDER_BOX);
      clone.setBackgroundColor(scheme.getDefaultBackground());
      segmentHighlighter.setTextAttributes(clone);
    }

    return segmentHighlighter;
  }

  private boolean mightStopAtVariable(@Nullable Variable var, int segmentNumber) {
    if (var == null) return false;
    if (var.isAlwaysStopAt()) return true;
    return var.getDefaultValueExpression().calculateQuickResult(getExpressionContextForSegment(segmentNumber)) == null;
  }

  private void focusCurrentHighlighter(boolean toSelect) {
    if (isFinished()) {
      return;
    }
    if (myCurrentVariableNumber >= myTabStopHighlighters.size()) {
      return;
    }
    RangeHighlighter segmentHighlighter = myTabStopHighlighters.get(myCurrentVariableNumber);
    if (segmentHighlighter != null) {
      final int segmentNumber = getCurrentSegmentNumber();
      RangeHighlighter newSegmentHighlighter = getSegmentHighlighter(segmentNumber, myTemplate.getVariables().get(myCurrentVariableNumber), toSelect, false);
      segmentHighlighter.dispose();
      myTabStopHighlighters.set(myCurrentVariableNumber, newSegmentHighlighter);
    }
  }

  private void reformat() {
    final PsiFile file = getPsiFile();
    if (file != null) {
      CodeStyleManager style = CodeStyleManager.getInstance(myProject);
      for (TemplateOptionalProcessor processor : DumbService.getDumbAwareExtensions(myProject, TemplateOptionalProcessor.EP_NAME)) {
        try {
          processor.processText(myProject, myTemplate, myDocument, myTemplateRange, myEditor);
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }
      PsiDocumentManager.getInstance(myProject).doPostponedOperationsAndUnblockDocument(myDocument);
      // for Python, we need to indent the template even if reformatting is enabled, because otherwise indents would be broken
      // and reformat wouldn't be able to fix them
      if (myTemplate.isToIndent()) {
        if (!myTemplateIndented) {
          LOG.assertTrue(myTemplateRange.isValid(), presentTemplate(myTemplate));
          smartIndent(myTemplateRange.getStartOffset(), myTemplateRange.getEndOffset());
          myTemplateIndented = true;
        }
      }
      if (myTemplate.isToReformat()) {
        try {
          int endSegmentNumber = getFinalSegmentNumber();
          PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
          RangeMarker dummyAdjustLineMarkerRange = null;
          int endVarOffset = -1;
          if (endSegmentNumber >= 0) {
            endVarOffset = mySegments.getSegmentStart(endSegmentNumber);
            TextRange range = CodeStyleManagerImpl.insertNewLineIndentMarker(file, myDocument, endVarOffset);
            if (range != null) dummyAdjustLineMarkerRange = myDocument.createRangeMarker(range);
          }
          int reformatStartOffset = myTemplateRange.getStartOffset();
          int reformatEndOffset = myTemplateRange.getEndOffset();
          if (dummyAdjustLineMarkerRange == null && endVarOffset >= 0) {
            // There is a possible case that indent marker element was not inserted (e.g. because there is no blank line
            // at the target offset). However, we want to reformat white space adjacent to the current template (if any).
            PsiElement whiteSpaceElement = CodeStyleManagerImpl.findWhiteSpaceNode(file, endVarOffset);
            if (whiteSpaceElement != null) {
              TextRange whiteSpaceRange = whiteSpaceElement.getTextRange();
              if (whiteSpaceElement.getContainingFile() != null) {
                // Support injected white space nodes.
                whiteSpaceRange = InjectedLanguageManager.getInstance(file.getProject()).injectedToHost(whiteSpaceElement, whiteSpaceRange);
              }
              reformatStartOffset = Math.min(reformatStartOffset, whiteSpaceRange.getStartOffset());
              reformatEndOffset = Math.max(reformatEndOffset, whiteSpaceRange.getEndOffset());
            }
          }
          style.reformatText(file, reformatStartOffset, reformatEndOffset);
          unblockDocument();

          if (dummyAdjustLineMarkerRange != null && dummyAdjustLineMarkerRange.isValid()) {
            //[ven] TODO: [max] correct javadoc reformatting to eliminate isValid() check!!!
            mySegments.replaceSegmentAt(endSegmentNumber, dummyAdjustLineMarkerRange.getStartOffset(), dummyAdjustLineMarkerRange.getEndOffset());
            myDocument.deleteString(dummyAdjustLineMarkerRange.getStartOffset(), dummyAdjustLineMarkerRange.getEndOffset());
            PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
          }
          if (endSegmentNumber >= 0) {
            final int offset = mySegments.getSegmentStart(endSegmentNumber);
            final int lineStart = myDocument.getLineStartOffset(myDocument.getLineNumber(offset));
            // if $END$ is at line start, put it at correct indentation
            if (myDocument.getCharsSequence().subSequence(lineStart, offset).toString().trim().isEmpty()) {
              final int adjustedOffset = style.adjustLineIndent(file, offset);
              mySegments.replaceSegmentAt(endSegmentNumber, adjustedOffset, adjustedOffset);
            }
          }
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }
    }
  }

  /**
   * @return the segment template end on. Caret going to be positioned at this segment in the end.
   */
  private int getFinalSegmentNumber() {
    int endSegmentNumber = myTemplate.getEndSegmentNumber();
    if (endSegmentNumber < 0 && getSelectionBeforeTemplate() == null) {
      endSegmentNumber = myTemplate.getVariableSegmentNumber(TemplateImpl.SELECTION);
    }
    return endSegmentNumber;
  }

  private void smartIndent(int startOffset, int endOffset) {
    int startLineNum = myDocument.getLineNumber(startOffset);
    int endLineNum = myDocument.getLineNumber(endOffset);
    if (endLineNum == startLineNum) {
      return;
    }

    int selectionIndent = -1;
    int selectionStartLine = -1;
    int selectionEndLine = -1;
    int selectionSegment = myTemplate.getVariableSegmentNumber(TemplateImpl.SELECTION);
    if (selectionSegment >= 0) {
      int selectionStart = myTemplate.getSegmentOffset(selectionSegment);
      selectionIndent = 0;
      String templateText = myTemplate.getTemplateText();
      while (selectionStart > 0 && templateText.charAt(selectionStart - 1) == ' ') {
        // TODO handle tabs
        selectionIndent++;
        selectionStart--;
      }
      selectionStartLine = myDocument.getLineNumber(mySegments.getSegmentStart(selectionSegment));
      selectionEndLine = myDocument.getLineNumber(mySegments.getSegmentEnd(selectionSegment));
    }

    int indentLineNum = startLineNum;

    int lineLength = 0;
    for (; indentLineNum >= 0; indentLineNum--) {
      lineLength = myDocument.getLineEndOffset(indentLineNum) - myDocument.getLineStartOffset(indentLineNum);
      if (lineLength > 0) {
        break;
      }
    }
    if (indentLineNum < 0) {
      return;
    }
    StringBuilder buffer = new StringBuilder();
    CharSequence text = myDocument.getCharsSequence();
    for (int i = 0; i < lineLength; i++) {
      char ch = text.charAt(myDocument.getLineStartOffset(indentLineNum) + i);
      if (ch != ' ' && ch != '\t') {
        break;
      }
      buffer.append(ch);
    }
    if (buffer.length() == 0 && selectionIndent <= 0 || startLineNum >= endLineNum) {
      return;
    }
    String stringToInsert = buffer.toString();
    int finalSelectionStartLine = selectionStartLine;
    int finalSelectionEndLine = selectionEndLine;
    int finalSelectionIndent = selectionIndent;
    DocumentUtil.executeInBulk(myDocument, true, () -> {
      for (int i = startLineNum + 1; i <= endLineNum; i++) {
        if (i > finalSelectionStartLine && i <= finalSelectionEndLine) {
          myDocument.insertString(myDocument.getLineStartOffset(i), StringUtil.repeatSymbol(' ', finalSelectionIndent));
        }
        else {
          myDocument.insertString(myDocument.getLineStartOffset(i), stringToInsert);
        }
      }
    });
  }

  public void addTemplateStateListener(TemplateEditingListener listener) {
    myListeners.add(listener);
  }

  private void fireTemplateFinished(boolean brokenOff) {
    for (TemplateEditingListener listener : myListeners) {
      listener.templateFinished(ObjectUtils.chooseNotNull(myTemplate, myPrevTemplate), brokenOff);
    }
  }

  private void fireBeforeTemplateFinished(boolean brokenOff) {
    for (TemplateEditingListener listener : myListeners) {
      listener.beforeTemplateFinished(this, myTemplate, brokenOff);
    }
  }

  private void fireWaitingForInput() {
    for (TemplateEditingListener listener : myListeners) {
      listener.waitingForInput(myTemplate);
    }
  }
  private void fireTemplateCancelled() {
    for (TemplateEditingListener listener : myListeners) {
      listener.templateCancelled(myTemplate);
    }
  }

  private void fireCurrentVariableChanged(int oldIndex) {
    for (TemplateEditingListener listener : myListeners) {
      listener.currentVariableChanged(this, myTemplate, oldIndex, myCurrentVariableNumber);
    }
    if (myCurrentSegmentNumber < 0) {
      if (myCurrentVariableNumber >= 0) {
        LOG.error("A variable with no segment: " + myCurrentVariableNumber + "; " + presentTemplate(myTemplate));
      }
    }
  }

  public Map getProperties() {
    return myProperties;
  }

  public TemplateImpl getTemplate() {
    return myTemplate;
  }

  public Editor getEditor() {
    return myEditor;
  }

  private static final class MyBasicUndoableAction extends BasicUndoableAction implements Disposable {
    @Nullable
    private TemplateState myTemplateState;

    private MyBasicUndoableAction(@NotNull TemplateState templateState, @Nullable Document document) {
      super(document != null ? new DocumentReference[]{DocumentReferenceManager.getInstance().create(document)} : null);
      myTemplateState = templateState;
    }

    @Override
    public void undo() {
      if (myTemplateState != null) {
        LookupManager.getInstance(myTemplateState.myProject).hideActiveLookup();
        myTemplateState.cancelTemplate();
      }
    }

    @Override
    public void redo() {
    }

    @Override
    public void dispose() {
      myTemplateState = null;
    }
  }
}
