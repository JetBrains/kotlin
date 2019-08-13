/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiAwareObject;
import org.jetbrains.annotations.NotNull;

public class PsiElementFromSelectionsRule implements GetDataRule {
  private static final Logger LOG = Logger.getInstance(PsiElementFromSelectionsRule.class);

  @Override
  public Object getData(@NotNull DataProvider dataProvider) {
    Object items = PlatformDataKeys.SELECTED_ITEMS.getData(dataProvider);
    if (items == null) return null;

    if (!(items instanceof Object[])) {
      String errorMessage = "Value of type Object[] is expected, but " + items.getClass() + " is returned by " + dataProvider.getClass();
      PluginException.logPluginError(LOG, errorMessage, null, dataProvider.getClass());
      return null;
    }
    Project project = CommonDataKeys.PROJECT.getData(dataProvider);
    Object[] objects = (Object[])items;
    PsiElement[] elements = new PsiElement[objects.length];
    for (int i = 0, len = objects.length; i < len; i++) {
      Object o = objects[i];
      PsiElement element = o instanceof PsiElement ? (PsiElement)o :
                           o instanceof PsiAwareObject && project != null ? ((PsiAwareObject)o).findElement(project) : null;
      if (element == null || !element.isValid()) return null;
      elements[i] = element;
    }

    return elements;
  }
}
