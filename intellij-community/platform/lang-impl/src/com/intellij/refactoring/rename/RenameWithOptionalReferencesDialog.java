/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.refactoring.rename;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.ui.NonFocusableCheckBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 */
public abstract class RenameWithOptionalReferencesDialog extends RenameDialog {
  private JCheckBox myCbSearchForReferences;

  public RenameWithOptionalReferencesDialog(@NotNull Project project,
                                            @NotNull PsiElement psiElement,
                                            @Nullable PsiElement nameSuggestionContext,
                                            Editor editor) {
    super(project, psiElement, nameSuggestionContext, editor);
  }

  @Override
  protected void createCheckboxes(JPanel panel, GridBagConstraints gbConstraints) {
    gbConstraints.insets = new Insets(0, 0, 4, 0);
    gbConstraints.gridwidth = 1;
    gbConstraints.gridx = 0;
    gbConstraints.weighty = 0;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;
    myCbSearchForReferences = new NonFocusableCheckBox(RefactoringBundle.message("search.for.references"));
    myCbSearchForReferences.setSelected(getSearchForReferences());
    panel.add(myCbSearchForReferences, gbConstraints);

    super.createCheckboxes(panel, gbConstraints);
  }

  @Override
  protected void doAction() {
    setSearchForReferences(myCbSearchForReferences.isSelected());
    super.doAction();
  }

  protected abstract boolean getSearchForReferences();

  protected abstract void setSearchForReferences(boolean value);
}
