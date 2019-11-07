// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.source.codeStyle;

import com.intellij.application.options.CodeStyle;
import com.intellij.formatting.*;
import com.intellij.ide.DataManager;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageFormatting;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.DocumentBasedFormattingModel;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.text.TextRangeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

public class CodeFormatterFacade {

  private static final Logger LOG = Logger.getInstance(CodeFormatterFacade.class);

  private static final String WRAP_LINE_COMMAND_NAME = "AutoWrapLongLine";

  /**
   * This key is used as a flag that indicates if {@code 'wrap long line during formatting'} activity is performed now.
   *
   * @see CommonCodeStyleSettings#WRAP_LONG_LINES
   */
  public static final Key<Boolean> WRAP_LONG_LINE_DURING_FORMATTING_IN_PROGRESS_KEY
    = new Key<>("WRAP_LONG_LINE_DURING_FORMATTING_IN_PROGRESS_KEY");

  private final CodeStyleSettings mySettings;
  private final FormatterTagHandler myTagHandler;
  private final int myRightMargin;
  private final boolean myCanChangeWhitespaceOnly;

  public CodeFormatterFacade(CodeStyleSettings settings, @Nullable Language language) {
    this(settings, language, false);
  }

  public CodeFormatterFacade(CodeStyleSettings settings,
                             @Nullable Language language,
                             boolean canChangeWhitespaceOnly) {
    mySettings = settings;
    myTagHandler = new FormatterTagHandler(settings);
    myRightMargin = mySettings.getRightMargin(language);
    myCanChangeWhitespaceOnly = canChangeWhitespaceOnly;
  }

  public ASTNode processElement(ASTNode element) {
    TextRange range = element.getTextRange();
    return processRange(element, range.getStartOffset(), range.getEndOffset());
  }

