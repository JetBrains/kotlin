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
package com.intellij.refactoring.inline;

import com.intellij.CommonBundle;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractInlineLocalDialog extends InlineOptionsDialog {
  public AbstractInlineLocalDialog(Project project, PsiElement variable, final PsiReference ref, int occurrencesCount) {
    super(project, true, variable);
    if (ref == null || occurrencesCount == 1) {
      setDoNotAskOption(new DoNotAskOption() {
        @Override
        public boolean isToBeShown() {
          return EditorSettingsExternalizable.getInstance().isShowInlineLocalDialog();
        }

        @Override
        public void setToBeShown(boolean value, int exitCode) {
          EditorSettingsExternalizable.getInstance().setShowInlineLocalDialog(value);
        }

        @Override
        public boolean canBeHidden() {
          return true;
        }

        @Override
        public boolean shouldSaveOptionsOnCancel() {
          return false;
        }

        @NotNull
        @Override
        public String getDoNotShowMessage() {
          return CommonBundle.message("dialog.options.do.not.show");
        }
      });
    }
  }
}
