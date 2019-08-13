// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.formatting.FormatConstants;
import com.intellij.ide.DataManager;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.impl.TextChangeImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.WhiteSpaceFormattingStrategy;
import com.intellij.psi.formatter.WhiteSpaceFormattingStrategyFactory;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Encapsulates logic for processing {@link EditorSettings#isWrapWhenTypingReachesRightMargin(Project)} option.
 *
 * @author Denis Zhdanov
 */
public class AutoHardWrapHandler {

  /**
   * This key is used as a flag that indicates if {@code 'auto wrap line on typing'} activity is performed now.
   *
   * @see CodeStyleSettings#isWrapOnTyping(Language)
   */
  public static final Key<Boolean> AUTO_WRAP_LINE_IN_PROGRESS_KEY = new Key<>("AUTO_WRAP_LINE_IN_PROGRESS");

  private static final AutoHardWrapHandler INSTANCE = new AutoHardWrapHandler();

  /**
   * There is a possible case that the user configured editor to
   * {@link EditorSettings#isWrapWhenTypingReachesRightMargin(Project) wrap line on reaching right margin} and that he or she
   * types in the middle of the long line. One line part is cut from end and moved to the next line as a result. But the user
   * keeps typing and another part of the line should be moved to the next line then. We don't want to have a number of
   * such line endings to be located on distinct lines.
   * <p/>
   * Hence, we remember last auto-wrap change per-document and merge it with the new auto-wrap if necessary. Current collection
   * holds that {@code 'document -> last auto-wrap change'} mappings.
   */
  private final Map<Document, AutoWrapChange> myAutoWrapChanges = ContainerUtil.createWeakMap();

  public static AutoHardWrapHandler getInstance() {
    return INSTANCE;
  }

  /**
   * The user is allowed to configured IJ in a way that it automatically wraps line on right margin exceeding on typing
   * (check {@link EditorSettings#isWrapWhenTypingReachesRightMargin(Project)}).
   * <p/>
   * This method encapsulates that functionality, i.e. it performs the following logical actions:
   * <pre>
   * <ol>
   *   <li>Check if IJ is configured to perform automatic line wrapping on typing. Return in case of the negative answer;</li>
   *   <li>Check if right margin is exceeded. Return in case of the negative answer;</li>
   *   <li>Perform line wrapping;</li>
   * </ol>
   </pre>
   *
   * @param editor                          active editor
   * @param dataContext                     current data context
   * @param modificationStampBeforeTyping   document modification stamp before the current symbols typing
   */
  public void wrapLineIfNecessary(@NotNull Editor editor, @NotNull DataContext dataContext, long modificationStampBeforeTyping) {
    Project project = editor.getProject();
    Document document = editor.getDocument();
    AutoWrapChange change = myAutoWrapChanges.get(document);
    if (change != null) {
      change.charTyped(editor, modificationStampBeforeTyping);
    }

    // Return eagerly if we don't need to auto-wrap line, e.g. because of right margin exceeding.
    if (/*editor.isOneLineMode()
        || */project == null
        || !editor.getSettings().isWrapWhenTypingReachesRightMargin(project)
        || (TemplateManager.getInstance(project) != null && TemplateManager.getInstance(project).getActiveTemplate(editor) != null))
    {
      return;
    }

    CaretModel caretModel = editor.getCaretModel();
    int caretOffset = caretModel.getOffset();
    int line = document.getLineNumber(caretOffset);
    int startOffset = document.getLineStartOffset(line);
    int endOffset = document.getLineEndOffset(line);

    final CharSequence endOfString = document.getCharsSequence().subSequence(caretOffset, endOffset);
    final boolean endsWithSpaces = StringUtil.isEmptyOrSpaces(String.valueOf(endOfString));
    // Check if right margin is exceeded.
    int margin = editor.getSettings().getRightMargin(project);
    if (margin <= 0) {
      return;
    }

    LogicalPosition logEndLinePosition = editor.offsetToLogicalPosition(endOffset);
    if (margin >= logEndLinePosition.column) {
      if (change != null) {
        change.modificationStamp = document.getModificationStamp();
      }
      return;
    }

    // We assume that right margin is exceeded if control flow reaches this place. Hence, we define wrap position and perform
    // smart line break there.
    LineWrapPositionStrategy strategy = LanguageLineWrapPositionStrategy.INSTANCE.forEditor(editor);

    // There is a possible case that user starts typing in the middle of the long string. Hence, there is a possible case that
    // particular symbols were already wrapped because of typing. Example:
    //    a b c d e f g <caret>h i j k l m n o p| <- right margin
    // Suppose the user starts typing at caret:
    //    type '1': a b c d e f g 1<caret>h i j k l m n o | <- right margin
    //              p                                     | <- right margin
    //    type '2': a b c d e f g 12<caret>h i j k l m n o| <- right margin
    //                                                    | <- right margin
    //              p                                     | <- right margin
    //    type '3': a b c d e f g 123<caret>h i j k l m n | <- right margin
    //              o                                     | <- right margin
    //                                                    | <- right margin
    //              p                                     | <- right margin
    // We want to prevent such behavior, hence, we remove automatically generated wraps and wrap the line as a whole.
    if (change == null) {
      change = new AutoWrapChange();
      myAutoWrapChanges.put(document, change);
    }
    else {
      final int start = change.change.getStart();
      final int end = change.change.getEnd();
      if (!change.isEmpty() && start < end) {
        document.replaceString(start, end, change.change.getText());
      }
      change.reset();
    }
    change.update(editor);

    // Is assumed to be max possible number of characters inserted on the visual line with caret.
    int maxPreferredOffset = editor.logicalPositionToOffset(
      new LogicalPosition(caretModel.getLogicalPosition().line, margin - FormatConstants.getReservedLineWrapWidthInColumns(editor))
    );

    int wrapOffset = strategy.calculateWrapPosition(document, project, startOffset, endOffset, maxPreferredOffset, true, false);
    if (wrapOffset < 0) {
      return;
    }

    WhiteSpaceFormattingStrategy formattingStrategy = WhiteSpaceFormattingStrategyFactory.getStrategy(editor);
    if (wrapOffset <= startOffset || wrapOffset > maxPreferredOffset
        || formattingStrategy.check(document.getCharsSequence(), startOffset, wrapOffset) >= wrapOffset)
    {
      // Don't perform hard line wrapping if it doesn't makes sense (no point to wrap at first position and no point to wrap
      // on first non-white space symbol because wrapped part will have the same indent value).
      return;
    }

    final int[] wrapIntroducedSymbolsNumber = new int[1];
    final int[] caretOffsetDiff = new int[1];
    final int baseCaretOffset = caretModel.getOffset();
    DocumentListener listener = new DocumentListener() {
      @Override
      public void beforeDocumentChange(@NotNull DocumentEvent event) {
        if (event.getOffset() < baseCaretOffset + caretOffsetDiff[0]) {
          caretOffsetDiff[0] += event.getNewLength() - event.getOldLength();
        }

        if (autoFormatted(event)) {
          return;
        }
        wrapIntroducedSymbolsNumber[0] += event.getNewLength() - event.getOldLength();
      }

      private boolean autoFormatted(DocumentEvent event) {
        return event.getNewLength() <= event.getOldLength() && endsWithSpaces;
      }
    };

    caretModel.moveToOffset(wrapOffset);
    DataManager.getInstance().saveInDataContext(dataContext, AUTO_WRAP_LINE_IN_PROGRESS_KEY, true);
    document.addDocumentListener(listener);
    try {
      EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_ENTER).execute(editor, dataContext);
    }
    finally {
      DataManager.getInstance().saveInDataContext(dataContext, AUTO_WRAP_LINE_IN_PROGRESS_KEY, null);
      document.removeDocumentListener(listener);
    }

    change.modificationStamp = document.getModificationStamp();
    change.change.setStart(wrapOffset);
    change.change.setEnd(wrapOffset + wrapIntroducedSymbolsNumber[0]);

    caretModel.moveToOffset(baseCaretOffset + caretOffsetDiff[0]);
  }

  private static class AutoWrapChange {

    final TextChangeImpl change = new TextChangeImpl("", 0, 0);
    int visualLine;
    int logicalLine;
    long modificationStamp;

    void reset() {
      visualLine = -1;
      logicalLine = -1;
      change.setStart(0);
      change.setEnd(0);
    }

    void update(Editor editor) {
      modificationStamp = editor.getDocument().getModificationStamp();

      CaretModel caretModel = editor.getCaretModel();
      visualLine = caretModel.getVisualPosition().line;
      logicalLine = caretModel.getLogicalPosition().line;
    }

    void charTyped(Editor editor, long modificationStamp) {
      if (matches(editor.getCaretModel(), modificationStamp)) {
        this.modificationStamp = editor.getDocument().getModificationStamp();
        change.advance(1);
      }
      else {
        reset();
      }
    }

    boolean isEmpty() {
      return change.getDiff() == 0;
    }

    private boolean matches(CaretModel caretModel, long modificationStamp) {
      return this.modificationStamp == modificationStamp && caretModel.getOffset() <= change.getStart()
             && visualLine == caretModel.getVisualPosition().line && logicalLine == caretModel.getLogicalPosition().line;
    }

    @Override
    public String toString() {
      return "visual line: " + visualLine + ", logical line: " + logicalLine + ", modification stamp: " + modificationStamp
             + ", text change: " + change;
    }
  }

}
