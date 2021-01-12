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
package com.intellij.codeInsight.template;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import java.util.List;

/**
 * @author peter
 */
public class ListResult implements Result {
  private final List<Result> myComponents;

  public ListResult(List<Result> components) {
    myComponents = components;
  }

  public List<Result> getComponents() {
    return myComponents;
  }

  @Override
  public String toString() {
    return myComponents.toString();
  }

  @Override
  public boolean equalsToText(String text, PsiElement context) {
    return false;
  }

  @Override
  public void handleFocused(PsiFile psiFile, Document document, int segmentStart, int segmentEnd) {
  }
}
