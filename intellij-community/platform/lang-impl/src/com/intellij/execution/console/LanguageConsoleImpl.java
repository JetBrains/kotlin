// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.impl.ConsoleViewUtil;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.DataManager;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.impl.EditorFactoryImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBScrollBar;
import com.intellij.ui.components.JBScrollPane.Alignment;
import com.intellij.util.DocumentUtil;
import com.intellij.util.FileContentUtil;
import com.intellij.util.ui.AbstractLayoutManager;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;

/**
 * @author Gregory.Shrago
 * In case of REPL consider to use {@link LanguageConsoleBuilder}
 */
public class LanguageConsoleImpl extends ConsoleViewImpl implements LanguageConsoleView, DataProvider {
  private final Helper myHelper;

  private final ConsoleExecutionEditor myConsoleExecutionEditor;
  private final EditorEx myHistoryViewer;
  private final JPanel myPanel = new JPanel(new MyLayout());
  private final JScrollBar myScrollBar = new JBScrollBar(Adjustable.HORIZONTAL);
  private final DocumentListener myDocumentAdapter = new DocumentListener() {
    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
      myPanel.revalidate();
    }
  };

  public LanguageConsoleImpl(@NotNull Project project, @NotNull String title, @NotNull Language language) {
    this(new Helper(project, new LightVirtualFile(title, language, "")));
  }

  public LanguageConsoleImpl(@NotNull Project project, @NotNull String title, @NotNull VirtualFile virtualFile) {
    this(new Helper(project, virtualFile).setTitle(title));
  }

  public LanguageConsoleImpl(@NotNull Helper helper) {
    super(helper.project, GlobalSearchScope.allScope(helper.project), true, true);
    myHelper = helper;
    EditorFactory editorFactory = EditorFactory.getInstance();
    myConsoleExecutionEditor = new ConsoleExecutionEditor(helper);
    Disposer.register(this, myConsoleExecutionEditor);
    Document historyDocument = ((EditorFactoryImpl)editorFactory).createDocument(true);
    UndoUtil.disableUndoFor(historyDocument);
    myHistoryViewer = (EditorEx)editorFactory.createViewer(historyDocument, getProject(), EditorKind.CONSOLE);
    myHistoryViewer.getDocument().addDocumentListener(myDocumentAdapter);
    myConsoleExecutionEditor.getDocument().addDocumentListener(myDocumentAdapter);
    myScrollBar.setModel(new MyModel(myScrollBar, myHistoryViewer, myConsoleExecutionEditor.getEditor()));
    myScrollBar.putClientProperty(Alignment.class, Alignment.BOTTOM);
  }

  @NotNull
  @Override
  protected final EditorEx doCreateConsoleEditor() {
    return myHistoryViewer;
  }

  @Override
  protected final void disposeEditor() {
  }

  @NotNull
  @Override
  protected JComponent createCenterComponent() {
    initComponents();
    return myPanel;
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    return getConsoleEditor().getContentComponent();
  }

  private void initComponents() {
    setupComponents();

    myPanel.add(myHistoryViewer.getComponent());
    myPanel.add(myConsoleExecutionEditor.getComponent());
    myPanel.add(myScrollBar);
    myPanel.setBackground(myConsoleExecutionEditor.getEditor().getBackgroundColor());
    DataManager.registerDataProvider(myPanel, this);
  }

  @Override
  public void setConsoleEditorEnabled(boolean consoleEditorEnabled) {
    if (isConsoleEditorEnabled() == consoleEditorEnabled) {
      return;
    }
    myConsoleExecutionEditor.setConsoleEditorEnabled(consoleEditorEnabled);
    setHistoryScrollBarVisible(!consoleEditorEnabled);
    myScrollBar.setVisible(consoleEditorEnabled);
  }

  private void setHistoryScrollBarVisible(boolean visible) {
    JScrollBar prev = myHistoryViewer.getScrollPane().getHorizontalScrollBar();
    prev.setEnabled(visible);
  }

  private void setupComponents() {
    myHelper.setupEditor(myConsoleExecutionEditor.getEditor());
    myHelper.setupEditor(myHistoryViewer);

    myHistoryViewer.getComponent().setMinimumSize(JBUI.emptySize());
    myHistoryViewer.getComponent().setPreferredSize(JBUI.emptySize());
    myHistoryViewer.setCaretEnabled(false);

    myConsoleExecutionEditor.initComponent();

    setHistoryScrollBarVisible(false);

    myHistoryViewer.getContentComponent().addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent event) {
        if (isConsoleEditorEnabled() && UIUtil.isReallyTypedEvent(event)) {
          IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(
            () -> IdeFocusManager.getGlobalInstance().requestFocus(myConsoleExecutionEditor.getEditor().getContentComponent(), true)
          );
          myConsoleExecutionEditor.getEditor().processKeyTyped(event);
        }
      }
    });

    EmptyAction.registerActionShortcuts(myHistoryViewer.getComponent(), myConsoleExecutionEditor.getComponent());
  }

  @Override
  public final boolean isConsoleEditorEnabled() {
    return myConsoleExecutionEditor.isConsoleEditorEnabled();
  }

  @Override
  @Nullable
  public String getPrompt() {
    return myConsoleExecutionEditor.getPrompt();
  }

  @Override
  @Nullable
  public ConsoleViewContentType getPromptAttributes() {
    return myConsoleExecutionEditor.getPromptAttributes();
  }

  @Override
  public void setPromptAttributes(@NotNull ConsoleViewContentType textAttributes) {
    myConsoleExecutionEditor.setPromptAttributes(textAttributes);
  }

  @Override
  public void setPrompt(@Nullable String prompt) {
    myConsoleExecutionEditor.setPrompt(prompt);
  }

  @Override
  public void setEditable(boolean editable) {
   myConsoleExecutionEditor.setEditable(editable);
  }

  @Override
  public boolean isEditable() {
    return myConsoleExecutionEditor.isEditable();
  }

  @Override
  @NotNull
  public final PsiFile getFile() {
    return myHelper.getFileSafe();
  }

  @Override
  @NotNull
  public final VirtualFile getVirtualFile() {
    return myConsoleExecutionEditor.getVirtualFile();
  }

  @Override
  @NotNull
  public final EditorEx getHistoryViewer() {
    return myHistoryViewer;
  }

  @Override
  @NotNull
  public final Document getEditorDocument() {
    return myConsoleExecutionEditor.getDocument();
  }

  @Override
  @NotNull
  public final EditorEx getConsoleEditor() {
    return myConsoleExecutionEditor.getEditor();
  }

  @Override
  @NotNull
  public String getTitle() {
    return myHelper.title;
  }

  @Override
  public void setTitle(@NotNull String title) {
    myHelper.setTitle(title);
  }

  public String addToHistory(@NotNull TextRange textRange, @NotNull EditorEx editor, boolean preserveMarkup) {
    return addToHistoryInner(textRange, editor, false, preserveMarkup);
  }

  @NotNull
  public String prepareExecuteAction(boolean addToHistory, boolean preserveMarkup, boolean clearInput) {
    EditorEx editor = getCurrentEditor();
    Document document = editor.getDocument();
    String text = document.getText();
    TextRange range = new TextRange(0, document.getTextLength());
    if (!clearInput) {
      editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
    }

    if (addToHistory) {
      addToHistoryInner(range, editor, clearInput, preserveMarkup);
    }
    else if (clearInput) {
      setInputText("");
    }
    return text;
  }

  @NotNull
  protected String addToHistoryInner(@NotNull final TextRange textRange, @NotNull final EditorEx editor, boolean erase, final boolean preserveMarkup) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    String result = addTextRangeToHistory(textRange, editor, preserveMarkup);
    if (erase) {
      DocumentUtil.writeInRunUndoTransparentAction(
        () -> editor.getDocument().deleteString(textRange.getStartOffset(), textRange.getEndOffset()));
    }
    // always scroll to end on user input
    scrollToEnd();
    return result;
  }

  public static String printWithHighlighting(@NotNull LanguageConsoleView console, @NotNull Editor inputEditor, @NotNull TextRange textRange) {
    String text;
    EditorHighlighter highlighter;
    if (inputEditor instanceof EditorWindow) {
      PsiFile file = ((EditorWindow)inputEditor).getInjectedFile();
      highlighter =
        HighlighterFactory.createHighlighter(file.getVirtualFile(), EditorColorsManager.getInstance().getGlobalScheme(), console.getProject());
      String fullText = InjectedLanguageUtil.getUnescapedText(file, null, null);
      highlighter.setText(fullText);
      text = textRange.substring(fullText);
    }
    else {
      text = inputEditor.getDocument().getText(textRange);
      highlighter = ((EditorEx)inputEditor).getHighlighter();
    }
    SyntaxHighlighter syntax =
      highlighter instanceof LexerEditorHighlighter ? ((LexerEditorHighlighter)highlighter).getSyntaxHighlighter() : null;
    ((LanguageConsoleImpl)console).doAddPromptToHistory();
    if (syntax != null) {
      ConsoleViewUtil.printWithHighlighting(console, text, syntax);
    }
    else {
      console.print(text, ConsoleViewContentType.USER_INPUT);
    }
    console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
    return text;
  }

  @NotNull
  protected String addTextRangeToHistory(@NotNull TextRange textRange, @NotNull EditorEx inputEditor, boolean preserveMarkup) {
    return printWithHighlighting(this, inputEditor, textRange);


    //if (preserveMarkup) {
    //  duplicateHighlighters(markupModel, DocumentMarkupModel.forDocument(inputEditor.getDocument(), myProject, true), offset, textRange);
    //  // don't copy editor markup model, i.e. brace matcher, spell checker, etc.
    //  // duplicateHighlighters(markupModel, inputEditor.getMarkupModel(), offset, textRange);
    //}
  }

  protected void doAddPromptToHistory() {
    if (myConsoleExecutionEditor.getPrompt() != null) {
      print(myConsoleExecutionEditor.getPrompt(), myConsoleExecutionEditor.getPromptAttributes());
    }
  }


  //private static void duplicateHighlighters(@NotNull MarkupModel to, @NotNull MarkupModel from, int offset, @NotNull TextRange textRange) {
  //  for (RangeHighlighter rangeHighlighter : from.getAllHighlighters()) {
  //    if (!rangeHighlighter.isValid()) {
  //      continue;
  //    }
  //    Object tooltip = rangeHighlighter.getErrorStripeTooltip();
  //    HighlightInfo highlightInfo = tooltip instanceof HighlightInfo? (HighlightInfo)tooltip : null;
  //    if (highlightInfo != null) {
  //      if (highlightInfo.getSeverity() != HighlightSeverity.INFORMATION) {
  //        continue;
  //      }
  //      if (highlightInfo.type.getAttributesKey() == EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES) {
  //        continue;
  //      }
  //    }
  //    int localOffset = textRange.getStartOffset();
  //    int start = Math.max(rangeHighlighter.getStartOffset(), localOffset) - localOffset;
  //    int end = Math.min(rangeHighlighter.getEndOffset(), textRange.getEndOffset()) - localOffset;
  //    if (start > end) {
  //      continue;
  //    }
  //    RangeHighlighter h = to.addRangeHighlighter(start + offset, end + offset, rangeHighlighter.getLayer(),
  //                                                rangeHighlighter.getTextAttributes(), rangeHighlighter.getTargetArea());
  //    ((RangeHighlighterEx)h).setAfterEndOfLine(((RangeHighlighterEx)rangeHighlighter).isAfterEndOfLine());
  //  }
  //}

  @Override
  public void dispose() {
    super.dispose();
    // double dispose via RunContentDescriptor and ContentImpl
    if (myHistoryViewer.isDisposed()) return;

    myConsoleExecutionEditor.getDocument().removeDocumentListener(myDocumentAdapter);
    myHistoryViewer.getDocument().removeDocumentListener(myDocumentAdapter);

    EditorFactory editorFactory = EditorFactory.getInstance();
    editorFactory.releaseEditor(myHistoryViewer);

    if (getProject().isOpen()) {
      FileEditorManager editorManager = FileEditorManager.getInstance(getProject());
      if (editorManager.isFileOpen(getVirtualFile())) {
        editorManager.closeFile(getVirtualFile());
      }
    }
  }

  @Nullable
  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    return super.getData(dataId);
  }


  @Override
  @NotNull
  public EditorEx getCurrentEditor() {
    return myConsoleExecutionEditor.getCurrentEditor();
  }

  @Override
  @NotNull
  public Language getLanguage() {
    return getFile().getLanguage();
  }

  @Override
  public void setLanguage(@NotNull Language language) {
    myHelper.setLanguage(language);
    myHelper.getFileSafe();
  }

  @Override
  public void setInputText(@NotNull final String query) {
    myConsoleExecutionEditor.setInputText(query);
  }

  boolean isHistoryViewerForceAdditionalColumnsUsage() {
    return true;
  }

  int getMinHistoryLineCount() {
    return 2;
  }

  public static class Helper {
    public final Project project;
    public final VirtualFile virtualFile;
    String title;
    PsiFile file;

    public Helper(@NotNull Project project, @NotNull VirtualFile virtualFile) {
      this.project = project;
      this.virtualFile = virtualFile;
      title = virtualFile.getName();
    }

    public String getTitle() {
      return this.title;
    }

    public Helper setTitle(String title) {
      this.title = title;
      return this;
    }

    @NotNull
    public PsiFile getFile() {
      return ReadAction.compute(() -> PsiUtilCore.getPsiFile(project, virtualFile));
    }

    @NotNull
    public Document getDocument() {
      Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
      if (document == null) {
        Language language = (virtualFile instanceof LightVirtualFile) ? ((LightVirtualFile)virtualFile).getLanguage() : null;
        throw new AssertionError(String.format("no document for: %s (fileType: %s, language: %s, length: %s, valid: %s)",
                                               virtualFile,
                                               virtualFile.getFileType(), language, virtualFile.getLength(), virtualFile.isValid()));
      }
      return document;
    }

    public void setLanguage(Language language) {
      if (!(virtualFile instanceof LightVirtualFile)) {
        throw new UnsupportedOperationException();
      }
      ((LightVirtualFile)virtualFile).setLanguage(language);
      ((LightVirtualFile)virtualFile).setContent(getDocument(), getDocument().getText(), false);
      FileContentUtil.reparseFiles(project, Collections.singletonList(virtualFile), false);
    }

    public void setupEditor(@NotNull EditorEx editor) {
      ConsoleViewUtil.setupConsoleEditor(editor, false, false);
      editor.getContentComponent().setFocusCycleRoot(false);
      editor.setHorizontalScrollbarVisible(true);
      editor.setVerticalScrollbarVisible(true);
      editor.setBorder(null);

      EditorSettings editorSettings = editor.getSettings();
      editorSettings.setAdditionalLinesCount(1);
      editorSettings.setAdditionalColumnsCount(1);

      DataManager.registerDataProvider(editor.getComponent(), (dataId) -> getEditorData(editor, dataId));
    }

    @NotNull
    public PsiFile getFileSafe() {
      return file == null || !file.isValid() ? file = getFile() : file;
    }

    @Nullable
    protected Object getEditorData(@NotNull EditorEx editor, String dataId) {
      if (OpenFileDescriptor.NAVIGATE_IN_EDITOR.is(dataId)) {
        return editor;
      }
      else if (project.isInitialized()) {
        Caret caret = editor.getCaretModel().getCurrentCaret();
        return FileEditorManagerEx.getInstanceEx(project).getData(dataId, editor, caret);
      }
      return null;
    }
  }

  private class MyLayout extends AbstractLayoutManager {
    @Override
    public Dimension preferredLayoutSize(final Container parent) {
      return new Dimension(0, 0);
    }

    @Override
    public void layoutContainer(@NotNull final Container parent) {
      final int componentCount = parent.getComponentCount();
      if (componentCount == 0) {
        return;
      }

      final EditorEx history = myHistoryViewer;
      final EditorEx input = isConsoleEditorEnabled() ? myConsoleExecutionEditor.getEditor() : null;
      if (input == null) {
        parent.getComponent(0).setBounds(parent.getBounds());
        return;
      }

      final Dimension panelSize = parent.getSize();
      if (myScrollBar.isVisible()) {
        Dimension size = myScrollBar.getPreferredSize();
        if (panelSize.height < size.height) return;
        panelSize.height -= size.height;
        myScrollBar.setBounds(0, panelSize.height, panelSize.width, size.height);
      }
      if (panelSize.getHeight() <= 0) {
        return;
      }
      final Dimension historySize = history.getContentSize();
      final Dimension inputSize = input.getContentSize();

      // deal with width
      if (isHistoryViewerForceAdditionalColumnsUsage()) {
        history.getSoftWrapModel().forceAdditionalColumnsUsage();

        int minAdditionalColumns = 2;
        // calculate content size without additional columns except minimal amount
        int historySpaceWidth = EditorUtil.getPlainSpaceWidth(history);
        historySize.width += historySpaceWidth * (minAdditionalColumns - history.getSettings().getAdditionalColumnsCount());
        // calculate content size without additional columns except minimal amount
        int inputSpaceWidth = EditorUtil.getPlainSpaceWidth(input);
        inputSize.width += inputSpaceWidth * (minAdditionalColumns - input.getSettings().getAdditionalColumnsCount());
        // calculate additional columns according to the corresponding width
        int max = Math.max(historySize.width, inputSize.width);
        history.getSettings().setAdditionalColumnsCount(minAdditionalColumns + (max - historySize.width) / historySpaceWidth);
        input.getSettings().setAdditionalColumnsCount(minAdditionalColumns + (max - inputSize.width) / inputSpaceWidth);
      }

      int newInputHeight;
      // deal with height, WEB-11122 we cannot trust editor width - it could be 0 in case of soft wrap even if editor has text
      if (history.getDocument().getLineCount() == 0) {
        historySize.height = 0;
      }

      int minHistoryHeight = historySize.height > 0 ? getMinHistoryLineCount() * history.getLineHeight() : 0;
      int minInputHeight = input.isViewer() ? 0 : input.getLineHeight();
      final int inputPreferredHeight = input.isViewer() ? 0 : Math.max(minInputHeight, inputSize.height);
      final int historyPreferredHeight = Math.max(minHistoryHeight, historySize.height);
      if (panelSize.height < minInputHeight) {
        newInputHeight = panelSize.height;
      }
      else if (panelSize.height < inputPreferredHeight) {
        newInputHeight = panelSize.height - minHistoryHeight;
      }
      else if (panelSize.height < (inputPreferredHeight + historyPreferredHeight) || inputPreferredHeight == 0) {
        newInputHeight = inputPreferredHeight;
      }
      else {
        newInputHeight = panelSize.height - historyPreferredHeight;
      }

      int oldHistoryHeight = history.getComponent().getHeight();
      int newHistoryHeight = panelSize.height - newInputHeight;
      int delta = newHistoryHeight - ((newHistoryHeight / history.getLineHeight()) * history.getLineHeight());
      newHistoryHeight -= delta;
      newInputHeight += delta;

      // apply new bounds & scroll history viewer
      input.getComponent().setBounds(0, newHistoryHeight, panelSize.width, newInputHeight);
      history.getComponent().setBounds(0, 0, panelSize.width, newHistoryHeight);
      input.getComponent().doLayout();
      history.getComponent().doLayout();
      if (newHistoryHeight < oldHistoryHeight) {
        JViewport viewport = history.getScrollPane().getViewport();
        Point position = viewport.getViewPosition();
        position.translate(0, oldHistoryHeight - newHistoryHeight);
        viewport.setViewPosition(position);
      }
    }
  }

  private static final class MyModel extends DefaultBoundedRangeModel {
    private volatile boolean myInternalChange;
    private final JScrollBar myBar;
    private final EditorEx myFirstEditor;
    private final EditorEx mySecondEditor;
    private int myFirstValue;
    private int mySecondValue;

    private MyModel(JScrollBar bar, EditorEx first, EditorEx second) {
      myBar = bar;
      myFirstEditor = first;
      mySecondEditor = second;
      addChangeListener(event -> onChange());
      first.getScrollPane().getViewport().addChangeListener(event -> onUpdate(event.getSource()));
      second.getScrollPane().getViewport().addChangeListener(event -> onUpdate(event.getSource()));
    }

    private boolean isInternal() {
      return myInternalChange || !myFirstEditor.getComponent().isVisible() || !mySecondEditor.getComponent().isVisible();
    }

    private void onChange() {
      if (isInternal()) return;
      myInternalChange = true;
      setValue(myFirstEditor.getScrollPane().getViewport(), getValue());
      setValue(mySecondEditor.getScrollPane().getViewport(), getValue());
      myInternalChange = false;
    }

    private void onUpdate(Object source) {
      if (isInternal()) return;
      JViewport first = myFirstEditor.getScrollPane().getViewport();
      JViewport second = mySecondEditor.getScrollPane().getViewport();
      int value = getValue();
      if (source == first) {
        Point position = first.getViewPosition();
        if (position.x != myFirstValue) {
          myFirstValue = value = position.x;
        }
      }
      else {
        Point position = second.getViewPosition();
        if (position.x != mySecondValue) {
          mySecondValue = value = position.x;
        }
      }
      int ext = Math.min(first.getExtentSize().width, second.getExtentSize().width);
      int max = Math.max(first.getViewSize().width, second.getViewSize().width);
      setRangeProperties(value, ext, 0, max, false);
      myBar.setEnabled(ext < max);
    }

    private static void setValue(JViewport viewport, int value) {
      Point position = viewport.getViewPosition();
      position.x = Math.max(0, Math.min(value, viewport.getViewSize().width - viewport.getExtentSize().width));
      viewport.setViewPosition(position);
    }
  }
}
