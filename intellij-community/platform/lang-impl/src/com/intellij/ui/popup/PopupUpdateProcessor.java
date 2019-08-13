// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ui.popup;

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.lookup.*;
import com.intellij.ide.util.gotoByName.QuickSearchComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 */
public abstract class PopupUpdateProcessor extends PopupUpdateProcessorBase {

  private final Project myProject;

  protected PopupUpdateProcessor(Project project) {
    myProject = project;
  }

  @Override
  public void beforeShown(@NotNull final LightweightWindowEvent windowEvent) {
    final Lookup activeLookup = LookupManager.getInstance(myProject).getActiveLookup();
    if (activeLookup != null) {
      activeLookup.addLookupListener(new LookupListener() {
        @Override
        public void currentItemChanged(@NotNull LookupEvent event) {
          if (windowEvent.asPopup().isVisible()) { //was not canceled yet
            final LookupElement item = event.getItem();
            if (item != null) {
              PsiElement targetElement =
                DocumentationManager.getInstance(myProject).getElementFromLookup(activeLookup.getEditor(), activeLookup.getPsiFile());

              updatePopup(targetElement); //open next
            }
          } else {
            activeLookup.removeLookupListener(this);
          }
        }
      });
    }
    else {
      final Component focusedComponent = WindowManagerEx.getInstanceEx().getFocusedComponent(myProject);
      QuickSearchComponent quickSearch = findQuickSearchComponent(focusedComponent);
      if (quickSearch != null) {
        quickSearch.registerHint(windowEvent.asPopup());
      }
      else if (focusedComponent instanceof JComponent) {
        HintUpdateSupply supply = HintUpdateSupply.getSupply((JComponent)focusedComponent);
        if (supply != null) supply.registerHint(windowEvent.asPopup());
      }
    }
  }

  private static QuickSearchComponent findQuickSearchComponent(Component c) {
    while (c != null) {
      if (c instanceof QuickSearchComponent) {
        return (QuickSearchComponent) c;
      }
      c = c.getParent();
    }
    return null;
  }
}
