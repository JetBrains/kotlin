/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.codeInsight.completion.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author mike
 */
public class HippieWordCompletionHandler implements CodeInsightActionHandler {
  private static final Key<CompletionState> KEY_STATE = new Key<>("HIPPIE_COMPLETION_STATE");
  private final boolean myForward;

  public HippieWordCompletionHandler(boolean forward) {
    myForward = forward;
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull final Editor editor, @NotNull PsiFile file) {
    if (!EditorModificationUtil.requestWriting(editor)) {
      return;
    }

    int caretOffset = editor.getCaretModel().getOffset();
    if (editor.isViewer() || editor.getDocument().getRangeGuard(caretOffset, caretOffset) != null) {
      editor.getDocument().fireReadOnlyModificationAttempt();
      EditorModificationUtil.checkModificationAllowed(editor);
      return;
    }

    LookupManager.getInstance(project).hideActiveLookup();

    final CharSequence charsSequence = editor.getDocument().getCharsSequence();

    final CompletionData data = computeData(editor, charsSequence);

    final CompletionState completionState = getCompletionState(editor);

    String oldPrefix = completionState.oldPrefix;
    CompletionVariant lastProposedVariant = completionState.lastProposedVariant;
    boolean fromOtherFiles = completionState.fromOtherFiles;

    if (lastProposedVariant == null || oldPrefix == null ||
        !completionState.caretOffsets.equals(getCaretOffsets(editor)) ||
        completionState.lastModCount != editor.getDocument().getModificationStamp()) {
      //we are starting over
      oldPrefix = data.myPrefix;
      completionState.oldPrefix = oldPrefix;
      lastProposedVariant = null;
      fromOtherFiles = false;
    } else {
      data.startOffset = completionState.lastStartOffset;
    }

    CompletionVariant nextVariant = computeNextVariant(editor, oldPrefix, lastProposedVariant, data, file, fromOtherFiles, false);
    if (nextVariant == null) {
      insertStringForEachCaret(editor, oldPrefix, caretOffset - data.startOffset);
      editor.putUserData(KEY_STATE, null);
      return;
    }

    RangeMarker start = editor.getDocument().createRangeMarker(data.startOffset, data.startOffset);
    nextVariant.fastenBelts();
    try {
      insertStringForEachCaret(editor, nextVariant.variant, caretOffset - data.startOffset);
    }
    finally {
      nextVariant.unfastenBelts();
    }

    if (!start.isValid()) {
      editor.putUserData(KEY_STATE, null);
      return;
    }

    completionState.lastProposedVariant = nextVariant;
    completionState.lastStartOffset = start.getStartOffset();
    completionState.lastModCount = editor.getDocument().getModificationStamp();
    completionState.caretOffsets = getCaretOffsets(editor);
    completionState.fromOtherFiles = nextVariant.editor != editor;
    if (nextVariant.editor == editor) highlightWord(nextVariant, project);

    start.dispose();
  }

  private static void insertStringForEachCaret(final Editor editor, final String text, final int relativeOffset) {
    editor.getCaretModel().runForEachCaret(caret -> {
      int caretOffset = caret.getOffset();
      int startOffset = Math.max(0, caretOffset - relativeOffset);
      editor.getDocument().replaceString(startOffset, caretOffset, text);
      caret.moveToOffset(startOffset + text.length());
    });
  }  

