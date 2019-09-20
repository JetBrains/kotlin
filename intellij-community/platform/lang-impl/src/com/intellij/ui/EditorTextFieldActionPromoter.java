/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ui;

import com.intellij.openapi.actionSystem.ActionPromoter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actions.TextComponentEditorAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class EditorTextFieldActionPromoter implements ActionPromoter {
  /**
   * Encapsulates sorting rule that defines what editor actions have precedence to non-editor actions. Current approach is that
   * we want to process text processing-oriented editor actions with higher priority than non-editor actions and all
   * other editor actions with lower priority.
   * <p/>
   * Rationale: there is at least one commit-specific action that is mapped to the editor action by default
   * ({@code 'show commit messages history'} vs {@code 'scroll to center'}). We want to process the former on target
   * short key triggering. Another example is that {@code 'Ctrl+Shift+Right/Left Arrow'} shortcut is bound to
   * {@code 'expand/reduce selection by word'} editor action and {@code 'change dialog width'} non-editor action
   * and we want to use the first one.
   */
  private static final Comparator<AnAction> ACTIONS_COMPARATOR = (o1, o2) -> {
    boolean textFieldAction1 = o1 instanceof TextComponentEditorAction;
    boolean textFieldAction2 = o2 instanceof TextComponentEditorAction;
    boolean plainEditorAction1 = o1 instanceof EditorAction && !textFieldAction1;
    boolean plainEditorAction2 = o2 instanceof EditorAction && !textFieldAction2;
    if (textFieldAction1 && plainEditorAction2) return -1;
    if (textFieldAction2 && plainEditorAction1) return 1;
    return 0;
  };

  @Override
  public List<AnAction> promote(List<AnAction> actions, DataContext context) {
    ArrayList<AnAction> result = new ArrayList<>(actions);
    Collections.sort(result, ACTIONS_COMPARATOR);
    return result;
  }
}
