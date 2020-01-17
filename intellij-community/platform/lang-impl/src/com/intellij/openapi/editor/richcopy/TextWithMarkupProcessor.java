// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.richcopy;

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor;
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RawText;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.richcopy.SyntaxInfoBuilder.Context;
import com.intellij.openapi.editor.richcopy.SyntaxInfoBuilder.MyMarkupIterator;
import com.intellij.openapi.editor.richcopy.model.SyntaxInfo;
import com.intellij.openapi.editor.richcopy.settings.RichCopySettings;
import com.intellij.openapi.editor.richcopy.view.HtmlTransferableData;
import com.intellij.openapi.editor.richcopy.view.RawTextWithMarkup;
import com.intellij.openapi.editor.richcopy.view.RtfTransferableData;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiFile;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.editor.richcopy.SyntaxInfoBuilder.createMarkupIterator;

/**
 * Generates text with markup (in RTF and HTML formats) for interaction via clipboard with third-party applications.
 * <p>
 * Interoperability with the following applications was tested:
 * MS Office 2010 (Word, PowerPoint, Outlook), OpenOffice (Writer, Impress), Gmail, Mac TextEdit, Mac Mail, Mac Keynote.
 */
public class TextWithMarkupProcessor extends CopyPastePostProcessor<RawTextWithMarkup> {
  private static final Logger LOG = Logger.getInstance(TextWithMarkupProcessor.class);

  private List<RawTextWithMarkup> myResult;

  @NotNull
  @Override
  public List<RawTextWithMarkup> collectTransferableData(PsiFile file, Editor editor, int[] startOffsets, int[] endOffsets) {
    if (!RichCopySettings.getInstance().isEnabled() || !(editor instanceof EditorEx)) {
      return Collections.emptyList();
    }

    EditorHighlighter highlighter = null;
    EditorColorsScheme editorColorsScheme = editor.getColorsScheme();
    EditorColorsScheme schemeToUse = RichCopySettings.getInstance().getColorsScheme(editorColorsScheme);

    try {
      List<Caret> carets = editor.getCaretModel().getAllCarets();
      Caret firstCaret = carets.get(0);
      final int indentSymbolsToStrip;
      final int firstLineStartOffset;
      if (Registry.is("editor.richcopy.strip.indents") && carets.size() == 1) {
        Pair<Integer, Integer> p = calcIndentSymbolsToStrip(editor.getDocument(), firstCaret.getSelectionStart(), firstCaret.getSelectionEnd());
        firstLineStartOffset = p.first;
        indentSymbolsToStrip = p.second;
      }
      else {
        firstLineStartOffset = firstCaret.getSelectionStart();
        indentSymbolsToStrip = 0;
      }
      logInitial(editor, startOffsets, endOffsets, indentSymbolsToStrip, firstLineStartOffset);
      CharSequence text = editor.getDocument().getCharsSequence();
      highlighter = ((EditorEx)editor).getHighlighter();
      if (editorColorsScheme != schemeToUse) {
        highlighter.setColorScheme(schemeToUse);
      }
      MarkupModel markupModel = DocumentMarkupModel.forDocument(editor.getDocument(), file.getProject(), false);
      Context context = new Context(text, schemeToUse, indentSymbolsToStrip);
      int endOffset = 0;
      Caret prevCaret = null;

      for (Caret caret : carets) {
        int caretSelectionStart = caret.getSelectionStart();
        int caretSelectionEnd = caret.getSelectionEnd();
        int startOffsetToUse;
        int additionalShift = 0;
        if (caret == firstCaret) {
          startOffsetToUse = firstLineStartOffset;
        }
        else {
          startOffsetToUse = caretSelectionStart;
          assert prevCaret != null;
          String prevCaretSelectedText = prevCaret.getSelectedText();
          // Block selection fills short lines by white spaces
          int fillStringLength = prevCaretSelectedText == null ? 0 : prevCaretSelectedText.length() - (prevCaret.getSelectionEnd() - prevCaret.getSelectionStart());
          context.addCharacter(endOffset + fillStringLength);
          additionalShift = fillStringLength + 1;
        }
        context.reset(endOffset - caretSelectionStart + additionalShift);
        endOffset = caretSelectionEnd;
        prevCaret = caret;
        if (endOffset <= startOffsetToUse) {
          continue;
        }
        MyMarkupIterator markupIterator = createMarkupIterator(highlighter, text, schemeToUse, markupModel, startOffsetToUse, endOffset);
        try {
          context.iterate(markupIterator, endOffset);
        }
        finally {
          markupIterator.dispose();
        }
      }
      SyntaxInfo syntaxInfo = context.finish();
      logSyntaxInfo(syntaxInfo);

      createResult(syntaxInfo, editor);
      return ObjectUtils.notNull(myResult, Collections.emptyList());
    }
    catch (Throwable t) {
      // catching the exception so that the rest of copy/paste functionality can still work fine
      LOG.error("Error generating text with markup", t, new Attachment("highlighter.txt", String.valueOf(highlighter)));
    }
    finally {
      if (highlighter != null && editorColorsScheme != schemeToUse) {
        highlighter.setColorScheme(editorColorsScheme);
      }
    }
    return Collections.emptyList();
  }

