// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.codeInsight.hint.actions.NextParameterAction;
import com.intellij.openapi.actionSystem.ActionPromoter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.actions.TabAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TabActionsPromoter implements ActionPromoter {
  @Override
  public List<AnAction> promote(List<AnAction> actions, DataContext context) {
    List<AnAction> newList = new ArrayList<>(actions);
    newList.sort(Comparator.comparingInt(action -> {
      if (action instanceof BraceOrQuoteOutAction) return 0;
      else if (action instanceof TabAction || action instanceof NextParameterAction) return 1;
      else return -1;
    }));
    return newList;
  }
}
