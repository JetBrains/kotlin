// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.lookup.impl;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementAction;
import com.intellij.internal.statistic.service.fus.collectors.UIEventId;
import com.intellij.internal.statistic.service.fus.collectors.UIEventLogger;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.popup.ClosableByLeftArrow;
import com.intellij.util.ui.EmptyIcon;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;

/**
* @author peter
*/
public class LookupActionsStep extends BaseListPopupStep<LookupElementAction> implements ClosableByLeftArrow {
  private final LookupImpl myLookup;
  private final LookupElement myLookupElement;
  private final Icon myEmptyIcon;

  public LookupActionsStep(Collection<LookupElementAction> actions, LookupImpl lookup, LookupElement lookupElement) {
    super(null, new ArrayList<>(actions));
    myLookup = lookup;
    myLookupElement = lookupElement;

    int w = 0, h = 0;
    for (LookupElementAction action : actions) {
      final Icon icon = action.getIcon();
      if (icon != null) {
        w = Math.max(w, icon.getIconWidth());
        h = Math.max(h, icon.getIconHeight());
      }
    }
    myEmptyIcon = EmptyIcon.create(w, h);
  }

  @Override
  public PopupStep onChosen(LookupElementAction selectedValue, boolean finalChoice) {
    UIEventLogger.logUIEvent(UIEventId.LookupExecuteElementAction);

    final LookupElementAction.Result result = selectedValue.performLookupAction();
    if (result == LookupElementAction.Result.HIDE_LOOKUP) {
      myLookup.hideLookup(true);
    } else if (result == LookupElementAction.Result.REFRESH_ITEM) {
      myLookup.updateLookupWidth(myLookupElement);
      myLookup.requestResize();
      myLookup.refreshUi(false, true);
    } else if (result instanceof LookupElementAction.Result.ChooseItem) {
      myLookup.setCurrentItem(((LookupElementAction.Result.ChooseItem)result).item);
      CommandProcessor.getInstance().executeCommand(myLookup.getProject(), () -> myLookup.finishLookup(Lookup.AUTO_INSERT_SELECT_CHAR), null, null);
    }
    return FINAL_CHOICE;
  }

  @Override
  public Icon getIconFor(LookupElementAction aValue) {
    return LookupCellRenderer.augmentIcon(myLookup.getEditor(), aValue.getIcon(), myEmptyIcon);
  }

  @NotNull
  @Override
  public String getTextFor(LookupElementAction value) {
    return value.getText();
  }
}
