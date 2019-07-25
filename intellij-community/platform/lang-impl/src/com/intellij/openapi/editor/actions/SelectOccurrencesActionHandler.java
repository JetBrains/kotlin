/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.editor.actions;

import com.intellij.codeInsight.editorActions.SelectWordUtil;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.find.FindBundle;
import com.intellij.find.FindModel;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorLastActionTracker;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.LightweightHint;
import java.util.HashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;

abstract public class SelectOccurrencesActionHandler extends EditorActionHandler {

  private static final Key<Boolean> NOT_FOUND = Key.create("select.next.occurence.not.found");
  private static final Key<Boolean> WHOLE_WORDS = Key.create("select.next.occurence.whole.words");

  private static final Set<String> SELECT_ACTIONS = new HashSet<>(Arrays.asList(
    IdeActions.ACTION_SELECT_NEXT_OCCURENCE,
    IdeActions.ACTION_UNSELECT_PREVIOUS_OCCURENCE,
    IdeActions.ACTION_FIND_NEXT,
    IdeActions.ACTION_FIND_PREVIOUS
  ));

  protected static void setSelection(Editor editor, Caret caret, TextRange selectionRange) {
    EditorActionUtil.makePositionVisible(editor, selectionRange.getStartOffset());
    EditorActionUtil.makePositionVisible(editor, selectionRange.getEndOffset());
    caret.setSelection(selectionRange.getStartOffset(), selectionRange.getEndOffset());
  }

  protected static void showHint(final Editor editor) {
    String message = FindBundle.message("select.next.occurence.not.found.message");
    final LightweightHint hint = new LightweightHint(HintUtil.createInformationLabel(message));
    HintManagerImpl.getInstanceImpl().showEditorHint(hint,
                                                     editor,
                                                     HintManager.UNDER,
                                                     HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING,
                                                     0,
                                                     false);
  }

  protected static boolean getAndResetNotFoundStatus(Editor editor) {
    boolean status = editor.getUserData(NOT_FOUND) != null;
    editor.putUserData(NOT_FOUND, null);
    return status && isRepeatedActionInvocation();
  }

  protected static void setNotFoundStatus(Editor editor) {
    editor.putUserData(NOT_FOUND, Boolean.TRUE);
  }

  protected static boolean isWholeWordSearch(Editor editor) {
    if (!isRepeatedActionInvocation()) {
      editor.putUserData(WHOLE_WORDS, null);
    }
    Boolean value = editor.getUserData(WHOLE_WORDS);
    return value != null;
  }

  @Nullable
  protected static TextRange getSelectionRange(Editor editor, Caret caret) {
    return SelectWordUtil.getWordSelectionRange(editor.getDocument().getCharsSequence(),
                                                caret.getOffset(),
                                                SelectWordUtil.JAVA_IDENTIFIER_PART_CONDITION);
  }

  protected static void setWholeWordSearch(Editor editor, boolean isWholeWordSearch) {
    editor.putUserData(WHOLE_WORDS, isWholeWordSearch);
  }

  protected static boolean isRepeatedActionInvocation() {
    String lastActionId = EditorLastActionTracker.getInstance().getLastActionId();
    return SELECT_ACTIONS.contains(lastActionId);
  }

  protected static FindModel getFindModel(String text, boolean wholeWords) {
    FindModel model = new FindModel();
    model.setStringToFind(text);
    model.setCaseSensitive(true);
    model.setWholeWordsOnly(wholeWords);
    return model;
  }
}