  public ASTNode processRange(final ASTNode element, final int startOffset, final int endOffset) {
    final PsiElement psiElement = SourceTreeToPsiMap.treeElementToPsi(element);
    assert psiElement != null;
    final PsiFile file = psiElement.getContainingFile();
    final Document document = file.getViewProvider().getDocument();

    PsiElement elementToFormat = document instanceof DocumentWindow ? InjectedLanguageManager
          .getInstance(file.getProject()).getTopLevelFile(file) : psiElement;
    final PsiFile fileToFormat = elementToFormat.getContainingFile();

    RangeMarker rangeMarker = null;
    final FormattingModelBuilder builder = LanguageFormatting.INSTANCE.forContext(fileToFormat);
    if (builder != null) {
      if (document != null && endOffset < document.getTextLength()) {
        rangeMarker = document.createRangeMarker(startOffset, endOffset);
      }

      TextRange range = preprocess(element, TextRange.create(startOffset, endOffset));
      if (document instanceof DocumentWindow) {
        DocumentWindow documentWindow = (DocumentWindow)document;
        range = documentWindow.injectedToHost(range);
      }

      //final SmartPsiElementPointer pointer = SmartPointerManager.getInstance(psiElement.getProject()).createSmartPsiElementPointer(psiElement);
      final FormattingModel model = CoreFormatterUtil.buildModel(builder, elementToFormat, range, mySettings, FormattingMode.REFORMAT);
      if (file.getTextLength() > 0) {
        try {
          final FormatTextRanges ranges = new FormatTextRanges(range, true);
          setDisabledRanges(fileToFormat,ranges);
          FormatterEx.getInstanceEx().format(
            model, mySettings, mySettings.getIndentOptionsByFile(fileToFormat, range), ranges
          );

          wrapLongLinesIfNecessary(file, document, startOffset, endOffset);
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }

      if (!psiElement.isValid()) {
        if (rangeMarker != null) {
          final PsiElement at = file.findElementAt(rangeMarker.getStartOffset());
          final PsiElement result = PsiTreeUtil.getParentOfType(at, psiElement.getClass(), false);
          assert result != null;
          rangeMarker.dispose();
          return result.getNode();
        } else {
          assert false;
        }
      }
//      return SourceTreeToPsiMap.psiElementToTree(pointer.getElement());
    }

    if (rangeMarker != null) {
      rangeMarker.dispose();
    }
    return element;
  }

  public void processText(@NotNull PsiFile file, final FormatTextRanges ranges, boolean doPostponedFormatting) {
    final Project project = file.getProject();
    Document document = file.getViewProvider().getDocument();
    final List<FormatTextRange> textRanges = ranges.getRanges();
    if (document instanceof DocumentWindow) {
      file = InjectedLanguageManager.getInstance(file.getProject()).getTopLevelFile(file);
      final DocumentWindow documentWindow = (DocumentWindow)document;
      for (FormatTextRange range : textRanges) {
        range.setTextRange(documentWindow.injectedToHost(range.getTextRange()));
      }
      document = documentWindow.getDelegate();
    }

    final FormattingModelBuilder builder = LanguageFormatting.INSTANCE.forContext(file);
    if (builder != null) {
      if (file.getTextLength() > 0) {
        LOG.assertTrue(document != null);
        ranges.setExtendedRanges(new FormattingRangesExtender(document, file).getExtendedRanges(ranges.getTextRanges()));
        try {
          ASTNode containingNode = findContainingNode(file, ranges.getBoundRange());
          if (containingNode != null) {
            for (FormatTextRange range : ranges.getRanges()) {
              TextRange rangeToUse = preprocess(containingNode, range.getTextRange());
              range.setTextRange(rangeToUse);
            }
          }
          if (doPostponedFormatting) {
            invokePostponedFormatting(file, document, textRanges);
          }
          if (FormattingProgressTask.FORMATTING_CANCELLED_FLAG.get()) {
            return;
          }

          TextRange formattingModelRange = ObjectUtils.notNull(ranges.getBoundRange(), file.getTextRange());

          final FormattingModel originalModel = CoreFormatterUtil.buildModel(builder, file, formattingModelRange, mySettings, FormattingMode.REFORMAT);
          final FormattingModel model = new DocumentBasedFormattingModel(originalModel,
                                                                         document,
                                                                         project, mySettings, file.getFileType(), file);

          FormatterEx formatter = FormatterEx.getInstanceEx();
          if (CodeStyleManager.getInstance(project).isSequentialProcessingAllowed()) {
            formatter.setProgressTask(new FormattingProgressTask(project, file, document));
          }

          CommonCodeStyleSettings.IndentOptions indentOptions =
            mySettings.getIndentOptionsByFile(file, textRanges.size() == 1 ? textRanges.get(0).getTextRange() : null);

          setDisabledRanges(file, ranges);
          formatter.format(model, mySettings, indentOptions, ranges);
          for (FormatTextRange range : textRanges) {
            TextRange textRange = range.getTextRange();
            wrapLongLinesIfNecessary(file, document, textRange.getStartOffset(), textRange.getEndOffset());
          }
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }
    }
  }

  private void setDisabledRanges(@NotNull PsiFile file, FormatTextRanges ranges) {
    final Iterable<TextRange> excludedRangesIterable = TextRangeUtil.excludeRanges(
      file.getTextRange(), myTagHandler.getEnabledRanges(file.getNode(), file.getTextRange()));
    ranges.setDisabledRanges((Collection<TextRange>)excludedRangesIterable);
  }

  private static void invokePostponedFormatting(@NotNull PsiFile file,
                                                Document document,
                                                List<FormatTextRange> textRanges) {
    RangeMarker[] markers = new RangeMarker[textRanges.size()];
    int i = 0;
    for (FormatTextRange range : textRanges) {
      TextRange textRange = range.getTextRange();
      int start = textRange.getStartOffset();
      int end = textRange.getEndOffset();
      if (start >= 0 && end > start && end <= document.getTextLength()) {
        markers[i] = document.createRangeMarker(textRange);
        markers[i].setGreedyToLeft(true);
        markers[i].setGreedyToRight(true);
        i++;
      }
    }
    final PostprocessReformattingAspect component = file.getProject().getComponent(PostprocessReformattingAspect.class);
    FormattingProgressTask.FORMATTING_CANCELLED_FLAG.set(false);
    component.doPostponedFormatting(file.getViewProvider());
    i = 0;
    for (FormatTextRange range : textRanges) {
      RangeMarker marker = markers[i];
      if (marker != null) {
        range.setTextRange(TextRange.create(marker));
        marker.dispose();
      }
      i++;
    }
  }

  @Nullable
  static ASTNode findContainingNode(@NotNull PsiFile file, @Nullable TextRange range) {
    Language language = file.getLanguage();
    if (range == null) return null;
    final FileViewProvider viewProvider = file.getViewProvider();
    final PsiElement startElement = viewProvider.findElementAt(range.getStartOffset(), language);
    final PsiElement endElement = viewProvider.findElementAt(range.getEndOffset() - 1, language);
    final PsiElement commonParent = startElement != null && endElement != null ?
                                    PsiTreeUtil.findCommonParent(startElement, endElement) :
                                    null;
    ASTNode node = null;
    if (commonParent != null) {
      node = commonParent.getNode();
      // Find the topmost parent with the same range.
      ASTNode parent = node.getTreeParent();
      while (parent != null && parent.getTextRange().equals(commonParent.getTextRange())) {
        node = parent;
        parent = parent.getTreeParent();
      }
    }
    if (node == null) {
      node = file.getNode();
    }
    return node;
  }

  private TextRange preprocess(@NotNull final ASTNode node, @NotNull TextRange range) {
    TextRange result = range;
    PsiElement psi = node.getPsi();
    if (!psi.isValid()) {
      return result;
    }

    PsiFile file = psi.getContainingFile();

    // We use a set here because we encountered a situation when more than one PSI leaf points to the same injected fragment
    // (at least for sql injected into sql).
    final LinkedHashSet<TextRange> injectedFileRangesSet = new LinkedHashSet<>();

    if (!psi.getProject().isDefault()) {
      List<DocumentWindow> injectedDocuments = InjectedLanguageManager.getInstance(file.getProject()).getCachedInjectedDocumentsInRange(file, file.getTextRange());
      if (!injectedDocuments.isEmpty()) {
        for (DocumentWindow injectedDocument : injectedDocuments) {
          injectedFileRangesSet.add(TextRange.from(injectedDocument.injectedToHost(0), injectedDocument.getTextLength()));
        }
      }
      else {
        Collection<PsiLanguageInjectionHost> injectionHosts = collectInjectionHosts(file, range);
        PsiLanguageInjectionHost.InjectedPsiVisitor visitor = (injectedPsi, places) -> {
          for (PsiLanguageInjectionHost.Shred place : places) {
            Segment rangeMarker = place.getHostRangeMarker();
            injectedFileRangesSet.add(TextRange.create(rangeMarker.getStartOffset(), rangeMarker.getEndOffset()));
          }
        };
        for (PsiLanguageInjectionHost host : injectionHosts) {
          InjectedLanguageManager.getInstance(file.getProject()).enumerate(host, visitor);
        }
      }
    }

    if (!injectedFileRangesSet.isEmpty()) {
      List<TextRange> ranges = new ArrayList<>(injectedFileRangesSet);
      Collections.reverse(ranges);
      for (TextRange injectedFileRange : ranges) {
        int startHostOffset = injectedFileRange.getStartOffset();
        int endHostOffset = injectedFileRange.getEndOffset();
        if (startHostOffset >= range.getStartOffset() && endHostOffset <= range.getEndOffset()) {
          PsiFile injected = InjectedLanguageUtil.findInjectedPsiNoCommit(file, startHostOffset);
          if (injected != null) {
            final TextRange initialInjectedRange = TextRange.create(0, injected.getTextLength());
            TextRange injectedRange = initialInjectedRange;
            for (PreFormatProcessor processor : PreFormatProcessor.EP_NAME.getExtensionList()) {
              if (processor.changesWhitespacesOnly() || !myCanChangeWhitespaceOnly) {
                injectedRange = processor.process(injected.getNode(), injectedRange);
              }
            }

            // Allow only range expansion (not reduction) for injected context.
            if ((initialInjectedRange.getStartOffset() > injectedRange.getStartOffset() && initialInjectedRange.getStartOffset() > 0)
                || (initialInjectedRange.getEndOffset() < injectedRange.getEndOffset()
                    && initialInjectedRange.getEndOffset() < injected.getTextLength()))
            {
              range = TextRange.create(
                range.getStartOffset() + injectedRange.getStartOffset() - initialInjectedRange.getStartOffset(),
                range.getEndOffset() + initialInjectedRange.getEndOffset() - injectedRange.getEndOffset());
            }
          }
        }
      }
    }

    if (!mySettings.FORMATTER_TAGS_ENABLED) {
      for (PreFormatProcessor processor: PreFormatProcessor.EP_NAME.getExtensionList()) {
        if (processor.changesWhitespacesOnly() || !myCanChangeWhitespaceOnly) {
          result = processor.process(node, result);
        }
      }
    }
    else {
      result = preprocessEnabledRanges(node, result);
    }

    return result;
  }

  private TextRange preprocessEnabledRanges(@NotNull final ASTNode node, @NotNull TextRange range) {
    TextRange result = TextRange.create(range.getStartOffset(), range.getEndOffset());
    List<TextRange> enabledRanges = myTagHandler.getEnabledRanges(node, result);
    int delta = 0;
    for (TextRange enabledRange : enabledRanges) {
      enabledRange = enabledRange.shiftRight(delta);
      for (PreFormatProcessor processor : PreFormatProcessor.EP_NAME.getExtensionList()) {
        if (processor.changesWhitespacesOnly() || !myCanChangeWhitespaceOnly) {
          TextRange processedRange = processor.process(node, enabledRange);
          delta += processedRange.getLength() - enabledRange.getLength();
        }
      }
    }
    result = result.grown(delta);
    return result;
  }

  @NotNull
  private static Collection<PsiLanguageInjectionHost> collectInjectionHosts(@NotNull PsiFile file, @NotNull TextRange range) {
    Stack<PsiElement> toProcess = new Stack<>();
    for (PsiElement e = file.findElementAt(range.getStartOffset()); e != null; e = e.getNextSibling()) {
      if (e.getTextRange().getStartOffset() >= range.getEndOffset()) {
        break;
      }
      toProcess.push(e);
    }
    if (toProcess.isEmpty()) {
      return Collections.emptySet();
    }
    Set<PsiLanguageInjectionHost> result = null;
    while (!toProcess.isEmpty()) {
      PsiElement e = toProcess.pop();
      if (e instanceof PsiLanguageInjectionHost) {
        if (result == null) {
          result = new HashSet<>();
        }
        result.add((PsiLanguageInjectionHost)e);
      }
      else {
        for (PsiElement child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
          if (e.getTextRange().getStartOffset() >= range.getEndOffset()) {
            break;
          }
          toProcess.push(child);
        }
      }
    }
    return result == null ? Collections.emptySet() : result;
  }


  /**
   * Inspects all lines of the given document and wraps all of them that exceed {@link CodeStyleSettings#getRightMargin(Language)}
   * right margin}.
   * <p/>
   * I.e. the algorithm is to do the following for every line:
   * <p/>
   * <pre>
   * <ol>
   *   <li>
   *      Check if the line exceeds {@link CodeStyleSettings#getRightMargin(Language)}  right margin}. Go to the next line in the case of
   *      negative answer;
   *   </li>
   *   <li>Determine line wrap position; </li>
   *   <li>
   *      Perform 'smart wrap', i.e. not only wrap the line but insert additional characters over than line feed if necessary.
   *      For example consider that we wrap a single-line comment - we need to insert comment symbols on a start of the wrapped
   *      part as well. Generally, we get the same behavior as during pressing 'Enter' at wrap position during editing document;
   *   </li>
   * </ol>
   </pre>
   *
   * @param file        file that holds parsed document tree
   * @param document    target document
   * @param startOffset start offset of the first line to check for wrapping (inclusive)
   * @param endOffset   end offset of the first line to check for wrapping (exclusive)
   */
  private void wrapLongLinesIfNecessary(@NotNull final PsiFile file, @Nullable final Document document, final int startOffset,
                                        final int endOffset)
  {
    if (!mySettings.getCommonSettings(file.getLanguage()).WRAP_LONG_LINES ||
        PostprocessReformattingAspect.getInstance(file.getProject()).isViewProviderLocked(file.getViewProvider()) ||
        document == null) {
      return;
    }

    FormatterTagHandler formatterTagHandler = new FormatterTagHandler(CodeStyle.getSettings(file));
    List<TextRange> enabledRanges = formatterTagHandler.getEnabledRanges(file.getNode(), new TextRange(startOffset, endOffset));

    final VirtualFile vFile = FileDocumentManager.getInstance().getFile(document);
    if ((vFile == null || vFile instanceof LightVirtualFile) && !ApplicationManager.getApplication().isUnitTestMode()) {
      // we assume that control flow reaches this place when the document is backed by a "virtual" file so any changes made by
      // a formatter affect only PSI and it is out of sync with a document text
      return;
    }

    Editor editor = PsiUtilBase.findEditor(file);
    EditorFactory editorFactory = null;
    if (editor == null) {
      if (!ApplicationManager.getApplication().isDispatchThread()) {
        return;
      }
      editorFactory = EditorFactory.getInstance();
      editor = editorFactory.createEditor(document, file.getProject(), file.getVirtualFile(), false);
    }
    try {
      final Editor editorToUse = editor;
      ApplicationManager.getApplication().runWriteAction(() -> {
        final CaretModel caretModel = editorToUse.getCaretModel();
        final int caretOffset = caretModel.getOffset();
        final RangeMarker caretMarker = editorToUse.getDocument().createRangeMarker(caretOffset, caretOffset);
        doWrapLongLinesIfNecessary(editorToUse, file.getProject(), editorToUse.getDocument(), startOffset, endOffset, enabledRanges);
        if (caretMarker.isValid() && caretModel.getOffset() != caretMarker.getStartOffset()) {
          caretModel.moveToOffset(caretMarker.getStartOffset());
        }
      });
    }
    finally {
      PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());
      if (documentManager.isUncommited(document)) documentManager.commitDocument(document);
      if (editorFactory != null) {
        editorFactory.releaseEditor(editor);
      }
    }
  }

  public void doWrapLongLinesIfNecessary(@NotNull final Editor editor, @NotNull final Project project, @NotNull Document document,
                                         int startOffset, int endOffset, List<? extends TextRange> enabledRanges) {
    // Normalization.
    int startOffsetToUse = Math.min(document.getTextLength(), Math.max(0, startOffset));
    int endOffsetToUse = Math.min(document.getTextLength(), Math.max(0, endOffset));

    LineWrapPositionStrategy strategy = LanguageLineWrapPositionStrategy.INSTANCE.forEditor(editor);
    CharSequence text = document.getCharsSequence();
    int startLine = document.getLineNumber(startOffsetToUse);
    int endLine = document.getLineNumber(Math.max(0, endOffsetToUse - 1));
    int maxLine = Math.min(document.getLineCount(), endLine + 1);
    int tabSize = EditorUtil.getTabSize(editor);
    if (tabSize <= 0) {
      tabSize = 1;
    }
    int spaceSize = EditorUtil.getSpaceWidth(Font.PLAIN, editor);
    int[] shifts = new int[2];
    // shifts[0] - lines shift.
    // shift[1] - offset shift.
    int cumulativeShift = 0;

    for (int line = startLine; line < maxLine; line++) {
      int startLineOffset = document.getLineStartOffset(line);
      int endLineOffset = document.getLineEndOffset(line);
      if (!canWrapLine(Math.max(startOffsetToUse, startLineOffset),
                       Math.min(endOffsetToUse, endLineOffset),
                       cumulativeShift,
                       enabledRanges)) {
        continue;
      }

      final int preferredWrapPosition
        = calculatePreferredWrapPosition(editor, text, tabSize, spaceSize, startLineOffset, endLineOffset, endOffsetToUse);

      if (preferredWrapPosition < 0 || preferredWrapPosition >= endLineOffset) {
        continue;
      }
      if (preferredWrapPosition >= endOffsetToUse) {
        return;
      }

      // We know that current line exceeds right margin if control flow reaches this place, so, wrap it.
      int wrapOffset = strategy.calculateWrapPosition(
        document, editor.getProject(), Math.max(startLineOffset, startOffsetToUse), Math.min(endLineOffset, endOffsetToUse),
        preferredWrapPosition, false, false
      );
      if (wrapOffset < 0 // No appropriate wrap position is found.
          // No point in splitting line when its left part contains only white spaces, example:
          //    line start -> |                   | <- right margin
          //                  |   aaaaaaaaaaaaaaaa|aaaaaaaaaaaaaaaaaaaa() <- don't want to wrap this line even if it exceeds right margin
          || CharArrayUtil.shiftBackward(text, startLineOffset, wrapOffset - 1, " \t") < startLineOffset) {
        continue;
      }

      // Move caret to the target position and emulate pressing <enter>.
      editor.getCaretModel().moveToOffset(wrapOffset);
      emulateEnter(editor, project, shifts);

      //If number of inserted symbols on new line after wrapping more or equal then symbols left on previous line
      //there was no point to wrapping it, so reverting to before wrapping version
      if (shifts[1] - 1 >= wrapOffset - startLineOffset) {
        document.deleteString(wrapOffset, wrapOffset + shifts[1]);
      }
      else {
        // We know that number of lines is just increased, hence, update the data accordingly.
        maxLine += shifts[0];
        endOffsetToUse += shifts[1];
        cumulativeShift += shifts[1];
      }

    }
  }

  private static boolean canWrapLine(int startOffset, int endOffset, int offsetShift, @NotNull List<? extends TextRange> enabledRanges) {
    for (TextRange range : enabledRanges)  {
      if (range.containsOffset(startOffset - offsetShift) && range.containsOffset(endOffset - offsetShift)) return true;
    }
    return false;
  }

  /**
   * Emulates pressing {@code Enter} at current caret position.
   *
   * @param editor       target editor
   * @param project      target project
   * @param shifts       two-elements array which is expected to be filled with the following info:
   *                       1. The first element holds added lines number;
   *                       2. The second element holds added symbols number;
   */
  private static void emulateEnter(@NotNull final Editor editor, @NotNull Project project, int[] shifts) {
    final DataContext dataContext = prepareContext(editor.getComponent(), project);
    int caretOffset = editor.getCaretModel().getOffset();
    Document document = editor.getDocument();
    SelectionModel selectionModel = editor.getSelectionModel();
    int startSelectionOffset = 0;
    int endSelectionOffset = 0;
    boolean restoreSelection = selectionModel.hasSelection();
    if (restoreSelection) {
      startSelectionOffset = selectionModel.getSelectionStart();
      endSelectionOffset = selectionModel.getSelectionEnd();
      selectionModel.removeSelection();
    }
    int textLengthBeforeWrap = document.getTextLength();
    int lineCountBeforeWrap = document.getLineCount();

    DataManager.getInstance().saveInDataContext(dataContext, WRAP_LONG_LINE_DURING_FORMATTING_IN_PROGRESS_KEY, true);
    CommandProcessor commandProcessor = CommandProcessor.getInstance();
    try {
      Runnable command =
        () -> EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_ENTER).execute(editor, dataContext);
      if (commandProcessor.getCurrentCommand() == null) {
        commandProcessor.executeCommand(editor.getProject(), command, WRAP_LINE_COMMAND_NAME, null);
      }
      else {
        command.run();
      }
    }
    finally {
      DataManager.getInstance().saveInDataContext(dataContext, WRAP_LONG_LINE_DURING_FORMATTING_IN_PROGRESS_KEY, null);
    }
    int symbolsDiff = document.getTextLength() - textLengthBeforeWrap;
    if (restoreSelection) {
      int newSelectionStart = startSelectionOffset;
      int newSelectionEnd = endSelectionOffset;
      if (startSelectionOffset >= caretOffset) {
        newSelectionStart += symbolsDiff;
      }
      if (endSelectionOffset >= caretOffset) {
        newSelectionEnd += symbolsDiff;
      }
      selectionModel.setSelection(newSelectionStart, newSelectionEnd);
    }
    shifts[0] = document.getLineCount() - lineCountBeforeWrap;
    shifts[1] = symbolsDiff;
  }

