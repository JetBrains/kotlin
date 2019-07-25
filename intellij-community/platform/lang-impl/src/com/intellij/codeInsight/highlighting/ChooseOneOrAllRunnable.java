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
package com.intellij.codeInsight.highlighting;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.IPopupChooserBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ChooseOneOrAllRunnable<T extends PsiElement> implements Runnable {
  private final T[] myClasses;
  private final Editor myEditor;

  private final String myTitle;

  public ChooseOneOrAllRunnable(final List<T> classes, final Editor editor, final String title, Class<T> type) {
    myClasses = ArrayUtil.toObjectArray(classes, type);
    myEditor = editor;
    myTitle = title;
  }

  protected abstract void selected(@NotNull T... classes);

  @Override
  public void run() {
    if (myClasses.length == 1) {
      selected((T[])ArrayUtil.toObjectArray(myClasses[0].getClass(), myClasses[0]));
    }
    else if (myClasses.length > 0) {
      PsiElementListCellRenderer<T> renderer = createRenderer();

      Arrays.sort(myClasses, renderer.getComparator());

      if (ApplicationManager.getApplication().isUnitTestMode()) {
        selected(myClasses);
        return;
      }
      List<Object> model = new ArrayList<>(Arrays.asList(myClasses));
      String selectAll = CodeInsightBundle.message("highlight.thrown.exceptions.chooser.all.entry");
      model.add(0, selectAll);

      final IPopupChooserBuilder builder = JBPopupFactory.getInstance()
        .createPopupChooserBuilder(model)
        .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        .setRenderer(renderer)
        .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        .setItemChosenCallback((selectedValue) -> {
          if (selectedValue.equals(selectAll)) {
            selected(myClasses);
          }
          else {
            selected((T[])ArrayUtil.toObjectArray(selectedValue.getClass(), selectedValue));
          }
        })
        .setTitle(myTitle);
      renderer.installSpeedSearch(builder);

      ApplicationManager.getApplication().invokeLater(() -> builder
        .createPopup()
        .showInBestPositionFor(myEditor));
    }
  }

  protected abstract PsiElementListCellRenderer<T> createRenderer();
}
