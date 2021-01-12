// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui;

import com.intellij.openapi.actionSystem.ActionPromoter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actions.TextComponentEditorAction;

import java.util.ArrayList;
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
    result.sort(ACTIONS_COMPARATOR);
    return result;
  }
}
