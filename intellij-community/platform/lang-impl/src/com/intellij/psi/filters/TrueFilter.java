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

import com.intellij.psi.PsiElement;

public class TrueFilter implements ElementFilter {
  public static final ElementFilter INSTANCE = new TrueFilter();

  private TrueFilter() { }

  @Override
  public boolean isClassAcceptable(Class hintClass) {
    return true;
  }

  @Override
  public boolean isAcceptable(Object element, PsiElement context) {
    return true;
  }

  @Override
  public String toString() {
    return "true";
  }
}