/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.ide.util.DeleteHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author yole
 */
public class PsiActionSupportFactoryImpl extends PsiActionSupportFactory {
  @Override
  public CopyPasteSupport createPsiBasedCopyPasteSupport(final Project project, final JComponent keyReceiver,
                                                         final PsiElementSelector dataSelector) {
    return new CopyPasteDelegator(project, keyReceiver) {
      @Override
      @NotNull
      protected PsiElement[] getSelectedElements() {
        PsiElement[] elements = dataSelector.getSelectedElements();
        return elements == null ? PsiElement.EMPTY_ARRAY : elements;
      }
    };
  }

  @Override
  public DeleteProvider createPsiBasedDeleteProvider() {
    return new DeleteHandler.DefaultDeleteProvider();
  }
}