  void createResult(SyntaxInfo syntaxInfo, Editor editor) {
    myResult = new ArrayList<>(2);
    myResult.add(new HtmlTransferableData(syntaxInfo, EditorUtil.getTabSize(editor)));
    myResult.add(new RtfTransferableData(syntaxInfo));
  }

  private void setRawText(String rawText) {
    if (myResult == null) {
      return;
    }
    for (RawTextWithMarkup data : myResult) {
      data.setRawText(rawText);
    }
    myResult = null;
  }

  private static void logInitial(@NotNull Editor editor,
                                 int @NotNull [] startOffsets,
                                 int @NotNull [] endOffsets,
                                 int indentSymbolsToStrip,
                                 int firstLineStartOffset) {
    if (!LOG.isDebugEnabled()) {
      return;
    }

    StringBuilder buffer = new StringBuilder();
    Document document = editor.getDocument();
    CharSequence text = document.getCharsSequence();
    for (int i = 0; i < startOffsets.length; i++) {
      int start = startOffsets[i];
      int lineStart = document.getLineStartOffset(document.getLineNumber(start));
      int end = endOffsets[i];
      int lineEnd = document.getLineEndOffset(document.getLineNumber(end));
      buffer.append("    region #").append(i).append(": ").append(start).append('-').append(end).append(", text at range ")
        .append(lineStart).append('-').append(lineEnd).append(": \n'").append(text.subSequence(lineStart, lineEnd)).append("'\n");
    }
    if (buffer.length() > 0) {
      buffer.setLength(buffer.length() - 1);
    }
    LOG.debug(String.format(
      "Preparing syntax-aware text. Given: %s selection, indent symbols to strip=%d, first line start offset=%d, selected text:%n%s",
      startOffsets.length > 1 ? "block" : "regular", indentSymbolsToStrip, firstLineStartOffset, buffer
    ));
  }

  private static void logSyntaxInfo(@NotNull SyntaxInfo info) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Constructed syntax info: " + info);
    }
  }

  private static Pair<Integer/* start offset to use */, Integer /* indent symbols to strip */> calcIndentSymbolsToStrip(
    @NotNull Document document, int startOffset, int endOffset) {
    int startLine = document.getLineNumber(startOffset);
    int endLine = document.getLineNumber(endOffset);
    CharSequence text = document.getCharsSequence();
    int maximumCommonIndent = Integer.MAX_VALUE;
    int firstLineStart = startOffset;
    int firstLineEnd = startOffset;
    for (int line = startLine; line <= endLine; line++) {
      int lineStartOffset = document.getLineStartOffset(line);
      int lineEndOffset = document.getLineEndOffset(line);
      if (line == startLine) {
        firstLineStart = lineStartOffset;
        firstLineEnd = lineEndOffset;
      }
      int nonWsOffset = lineEndOffset;
      for (int i = lineStartOffset; i < lineEndOffset && (i - lineStartOffset) < maximumCommonIndent && i < endOffset; i++) {
        char c = text.charAt(i);
        if (c != ' ' && c != '\t') {
          nonWsOffset = i;
          break;
        }
      }
      if (nonWsOffset >= lineEndOffset) {
        continue; // Blank line
      }
      int indent = nonWsOffset - lineStartOffset;
      maximumCommonIndent = Math.min(maximumCommonIndent, indent);
      if (maximumCommonIndent == 0) {
        break;
      }
    }
    int startOffsetToUse = Math.min(firstLineEnd, Math.max(startOffset, firstLineStart + maximumCommonIndent));
    return Pair.create(startOffsetToUse, maximumCommonIndent);
  }

  final static class RawTextSetter implements CopyPastePreProcessor {
    private final TextWithMarkupProcessor myProcessor;

    RawTextSetter() {
      myProcessor = CopyPastePostProcessor.EP_NAME.findExtensionOrFail(TextWithMarkupProcessor.class);
    }

    @Nullable
    @Override
    public String preprocessOnCopy(PsiFile file, int[] startOffsets, int[] endOffsets, String text) {
      myProcessor.setRawText(text);
      return null;
    }

    @NotNull
    @Override
    public String preprocessOnPaste(Project project, PsiFile file, Editor editor, String text, RawText rawText) {
      return text;
    }
  }
}