  /**
   * Checks if it's worth to try to wrap target line (it's long enough) and tries to calculate preferred wrap position.
   *
   * @param editor                target editor
   * @param text                  text contained at the given editor
   * @param tabSize               tab space to use (number of visual columns occupied by a tab)
   * @param spaceSize             space width in pixels
   * @param startLineOffset       start offset of the text line to process
   * @param endLineOffset         end offset of the text line to process
   * @param targetRangeEndOffset  target text region's end offset
   * @return                      negative value if no wrapping should be performed for the target line;
   *                              preferred wrap position otherwise
   */
  private int calculatePreferredWrapPosition(@NotNull Editor editor,
                                             @NotNull CharSequence text,
                                             int tabSize,
                                             int spaceSize,
                                             int startLineOffset,
                                             int endLineOffset,
                                             int targetRangeEndOffset) {
    boolean hasTabs = false;
    boolean canOptimize = true;
    boolean hasNonSpaceSymbols = false;
    loop:
    for (int i = startLineOffset; i < Math.min(endLineOffset, targetRangeEndOffset); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '\t': {
          hasTabs = true;
          if (hasNonSpaceSymbols) {
            canOptimize = false;
            break loop;
          }
        }
        case ' ': break;
        default: hasNonSpaceSymbols = true;
      }
    }

