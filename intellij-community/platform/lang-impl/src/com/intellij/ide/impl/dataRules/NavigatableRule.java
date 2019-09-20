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

package com.intellij.ide.impl.dataRules;

import com.intellij.ide.util.EditSourceUtil;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class NavigatableRule implements GetDataRule {
  @Override
  public Object getData(@NotNull DataProvider dataProvider) {
    final Navigatable navigatable = CommonDataKeys.NAVIGATABLE.getData(dataProvider);
    if (navigatable instanceof OpenFileDescriptor) {
      final OpenFileDescriptor openFileDescriptor = (OpenFileDescriptor)navigatable;

      if (openFileDescriptor.getFile().isValid()) {
        return openFileDescriptor;
      }
    }
    final PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataProvider);
    if (element instanceof Navigatable) {
      return element;
    }
    if (element != null) {
      return EditSourceUtil.getDescriptor(element);
    }

    final Object selection = PlatformDataKeys.SELECTED_ITEM.getData(dataProvider);
    if (selection instanceof Navigatable) {
      return selection;
    }

    return null;
  }
}
