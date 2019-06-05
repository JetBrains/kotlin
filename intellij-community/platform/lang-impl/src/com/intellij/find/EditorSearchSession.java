// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.find;

import com.intellij.execution.impl.ConsoleViewUtil;
import com.intellij.find.editorHeaderActions.*;
import com.intellij.find.impl.HelpID;
import com.intellij.find.impl.RegExHelpPopup;
import com.intellij.find.impl.livePreview.LivePreviewController;
import com.intellij.find.impl.livePreview.SearchResults;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.ex.DefaultCustomComponentAction;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;

/**
 * @author max, andrey.zaytsev
 */
public class EditorSearchSession implements SearchSession,
                                            DataProvider,
                                            SelectionListener,
                                            SearchResults.SearchResultsListener,
                                            SearchReplaceComponent.Listener {
  public static final DataKey<EditorSearchSession> SESSION_KEY = DataKey.create("EditorSearchSession");

  private final Editor myEditor;
  private final LivePreviewController myLivePreviewController;
  private final SearchResults mySearchResults;
  @NotNull
  private final FindModel myFindModel;
  private final SearchReplaceComponent myComponent;
  private final RangeMarker myStartSessionSelectionMarker;
  private final RangeMarker myStartSessionCaretMarker;

  private final LinkLabel<Object> myClickToHighlightLabel = new LinkLabel<>("Click to highlight", null, (__, ___) -> {
    setMatchesLimit(Integer.MAX_VALUE);
    updateResults(true);
  });
  private final Disposable myDisposable = Disposer.newDisposable(EditorSearchSession.class.getName());

  public EditorSearchSession(@NotNull Editor editor, Project project) {
    this(editor, project, createDefaultFindModel(project, editor));
  }

  public EditorSearchSession(@NotNull final Editor editor, Project project, @NotNull FindModel findModel) {
    assert !editor.isDisposed();

    myClickToHighlightLabel.setVisible(false);

    myFindModel = findModel;

    myEditor = editor;
    myStartSessionSelectionMarker = myEditor.getDocument().createRangeMarker(
      myEditor.getSelectionModel().getSelectionStart(),
      myEditor.getSelectionModel().getSelectionEnd()
    );
    myStartSessionCaretMarker = myEditor.getDocument().createRangeMarker(
      myEditor.getCaretModel().getOffset(),
      myEditor.getCaretModel().getOffset()
    );

    mySearchResults = new SearchResults(myEditor, project);
    myLivePreviewController = new LivePreviewController(mySearchResults, this, myDisposable);

    myComponent = SearchReplaceComponent
      .buildFor(project, myEditor.getContentComponent())
      .addPrimarySearchActions(new PrevOccurrenceAction(),
                               new NextOccurrenceAction(),
                               new FindAllAction(),
                               new Separator(),
                               new AddOccurrenceAction(),
                               new RemoveOccurrenceAction(),
                               new SelectAllAction(),
                               new Separator())
      .addSecondarySearchActions(new ToggleAnywhereAction(),
                                 new ToggleInCommentsAction(),
                                 new ToggleInLiteralsOnlyAction(),
                                 new ToggleExceptCommentsAction(),
                                 new ToggleExceptLiteralsAction(),
                                 new ToggleExceptCommentsAndLiteralsAction())
      .addExtraSearchActions(new ToggleMatchCase(),
                             new ToggleWholeWordsOnlyAction(),
                             new ToggleRegex(),
                             new DefaultCustomComponentAction(
                               () -> RegExHelpPopup.createRegExLink("<html><body><b>?</b></body></html>", null, null)),
                             new StatusTextAction(),
                             new DefaultCustomComponentAction(() -> myClickToHighlightLabel))
      .addSearchFieldActions(new RestorePreviousSettingsAction())
      .addPrimaryReplaceActions(new ReplaceAction(),
                                new ReplaceAllAction(),
                                new ExcludeAction())
      .addExtraReplaceAction(new TogglePreserveCaseAction(),
                             new ToggleSelectionOnlyAction())
      .addReplaceFieldActions(new PrevOccurrenceAction(false),
                              new NextOccurrenceAction(false))
      .withDataProvider(this)
      .withCloseAction(this::close)
      .withReplaceAction(this::replaceCurrent)
      .withSecondarySearchActionsIsModifiedGetter(() -> myFindModel.getSearchContext() != FindModel.SearchContext.ANY)
      .build();

    myComponent.addListener(this);
    new UiNotifyConnector(myComponent, new Activatable() {
      @Override
      public void showNotify() {
        initLivePreview();
      }

      @Override
      public void hideNotify() {
        myLivePreviewController.off();
        mySearchResults.removeListener(EditorSearchSession.this);
      }
    });

    new SwitchToFind(getComponent());
    new SwitchToReplace(getComponent());

    myFindModel.addObserver(new FindModel.FindModelObserver() {
      boolean myReentrantLock = false;
      boolean myIsGlobal = myFindModel.isGlobal();
      boolean myIsReplace = myFindModel.isReplaceState();
      @Override
      public void findModelChanged(FindModel findModel1) {
        if (myReentrantLock) return;
        try {
          myReentrantLock = true;
          String stringToFind = myFindModel.getStringToFind();
          if (!wholeWordsApplicable(stringToFind)) {
            myFindModel.setWholeWordsOnly(false);
          }
          if (myIsGlobal != myFindModel.isGlobal() || myIsReplace != myFindModel.isReplaceState()) {
            if (myFindModel.getStringToFind().isEmpty() && myFindModel.isGlobal()) {
              myFindModel.setStringToFind(StringUtil.notNullize(myEditor.getSelectionModel().getSelectedText()));
            }
            if (!myFindModel.isGlobal() && myFindModel.getStringToFind().equals(myEditor.getSelectionModel().getSelectedText())) {
              myFindModel.setStringToFind("");
            }
            myIsGlobal = myFindModel.isGlobal();
            myIsReplace = myFindModel.isReplaceState();
          }
          EditorSearchSession.this.updateUIWithFindModel();
          mySearchResults.clear();
          EditorSearchSession.this.updateResults(true);
          FindUtil.updateFindInFileModel(EditorSearchSession.this.getProject(), myFindModel, !ConsoleViewUtil.isConsoleViewEditor(editor));
        } finally {
          myReentrantLock = false;
        }
      }
    });

    updateUIWithFindModel();

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      initLivePreview();
    }
    updateMultiLineStateIfNeeded();

    EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
      @Override
      public void editorReleased(@NotNull EditorFactoryEvent event) {
        if (event.getEditor() == myEditor) {
          Disposer.dispose(myDisposable);
          myLivePreviewController.dispose();
          myStartSessionSelectionMarker.dispose();
          myStartSessionCaretMarker.dispose();
        }
      }
    }, myDisposable);
  }

  public Editor getEditor() {
    return myEditor;
  }

  @Nullable
  public static EditorSearchSession get(@Nullable Editor editor) {
    JComponent headerComponent = editor != null ? editor.getHeaderComponent() : null;
    SearchReplaceComponent searchReplaceComponent = ObjectUtils.tryCast(headerComponent, SearchReplaceComponent.class);
    return searchReplaceComponent != null ? SESSION_KEY.getData(searchReplaceComponent) : null;
  }

  @NotNull
  public static EditorSearchSession start(@NotNull Editor editor, @Nullable Project project) {
    EditorSearchSession session = new EditorSearchSession(editor, project);
    editor.setHeaderComponent(session.getComponent());
    return session;
  }

  @NotNull
  public static EditorSearchSession start(@NotNull Editor editor, @NotNull FindModel findModel, @Nullable Project project) {
    EditorSearchSession session = new EditorSearchSession(editor, project, findModel);
    editor.setHeaderComponent(session.getComponent());
    return session;
  }

  @NotNull
  @Override
  public SearchReplaceComponent getComponent() {
    return myComponent;
  }

  public Project getProject() {
    return myComponent.getProject();
  }

  @NotNull
  private static FindModel createDefaultFindModel(Project project, Editor editor) {
    FindModel findModel = new FindModel();
    findModel.copyFrom(FindManager.getInstance(project).getFindInFileModel());
    if (editor.getSelectionModel().hasSelection()) {
      String selectedText = editor.getSelectionModel().getSelectedText();
      if (selectedText != null) {
        findModel.setStringToFind(selectedText);
      }
    }
    findModel.setPromptOnReplace(false);
    return findModel;
  }


  @Override
  @Nullable
  public Object getData(@NotNull @NonNls final String dataId) {
    if (SearchSession.KEY.is(dataId)) {
      return this;
    }
    if (SESSION_KEY.is(dataId)) {
      return this;
    }
    if (CommonDataKeys.EDITOR_EVEN_IF_INACTIVE.is(dataId)) {
      return myEditor;
    }
    if (PlatformDataKeys.HELP_ID.is(dataId)) {
      return myFindModel.isReplaceState() ? HelpID.REPLACE_IN_EDITOR : HelpID.FIND_IN_EDITOR;
    }
    return null;
  }

  @Override
  public void searchResultsUpdated(@NotNull SearchResults sr) {
    if (sr.getFindModel() == null) return;
    if (myComponent.getSearchTextComponent().getText().isEmpty()) {
      updateUIWithEmptyResults();
    } else {
      int matches = sr.getMatchesCount();
      boolean tooManyMatches = matches > mySearchResults.getMatchesLimit();
      myComponent.setStatusText(tooManyMatches
                                ? ApplicationBundle.message("editorsearch.toomuch", mySearchResults.getMatchesLimit())
                                : ApplicationBundle.message("editorsearch.matches", matches));
      myClickToHighlightLabel.setVisible(tooManyMatches);
      if (!tooManyMatches && matches <= 0) {
        myComponent.setNotFoundBackground();
      }
      else {
        myComponent.setRegularBackground();
      }
    }
    myComponent.updateActions();
  }

  @Override
  public void cursorMoved() {
    myComponent.updateActions();
  }

  @Override
  public void updateFinished() {
  }

  @Override
  public void searchFieldDocumentChanged() {
    setMatchesLimit(LivePreviewController.MATCHES_LIMIT);
    String text = myComponent.getSearchTextComponent().getText();
    myFindModel.setStringToFind(text);
    if (!StringUtil.isEmpty(text)) {
      updateResults(true);
    }
    else {
      nothingToSearchFor(true);
    }
    updateMultiLineStateIfNeeded();
  }

  private void updateMultiLineStateIfNeeded() {
    myFindModel.setMultiline(myComponent.getSearchTextComponent().getText().contains("\n") ||
                             myComponent.getReplaceTextComponent().getText().contains("\n"));
  }

  @Override
  public void replaceFieldDocumentChanged() {
    setMatchesLimit(LivePreviewController.MATCHES_LIMIT);
    myFindModel.setStringToReplace(myComponent.getReplaceTextComponent().getText());
    updateMultiLineStateIfNeeded();
  }

  @Override
  public void multilineStateChanged() {
    myFindModel.setMultiline(myComponent.isMultiline());
  }

  @NotNull
  @Override
  public FindModel getFindModel() {
    return myFindModel;
  }

  @Override
  public boolean hasMatches() {
    return mySearchResults != null && mySearchResults.hasMatches();
  }

  @Override
  public void searchForward() {
    moveCursor(SearchResults.Direction.DOWN);
    addTextToRecent(myComponent.getSearchTextComponent());
  }

  @Override
  public void searchBackward() {
    moveCursor(SearchResults.Direction.UP);
    addTextToRecent(myComponent.getSearchTextComponent());
  }

  private void updateUIWithFindModel() {
    myComponent.update(myFindModel.getStringToFind(),
                       myFindModel.getStringToReplace(),
                       myFindModel.isReplaceState(),
                       myFindModel.isMultiline());
    myLivePreviewController.setTrackingSelection(!myFindModel.isGlobal());
  }

  private static boolean wholeWordsApplicable(String stringToFind) {
    return !stringToFind.startsWith(" ") &&
           !stringToFind.startsWith("\t") &&
           !stringToFind.endsWith(" ") &&
           !stringToFind.endsWith("\t");
  }

  private void setMatchesLimit(int value) {
    mySearchResults.setMatchesLimit(value);
  }

  private void replaceCurrent() {
    if (mySearchResults.getCursor() != null) {
      try {
        myLivePreviewController.performReplace();
      }
      catch (FindManager.MalformedReplacementStringException e) {
        Messages.showErrorDialog(myComponent, e.getMessage(), FindBundle.message("find.replace.invalid.replacement.string.title"));
      }
    }
  }

  public void addTextToRecent(JTextComponent textField) {
    myComponent.addTextToRecent(textField);
  }

  @Override
  public void selectionChanged(@NotNull SelectionEvent e) {
    updateResults(false);
  }

  private void moveCursor(SearchResults.Direction direction) {
    myLivePreviewController.moveCursor(direction);
  }

  @Override
  public void close() {
    IdeFocusManager.getInstance(getProject()).requestFocus(myEditor.getContentComponent(), false);

    myLivePreviewController.dispose();
    myEditor.setHeaderComponent(null);
  }

  private void initLivePreview() {
    if (myEditor.isDisposed()) return;

    myLivePreviewController.on();

    myLivePreviewController.setUserActivityDelay(0);
    updateResults(false);
    myLivePreviewController.setUserActivityDelay(LivePreviewController.USER_ACTIVITY_TRIGGERING_DELAY);

    mySearchResults.addListener(this);
  }

  private void updateResults(final boolean allowedToChangedEditorSelection) {
    final String text = myFindModel.getStringToFind();
    if (text.isEmpty()) {
      nothingToSearchFor(allowedToChangedEditorSelection);
    }
    else {

      if (myFindModel.isRegularExpressions()) {
        try {
          Pattern.compile(text);
        }
        catch (Exception e) {
          myComponent.setNotFoundBackground();
          myClickToHighlightLabel.setVisible(false);
          mySearchResults.clear();
          myComponent.setStatusText(INCORRECT_REGEX_MESSAGE);
          return;
        }
      }


      final FindManager findManager = FindManager.getInstance(getProject());
      if (allowedToChangedEditorSelection) {
        findManager.setFindWasPerformed();
        FindModel copy = new FindModel();
        copy.copyFrom(myFindModel);
        copy.setReplaceState(false);
        findManager.setFindNextModel(copy);
      }
      if (myLivePreviewController != null) {
        myLivePreviewController.updateInBackground(myFindModel, allowedToChangedEditorSelection);
      }
    }
  }

  private void nothingToSearchFor(boolean allowedToChangedEditorSelection) {
    updateUIWithEmptyResults();
    if (mySearchResults != null) {
      mySearchResults.clear();
    }
    if (allowedToChangedEditorSelection
        && !UIUtil.isClientPropertyTrue(myComponent.getSearchTextComponent(), SearchTextArea.JUST_CLEARED_KEY)) {
      restoreInitialCaretPositionAndSelection();
    }
  }

  private void restoreInitialCaretPositionAndSelection() {
    int originalSelectionStart = Math.min(myStartSessionSelectionMarker.getStartOffset(), myEditor.getDocument().getTextLength());
    int originalSelectionEnd = Math.min(myStartSessionSelectionMarker.getEndOffset(), myEditor.getDocument().getTextLength());

    myEditor.getSelectionModel().setSelection(originalSelectionStart, originalSelectionEnd);
    myEditor.getCaretModel().moveToOffset(Math.min(myStartSessionCaretMarker.getEndOffset(), myEditor.getDocument().getTextLength()));
    myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
  }

  private void updateUIWithEmptyResults() {
    myComponent.setRegularBackground();
    myComponent.setStatusText("");
    myClickToHighlightLabel.setVisible(false);
  }

  public String getTextInField() {
    return myComponent.getSearchTextComponent().getText();
  }

  public void setTextInField(final String text) {
    myComponent.getSearchTextComponent().setText(text);
    myFindModel.setStringToFind(text);
  }

  public void selectAllOccurrences() {
    FindUtil.selectSearchResultsInEditor(myEditor, mySearchResults.getOccurrences().iterator(), -1);
  }

  public void removeOccurrence() {
    mySearchResults.prevOccurrence(true);
  }

  public void addNextOccurrence() {
    mySearchResults.nextOccurrence(true);
  }

  public void clearUndoInTextFields() {
    myComponent.resetUndoRedoActions();
  }


  private abstract static class ButtonAction extends DumbAwareAction implements CustomComponentAction, ActionListener {
    private final String myTitle;
    private final char myMnemonic;

    ButtonAction(@NotNull String title, char mnemonic) {
      myTitle = title;
      myMnemonic = mnemonic;
    }

    @NotNull
    @Override
    public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
      JButton button = new JButton(myTitle);
      button.setFocusable(false);
      if (!UISettings.getInstance().getDisableMnemonicsInControls()) {
        button.setMnemonic(myMnemonic);
      }
      button.addActionListener(this);
      return button;
    }

    @Override
    public final void update(@NotNull AnActionEvent e) {
      JButton button = (JButton)e.getPresentation().getClientProperty(COMPONENT_KEY);
      if (button != null) {
        update(button);
      }
    }

    @Override
    public final void actionPerformed(@NotNull AnActionEvent e) {
      onClick();
    }

    @Override
    public final void actionPerformed(ActionEvent e) {
      onClick();
    }

    protected abstract void update(@NotNull JButton button);

    protected abstract void onClick();
  }

  private class ReplaceAction extends ButtonAction {
    ReplaceAction() {
      super("Replace", 'p');
    }

    @Override
    protected void update(@NotNull JButton button) {
      button.setEnabled(mySearchResults.hasMatches());
    }

    @Override
    protected void onClick() {
      replaceCurrent();
    }
  }

  private class ReplaceAllAction extends ButtonAction {
    ReplaceAllAction() {
      super("Replace all", 'a');
    }

    @Override
    protected void update(@NotNull JButton button) {
      button.setEnabled(mySearchResults.hasMatches());
    }

    @Override
    protected void onClick() {
      myLivePreviewController.performReplaceAll();
    }
  }

  private class ExcludeAction extends ButtonAction {
    ExcludeAction() {
      super("", 'l');
    }

    @Override
    protected void update(@NotNull JButton button) {
      FindResult cursor = mySearchResults.getCursor();
      button.setEnabled(cursor != null);
      button.setText(cursor != null && mySearchResults.isExcluded(cursor) ? "Include" : "Exclude");
    }

    @Override
    protected void onClick() {
      myLivePreviewController.exclude();
      moveCursor(SearchResults.Direction.DOWN);
    }
  }
}
