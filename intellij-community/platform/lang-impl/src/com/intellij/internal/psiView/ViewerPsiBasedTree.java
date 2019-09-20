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
package com.intellij.internal.psiView;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;

public interface ViewerPsiBasedTree extends Disposable {

  interface PsiTreeUpdater {
    void updatePsiTree(@NotNull PsiElement toSelect, @Nullable TextRange selectRangeInEditor);
  }

  void reloadTree(@Nullable PsiElement rootRootElement, @NotNull String text);

  void selectNodeFromPsi(@Nullable PsiElement element);

  default void selectNodeFromEditor(@Nullable PsiElement element) {
    selectNodeFromPsi(element);
  }

  @NotNull
  JComponent getComponent();

  boolean isFocusOwner();


  void focusTree();

  static void removeListenerOfClass(@NotNull Tree tree, @NotNull Class<?> listenerClass) {
    TreeSelectionListener[] listeners = tree.getTreeSelectionListeners();

    for (TreeSelectionListener listener : listeners) {
      if (listenerClass.isInstance(listener)) {
        tree.removeTreeSelectionListener(listener);
      }
    }
  }
}
