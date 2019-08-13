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
package com.intellij.psi.filters;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;

import java.util.Arrays;

/**
 * @author yole
 */
public class PlainTextFilter implements ElementFilter {
  protected final String[] myValue;

  public PlainTextFilter(String... values) {
    myValue = values;
  }

  public PlainTextFilter(String value1, String value2) {
    myValue = new String[]{value1, value2};
  }

  @Override
  public boolean isClassAcceptable(Class hintClass) {
    return true;
  }

  @Override
  public boolean isAcceptable(Object element, PsiElement context) {
    return element != null && Arrays.stream(myValue).anyMatch(v -> v == null || v.equals(getTextByElement(element)));
  }

  protected String getTextByElement(Object element) {
    String elementValue = null;
    if (element instanceof PsiNamedElement) {
      elementValue = ((PsiNamedElement)element).getName();
    }
    else if (element instanceof PsiElement) {
      elementValue = ((PsiElement)element).getText();
    }
    return elementValue;
  }

  @Override
  public String toString() {
    return '(' + StringUtil.join(myValue, " | ") + ')';
  }
}