  private static void highlightWord(final CompletionVariant variant, final Project project) {
    HighlightManager highlightManager = HighlightManager.getInstance(project);
    EditorColorsManager colorManager = EditorColorsManager.getInstance();
    TextAttributes attributes = colorManager.getGlobalScheme().getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES);
    highlightManager.addOccurrenceHighlight(variant.editor, variant.offset, variant.offset + variant.variant.length(), attributes,
                                            HighlightManager.HIDE_BY_ANY_KEY, null, null);
  }


  private static class CompletionData {
    public String myPrefix;
    public int startOffset;
  }

  @Nullable
  private CompletionVariant computeNextVariant(final Editor editor,
                                               @Nullable final String prefix,
                                               @Nullable CompletionVariant lastProposedVariant,
                                               final CompletionData data,
                                               PsiFile file,
                                               boolean includeWordsFromOtherFiles,
                                               boolean weAlreadyDoBestAttempt
  ) {
    final List<CompletionVariant> variants = computeVariants(editor, new CamelHumpMatcher(StringUtil.notNullize(prefix)), file, includeWordsFromOtherFiles);
    if (variants.isEmpty()) {
      return weAlreadyDoBestAttempt ? null:computeNextVariant(editor, prefix, null, data, file, !includeWordsFromOtherFiles, true);
    }

    if (lastProposedVariant != null) { // intern lastProposedVariant
      for (CompletionVariant variant : variants) {
        if (variant.variant.equals(lastProposedVariant.variant)) {
          if (lastProposedVariant.offset > data.startOffset && variant.offset > data.startOffset) lastProposedVariant = variant;
          if (lastProposedVariant.offset < data.startOffset && variant.offset < data.startOffset) lastProposedVariant = variant;
          if (includeWordsFromOtherFiles && lastProposedVariant.editor == variant.editor) lastProposedVariant = variant;
        }
      }
    }


    if (lastProposedVariant == null) {
      CompletionVariant result = null;

      if (myForward) {
        if (includeWordsFromOtherFiles) {
          return variants.get(variants.size() - 1);
        }
        for (CompletionVariant variant : variants) {
          if (variant.offset < data.startOffset) {
            result = variant;
          }
          else if (result == null) {
            result = variant;
            break;
          }
        }
      }
      else {
        if (includeWordsFromOtherFiles) {
          return variants.get(0);
        }
        for (CompletionVariant variant : variants) {
          if (variant.offset > data.startOffset) {
            return variant;
          }
        }

        return variants.iterator().next();
      }

      return result;
    }


    if (myForward) {
      CompletionVariant result = null;
      for (CompletionVariant variant : variants) {
        if (variant == lastProposedVariant) {
          if (result == null) {
            return computeNextVariant(editor, prefix, null, data, file, !includeWordsFromOtherFiles, true);
          }
          return result;
        }
        result = variant;
      }

      return variants.get(variants.size() - 1);
    }
    else {
      for (Iterator<CompletionVariant> i = variants.iterator(); i.hasNext();) {
        CompletionVariant variant = i.next();
        if (variant == lastProposedVariant) {
          if (i.hasNext()) {
            return i.next();
          }
          else {
            return computeNextVariant(editor, prefix, null, data, file, !includeWordsFromOtherFiles, true);
          }
        }
      }

    }

    return null;
  }

  public static class CompletionVariant {
    public final Editor editor;
    public final String variant;
    public int offset;
    private RangeMarker marker;

    public CompletionVariant(final Editor editor, final String variant, final int offset) {
      this.editor = editor;
      this.variant = variant;
      this.offset = offset;
    }

    public void fastenBelts() {
      marker = editor.getDocument().createRangeMarker(offset, offset);
    }

    public void unfastenBelts() {
      if (marker.isValid()) {
        offset = marker.getStartOffset();
        marker.dispose();
      }
      marker = null;
    }
  }

  private static boolean containsLettersOrDigits(CharSequence seq, int start, int end) {
    for (int i = start; i < end; i++) {
      if (Character.isLetterOrDigit(seq.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private static List<CompletionVariant> computeVariants(@NotNull final Editor editor,
                                                         CamelHumpMatcher matcher,
                                                         PsiFile file,
                                                         boolean includeWordsFromOtherFiles) {

    final ArrayList<CompletionVariant> words = new ArrayList<>();
    final List<CompletionVariant> afterWords = new ArrayList<>();

    if (includeWordsFromOtherFiles) {
      for(FileEditor fileEditor: FileEditorManager.getInstance(file.getProject()).getAllEditors()) {
        if (fileEditor instanceof TextEditor) {
          Editor anotherEditor = ((TextEditor)fileEditor).getEditor();
          if (anotherEditor != editor) {
            addWordsForEditor((EditorEx)anotherEditor, matcher, words, afterWords, false);
          }
        }
      }
    } else {
      addWordsForEditor((EditorEx)editor, matcher, words, afterWords, true);
    }

    Set<String> allWords = new HashSet<>();
    List<CompletionVariant> result = new ArrayList<>();

    Collections.reverse(words);

    for (CompletionVariant variant : words) {
      if (!allWords.contains(variant.variant)) {
        result.add(variant);
        allWords.add(variant.variant);
      }
    }

    Collections.reverse(result);

    allWords.clear();
    for (CompletionVariant variant : afterWords) {
      if (!allWords.contains(variant.variant)) {
        result.add(variant);
        allWords.add(variant.variant);
      }
    }

    return result;
  }

  private interface TokenProcessor {
    boolean processToken(int start, int end);
  }

  private static void addWordsForEditor(final EditorEx editor,
                                        final CamelHumpMatcher matcher,
                                        final List<CompletionVariant> words,
                                        final List<CompletionVariant> afterWords, boolean takeCaretsIntoAccount) {
    final CharSequence chars = editor.getDocument().getImmutableCharSequence();
    final int primaryCaretOffset;
    final int[] caretOffsets;
    if (takeCaretsIntoAccount) {
      CaretModel caretModel = editor.getCaretModel();
      primaryCaretOffset = caretModel.getOffset();
      caretOffsets = getCaretOffsets(caretModel);
    }
    else {
      primaryCaretOffset = 0;
      caretOffsets = new int[1];
    }
    TokenProcessor processor = new TokenProcessor() {
      @Override
      public boolean processToken(int start, int end) {
        for (int caretOffset : caretOffsets) {
          if (start <= caretOffset && end >= caretOffset) return true; //skip prefix itself
        }
        if (end - start > matcher.getPrefix().length()) {
          final String word = chars.subSequence(start, end).toString();
          if (matcher.isStartMatch(word)) {
            CompletionVariant v = new CompletionVariant(editor, word, start);
            if (end > primaryCaretOffset) {
              afterWords.add(v);
            }
            else {
              words.add(v);
            }
          }
        }
        return true;
      }
    };
    processWords(editor, 0, processor);
  }

  private static int[] getCaretOffsets(CaretModel caretModel) {
    int[] caretOffsets = new int[caretModel.getCaretCount()];
    int i = 0;
    for (Caret caret : caretModel.getAllCarets()) {
      caretOffsets[i++] = caret.getOffset();
    }
    return caretOffsets;
  }

  private static void processWords(Editor editor, int startOffset, TokenProcessor processor) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    HighlighterIterator iterator = ((EditorEx)editor).getHighlighter().createIterator(startOffset);
    while (!iterator.atEnd()) {
      int start = iterator.getStart();
      int end = iterator.getEnd();

      while (start < end) {
        int wordStart = start;
        while (wordStart < end && !isWordPart(chars.charAt(wordStart))) wordStart++;

        int wordEnd = wordStart;
        while (wordEnd < end && isWordPart(chars.charAt(wordEnd))) wordEnd++;

        if (wordEnd > wordStart && containsLettersOrDigits(chars, wordStart, wordEnd) && !processor.processToken(wordStart, wordEnd)) {
          return;
        }
        start = wordEnd + 1;
      }
      iterator.advance();
    }
  }

  private static boolean isWordPart(final char c) {
    return Character.isJavaIdentifierPart(c) || c == '-' || c == '*' ;
  }

  private static CompletionData computeData(final Editor editor, final CharSequence charsSequence) {
    final int offset = editor.getCaretModel().getOffset();

    final CompletionData data = new CompletionData();

    processWords(editor, Math.max(offset - 1, 0), new TokenProcessor() {
      @Override
      public boolean processToken(int start, int end) {
        if (start > offset) {
          return false;
        }
        if (end >= offset) {
          data.myPrefix = charsSequence.subSequence(start, offset).toString();
          data.startOffset = start;
          return false;
        }
        return true;
      }
    });

    if (data.myPrefix == null) {
      data.myPrefix = "";
      data.startOffset = offset;
    }
    return data;
  }

  private static CompletionState getCompletionState(Editor editor) {
    CompletionState state = editor.getUserData(KEY_STATE);
    if (state == null) {
      state = new CompletionState();
      editor.putUserData(KEY_STATE, state);
    }

    return state;
  }

  @NotNull
  private static List<Integer> getCaretOffsets(Editor editor) {
    return ContainerUtil.map(editor.getCaretModel().getAllCarets(), caret -> caret.getOffset());
  }

  private static class CompletionState {
    public String oldPrefix;
    public CompletionVariant lastProposedVariant;
    public boolean fromOtherFiles;
    int lastStartOffset;
    long lastModCount;
    List<Integer> caretOffsets = Collections.emptyList();
  }
}