    int reservedWidthInColumns = FormatConstants.getReservedLineWrapWidthInColumns(editor);

    if (!hasTabs) {
      return wrapPositionForTextWithoutTabs(startLineOffset, endLineOffset, targetRangeEndOffset, reservedWidthInColumns);
    }
    else if (canOptimize) {
      return wrapPositionForTabbedTextWithOptimization(text, tabSize, startLineOffset, endLineOffset, targetRangeEndOffset,
                                                       reservedWidthInColumns);
    }
    else {
      return wrapPositionForTabbedTextWithoutOptimization(editor, text, spaceSize, startLineOffset, endLineOffset, targetRangeEndOffset,
                                                          reservedWidthInColumns);
    }
  }

  private int wrapPositionForTextWithoutTabs(int startLineOffset, int endLineOffset, int targetRangeEndOffset, int reservedWidthInColumns) {
    if (Math.min(endLineOffset, targetRangeEndOffset) - startLineOffset > myRightMargin) {
      return startLineOffset + myRightMargin - reservedWidthInColumns;
    }
    return -1;
  }

  private int wrapPositionForTabbedTextWithOptimization(@NotNull CharSequence text,
                                                        int tabSize,
                                                        int startLineOffset,
                                                        int endLineOffset,
                                                        int targetRangeEndOffset,
                                                        int reservedWidthInColumns)
  {
    int width = 0;
    int symbolWidth;
    int result = Integer.MAX_VALUE;
    boolean wrapLine = false;
    for (int i = startLineOffset; i < Math.min(endLineOffset, targetRangeEndOffset); i++) {
      char c = text.charAt(i);
      symbolWidth = c == '\t' ? tabSize - (width % tabSize) : 1;
      if (width + symbolWidth + reservedWidthInColumns >= myRightMargin
          && (Math.min(endLineOffset, targetRangeEndOffset) - i) >= reservedWidthInColumns)
      {
        // Remember preferred position.
        result = i - 1;
      }
      if (width + symbolWidth >= myRightMargin) {
        wrapLine = true;
        break;
      }
      width += symbolWidth;
    }
    return wrapLine ? result : -1;
  }

  private int wrapPositionForTabbedTextWithoutOptimization(@NotNull Editor editor,
                                                           @NotNull CharSequence text,
                                                           int spaceSize,
                                                           int startLineOffset,
                                                           int endLineOffset,
                                                           int targetRangeEndOffset,
                                                           int reservedWidthInColumns)
  {
    int width = 0;
    int x = 0;
    int newX;
    int symbolWidth;
    int result = Integer.MAX_VALUE;
    boolean wrapLine = false;
    for (int i = startLineOffset; i < Math.min(endLineOffset, targetRangeEndOffset); i++) {
      char c = text.charAt(i);
      if (c == '\t') {
        newX = EditorUtil.nextTabStop(x, editor);
        int diffInPixels = newX - x;
        symbolWidth = diffInPixels / spaceSize;
        if (diffInPixels % spaceSize > 0) {
          symbolWidth++;
        }
      }
      else {
        newX = x + EditorUtil.charWidth(c, Font.PLAIN, editor);
        symbolWidth = 1;
      }
      if (width + symbolWidth + reservedWidthInColumns >= myRightMargin
          && (Math.min(endLineOffset, targetRangeEndOffset) - i) >= reservedWidthInColumns)
      {
        result = i - 1;
      }
      if (width + symbolWidth >= myRightMargin) {
        wrapLine = true;
        break;
      }
      x = newX;
      width += symbolWidth;
    }
    return wrapLine ? result : -1;
  }

  @NotNull
  private static DataContext prepareContext(@NotNull Component component, @NotNull final Project project) {
    // There is a possible case that formatting is performed from project view and editor is not opened yet. The problem is that
    // its data context doesn't contain information about project then. So, we explicitly support that here (see IDEA-72791).
    final DataContext baseDataContext = DataManager.getInstance().getDataContext(component);
    return new DelegatingDataContext(baseDataContext) {
      @Override
      public Object getData(@NotNull @NonNls String dataId) {
        Object result = baseDataContext.getData(dataId);
        if (result == null && CommonDataKeys.PROJECT.is(dataId)) {
          result = project;
        }
        return result;
      }
    };
  }

  private static class DelegatingDataContext implements DataContext, UserDataHolder {

    private final DataContext myDataContextDelegate;
    private final UserDataHolder myDataHolderDelegate;

    DelegatingDataContext(DataContext delegate) {
      myDataContextDelegate = delegate;
      if (delegate instanceof UserDataHolder) {
        myDataHolderDelegate = (UserDataHolder)delegate;
      }
      else {
        myDataHolderDelegate = null;
      }
    }

    @Override
    public Object getData(@NotNull @NonNls String dataId) {
      return myDataContextDelegate.getData(dataId);
    }

    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
      return myDataHolderDelegate == null ? null : myDataHolderDelegate.getUserData(key);
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
      if (myDataHolderDelegate != null) {
        myDataHolderDelegate.putUserData(key, value);
      }
    }
  }
}

