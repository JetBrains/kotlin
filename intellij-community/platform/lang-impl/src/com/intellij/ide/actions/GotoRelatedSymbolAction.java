/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class GotoRelatedSymbolAction extends AnAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    PsiElement element = getContextElement(e.getDataContext());
    e.getPresentation().setEnabled(element != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();

    final PsiElement element = getContextElement(dataContext);
    if (element == null) return;

    // it's calculated in advance because `NavigationUtil.collectRelatedItems` might be
    // calculated under a cancellable progress, and we can't use the data context anymore,
    // since it can't be reused between swing events
    RelativePoint popupLocation = JBPopupFactory.getInstance().guessBestPopupLocation(dataContext);

    List<GotoRelatedItem> items = NavigationUtil.collectRelatedItems(element, dataContext);
    if (items.isEmpty()) {
      final JComponent label = HintUtil.createErrorLabel("No related symbols");
      label.setBorder(JBUI.Borders.empty(2, 7));
      JBPopupFactory.getInstance().createBalloonBuilder(label)
        .setFadeoutTime(3000)
        .setFillColor(HintUtil.getErrorColor())
        .createBalloon()
        .show(popupLocation, Balloon.Position.above);
      return;
    }

    if (items.size() == 1 && items.get(0).getElement() != null) {
      items.get(0).navigate();
      return;
    }
    NavigationUtil.getRelatedItemsPopup(items, "Choose Target").show(popupLocation);
  }

  @TestOnly
  @NotNull
  public static List<GotoRelatedItem> getItems(@NotNull PsiFile psiFile, @Nullable Editor editor, @Nullable DataContext dataContext) {
    return NavigationUtil.collectRelatedItems(getContextElement(psiFile, editor), dataContext);
  }

  @Nullable
  private static PsiElement getContextElement(@NotNull DataContext dataContext) {
    PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    if (file != null && editor != null) {
      return getContextElement(file, editor);
    }
    return element == null ? file : element;
  }

  @NotNull
  private static PsiElement getContextElement(@NotNull PsiFile psiFile, @Nullable Editor editor) {
    PsiElement contextElement = psiFile;
    if (editor != null) {
      PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
      if (element != null) {
        contextElement = element;
      }
    }
    return contextElement;
  }
}
