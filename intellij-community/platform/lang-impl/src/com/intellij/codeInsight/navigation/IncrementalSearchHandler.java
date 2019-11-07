// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.navigation;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.ui.HintHint;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightweightHint;
import com.intellij.util.text.StringSearcher;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class IncrementalSearchHandler {
  private static final Key<PerEditorSearchData> SEARCH_DATA_IN_EDITOR_VIEW_KEY = Key.create("IncrementalSearchHandler.SEARCH_DATA_IN_EDITOR_VIEW_KEY");
  private static final Key<PerHintSearchData> SEARCH_DATA_IN_HINT_KEY = Key.create("IncrementalSearchHandler.SEARCH_DATA_IN_HINT_KEY");
  private static final Logger LOG = Logger.getInstance(IncrementalSearchHandler.class);

  private static boolean ourActionsRegistered = false;

  private static class PerHintSearchData {
    final Project project;
    final JLabel label;

    int searchStart;
    RangeHighlighter segmentHighlighter;
    boolean ignoreCaretMove = false;

    PerHintSearchData(Project project, JLabel label) {
      this.project = project;
      this.label = label;
    }
  }

  private static class PerEditorSearchData {
    LightweightHint hint;
    String lastSearch;
  }

  public static boolean isHintVisible(final Editor editor) {
    final PerEditorSearchData data = editor.getUserData(SEARCH_DATA_IN_EDITOR_VIEW_KEY);
    return data != null && data.hint != null && data.hint.isVisible();
  }

  public void invoke(Project project, final Editor editor) {
    if (!ourActionsRegistered) {
      EditorActionManager actionManager = EditorActionManager.getInstance();

      TypedAction typedAction = TypedAction.getInstance();
      typedAction.setupRawHandler(new MyTypedHandler(typedAction.getRawHandler()));

      actionManager.setActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE, new BackSpaceHandler(actionManager.getActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE)));
      actionManager.setActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP, new UpHandler(actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP)));
      actionManager.setActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN, new DownHandler(actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN)));

      ourActionsRegistered = true;
    }

    FeatureUsageTracker.getInstance().triggerFeatureUsed("editing.incremental.search");

    String selection = editor.getSelectionModel().getSelectedText();
    JLabel label2 = new MyLabel(selection == null ? "" : selection);

    PerEditorSearchData data = editor.getUserData(SEARCH_DATA_IN_EDITOR_VIEW_KEY);
    if (data == null) {
      data = new PerEditorSearchData();
    } else {
      if (data.hint != null) {
        if (data.lastSearch != null) {
          PerHintSearchData hintData = data.hint.getUserData(SEARCH_DATA_IN_HINT_KEY);
          //The user has not started typing
          if ("".equals(hintData.label.getText())) {
            label2 = new MyLabel(data.lastSearch);
          }
        }
        data.hint.hide();
      }
    }

    JLabel label1 = new MyLabel(" " + CodeInsightBundle.message("incremental.search.tooltip.prefix"));
    label1.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));

    JPanel panel = new MyPanel(label1);
    panel.add(label1, BorderLayout.WEST);
    panel.add(label2, BorderLayout.CENTER);
    panel.setBorder(BorderFactory.createLineBorder(Color.black));

    final DocumentListener[] documentListener = new DocumentListener[1];
    final CaretListener[] caretListener = new CaretListener[1];
    final Document document = editor.getDocument();

    final LightweightHint hint = new LightweightHint(panel) {
      @Override
      public void hide() {
        PerHintSearchData data = getUserData(SEARCH_DATA_IN_HINT_KEY);
        LOG.assertTrue(data != null);
        String prefix = data.label.getText();

        super.hide();

        if (data.segmentHighlighter != null){
          data.segmentHighlighter.dispose();
        }
        PerEditorSearchData editorData = editor.getUserData(SEARCH_DATA_IN_EDITOR_VIEW_KEY);
        editorData.hint = null;
        editorData.lastSearch = prefix;

        if (documentListener[0] != null){
          document.removeDocumentListener(documentListener[0]);
          documentListener[0] = null;
        }

        if (caretListener[0] != null){
          CaretListener listener = caretListener[0];
          editor.getCaretModel().removeCaretListener(listener);
        }
      }
    };

    documentListener[0] = new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent e) {
        if (!hint.isVisible()) return;
        hint.hide();
      }
    };
    document.addDocumentListener(documentListener[0]);

    caretListener[0] = new CaretListener() {
      @Override
      public void caretPositionChanged(@NotNull CaretEvent e) {
        PerHintSearchData data = hint.getUserData(SEARCH_DATA_IN_HINT_KEY);
        if (data != null && data.ignoreCaretMove) return;
        if (!hint.isVisible()) return;
        hint.hide();
      }
    };
    CaretListener listener = caretListener[0];
    editor.getCaretModel().addCaretListener(listener);

    final JComponent component = editor.getComponent();
    int x =  SwingUtilities.convertPoint(component,0,0,component).x;
    int y = - hint.getComponent().getPreferredSize().height;
    Point p = SwingUtilities.convertPoint(component,x,y,component.getRootPane().getLayeredPane());

    HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, p, HintManagerImpl.HIDE_BY_ESCAPE | HintManagerImpl.HIDE_BY_TEXT_CHANGE, 0, false, new HintHint(editor, p).setAwtTooltip(false));

    PerHintSearchData hintData = new PerHintSearchData(project, label2);
    hintData.searchStart = editor.getCaretModel().getOffset();
    hint.putUserData(SEARCH_DATA_IN_HINT_KEY, hintData);

    data.hint = hint;
    editor.putUserData(SEARCH_DATA_IN_EDITOR_VIEW_KEY, data);

    if (!hintData.label.getText().isEmpty()) {
      updatePosition(editor, hintData, true, false);
    }
  }

  private static boolean acceptableRegExp(String pattern) {
    final int len = pattern.length();

    for(int i=0;i<len;++i) {
      if (pattern.charAt(i) == '*') {
        return true;
      }
    }

    return false;
  }

  private static void updatePosition(Editor editor, PerHintSearchData data, boolean nothingIfFailed, boolean searchBack) {
    final String prefix = data.label.getText();
    int matchLength = prefix.length();
    int index;

    if (matchLength == 0) {
      index = data.searchStart;
    }
    else {
      final Document document = editor.getDocument();
      final CharSequence text = document.getCharsSequence();
      final int length = document.getTextLength();
      final boolean caseSensitive = detectSmartCaseSensitive(prefix);

      if (acceptableRegExp(prefix)) {
        @NonNls final StringBuilder buf = new StringBuilder(prefix.length());
        final int len = prefix.length();

        for (int i = 0; i < len; ++i) {
          final char ch = prefix.charAt(i);

          // bother only * withing text
          if (ch == '*' && i != 0 && i != len - 1) {
            buf.append("\\w");
          }
          else if ("{}[].+^$*()?".indexOf(ch) != -1) {
            // do not bother with other metachars
            buf.append('\\');
          }
          buf.append(ch);
        }

        try {
          Pattern pattern = Pattern.compile(buf.toString(), caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
          Matcher matcher = pattern.matcher(text);
          if (searchBack) {
            int lastStart = -1;
            int lastEnd = -1;

            while (matcher.find() && matcher.start() < data.searchStart) {
              lastStart = matcher.start();
              lastEnd = matcher.end();
            }

            index = lastStart;
            matchLength = lastEnd - lastStart;
          }
          else if (matcher.find(data.searchStart) || !nothingIfFailed && matcher.find(0)) {
            index = matcher.start();
            matchLength = matcher.end() - matcher.start();
          }
          else {
            index = -1;
          }
        }
        catch (PatternSyntaxException ex) {
          index = -1; // let the user to make the garbage pattern
        }
      }
      else {
        StringSearcher searcher = new StringSearcher(prefix, caseSensitive, !searchBack);

        if (searchBack) {
          index = searcher.scan(text, 0, data.searchStart);
        }
        else {
          index = searcher.scan(text, data.searchStart, length);
          index = index < 0 ? -1 : index;
        }
        if (index < 0 && !nothingIfFailed) {
          index = searcher.scan(text);
        }
      }
    }

    if (nothingIfFailed && index < 0) return;
    if (data.segmentHighlighter != null) {
      data.segmentHighlighter.dispose();
      data.segmentHighlighter = null;
    }
    if (index < 0) {
      data.label.setForeground(JBColor.RED);
    }
    else {
      data.label.setForeground(JBColor.foreground());
      if (matchLength > 0) {
        TextAttributes attributes = editor.getColorsScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
        data.segmentHighlighter = editor.getMarkupModel()
          .addRangeHighlighter(index, index + matchLength, HighlighterLayer.LAST + 1, attributes, HighlighterTargetArea.EXACT_RANGE);
      }
      data.ignoreCaretMove = true;
      editor.getCaretModel().moveToOffset(index);
      editor.getSelectionModel().removeSelection();
      editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
      data.ignoreCaretMove = false;
      IdeDocumentHistory.getInstance(data.project).includeCurrentCommandAsNavigation();
    }
  }

  private static boolean detectSmartCaseSensitive(String prefix) {
    boolean hasUpperCase = false;
    for(int i = 0; i < prefix.length(); i++){
      char c = prefix.charAt(i);
      if (Character.isUpperCase(c) && Character.toUpperCase(c) != Character.toLowerCase(c)){
        hasUpperCase = true;
        break;
      }
    }
    return hasUpperCase;
  }

  private static class MyLabel extends JLabel {
    MyLabel(String text) {
      super(text);
      this.setBackground(HintUtil.getInformationColor());
      this.setForeground(JBColor.foreground());
      this.setOpaque(true);
    }
  }

  private static class MyPanel extends JPanel{
    private final Component myLeft;

    MyPanel(Component left) {
      super(new BorderLayout());
      myLeft = left;
    }

    @Override
    public Dimension getPreferredSize() {
      Dimension size = super.getPreferredSize();
      Dimension lSize = myLeft.getPreferredSize();
      return new Dimension(size.width + lSize.width, size.height);
    }

    public Dimension getTruePreferredSize() {
      return super.getPreferredSize();
    }
  }

  public static class MyTypedHandler extends TypedActionHandlerBase {
    public MyTypedHandler(@Nullable TypedActionHandler originalHandler) {
      super(originalHandler);
    }

    @Override
    public void execute(@NotNull Editor editor, char charTyped, @NotNull DataContext dataContext) {
      PerEditorSearchData data = editor.getUserData(SEARCH_DATA_IN_EDITOR_VIEW_KEY);
      if (data == null || data.hint == null){
        if (myOriginalHandler != null) myOriginalHandler.execute(editor, charTyped, dataContext);
      }
      else{
        LightweightHint hint = data.hint;
        PerHintSearchData hintData = hint.getUserData(SEARCH_DATA_IN_HINT_KEY);
        String text = hintData.label.getText();
        text += charTyped;
        hintData.label.setText(text);
        MyPanel comp = (MyPanel)hint.getComponent();
        if (comp.getTruePreferredSize().width > comp.getSize().width){
          Rectangle bounds = hint.getBounds();
          hint.pack();
          hint.updateLocation(bounds.x, bounds.y);
        }
        updatePosition(editor, hintData, false, false);
      }
    }
  }

  public static class BackSpaceHandler extends EditorActionHandler{
    private final EditorActionHandler myOriginalHandler;

    public BackSpaceHandler(EditorActionHandler originalAction) {
      myOriginalHandler = originalAction;
    }

    @Override
    public void doExecute(@NotNull Editor editor, Caret caret, DataContext dataContext) {
      PerEditorSearchData data = editor.getUserData(SEARCH_DATA_IN_EDITOR_VIEW_KEY);
      if (data == null || data.hint == null){
        myOriginalHandler.execute(editor, caret, dataContext);
      }
      else{
        LightweightHint hint = data.hint;
        PerHintSearchData hintData = hint.getUserData(SEARCH_DATA_IN_HINT_KEY);
        String text = hintData.label.getText();
        if (!text.isEmpty()){
          text = text.substring(0, text.length() - 1);
        }
        hintData.label.setText(text);
        updatePosition(editor, hintData, false, false);
      }
    }
  }

  public static class UpHandler extends EditorActionHandler {
    private final EditorActionHandler myOriginalHandler;

    public UpHandler(EditorActionHandler originalHandler) {
      myOriginalHandler = originalHandler;
    }

    @Override
    public void doExecute(@NotNull Editor editor, Caret caret, DataContext dataContext) {
      PerEditorSearchData data = editor.getUserData(SEARCH_DATA_IN_EDITOR_VIEW_KEY);
      if (data == null || data.hint == null){
        myOriginalHandler.execute(editor, caret, dataContext);
      }
      else{
        LightweightHint hint = data.hint;
        PerHintSearchData hintData = hint.getUserData(SEARCH_DATA_IN_HINT_KEY);
        String prefix = hintData.label.getText();
        if (prefix == null) return;
        hintData.searchStart = editor.getCaretModel().getOffset();
        if (hintData.searchStart == 0) return;
        hintData.searchStart--;
        updatePosition(editor, hintData, true, true);
        hintData.searchStart = editor.getCaretModel().getOffset();
      }
    }

    @Override
    public boolean isEnabled(Editor editor, DataContext dataContext) {
      PerEditorSearchData data = editor.getUserData(SEARCH_DATA_IN_EDITOR_VIEW_KEY);
      return data != null && data.hint != null || myOriginalHandler.isEnabled(editor, dataContext);
    }
  }

  public static class DownHandler extends EditorActionHandler {
    private final EditorActionHandler myOriginalHandler;

    public DownHandler(EditorActionHandler originalHandler) {
      myOriginalHandler = originalHandler;
    }

    @Override
    public void doExecute(@NotNull Editor editor, Caret caret, DataContext dataContext) {
      PerEditorSearchData data = editor.getUserData(SEARCH_DATA_IN_EDITOR_VIEW_KEY);
      if (data == null || data.hint == null){
        myOriginalHandler.execute(editor, caret, dataContext);
      }
      else{
        LightweightHint hint = data.hint;
        PerHintSearchData hintData = hint.getUserData(SEARCH_DATA_IN_HINT_KEY);
        String prefix = hintData.label.getText();
        if (prefix == null) return;
        hintData.searchStart = editor.getCaretModel().getOffset();
        if (hintData.searchStart == editor.getDocument().getTextLength()) return;
        hintData.searchStart++;
        updatePosition(editor, hintData, true, false);
        hintData.searchStart = editor.getCaretModel().getOffset();
      }
    }

    @Override
    public boolean isEnabled(Editor editor, DataContext dataContext) {
      PerEditorSearchData data = editor.getUserData(SEARCH_DATA_IN_EDITOR_VIEW_KEY);
      return data != null && data.hint != null || myOriginalHandler.isEnabled(editor, dataContext);
    }
  }
}
