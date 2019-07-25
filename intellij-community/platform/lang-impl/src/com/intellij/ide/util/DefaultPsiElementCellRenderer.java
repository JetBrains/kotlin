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

package com.intellij.ide.util;

import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;

public class DefaultPsiElementCellRenderer extends PsiElementListCellRenderer<PsiElement> {
  @Override
  protected int getIconFlags() {
    return Iconable.ICON_FLAG_VISIBILITY;
  }

  @Override
  public String getElementText(PsiElement element){
    return SymbolPresentationUtil.getSymbolPresentableText(element);
  }

  @Override
  public String getContainerText(PsiElement element, final String name){
    return SymbolPresentationUtil.getSymbolContainerText(element);
  }

}
