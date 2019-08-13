
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

package com.intellij.codeInsight.template;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.editor.Document;

public class InvokeActionResult implements Result{
  private final Runnable myAction;

  public InvokeActionResult(Runnable action) {
    myAction = action;
  }

  public Runnable getAction() {
    return myAction;
  }

  @Override
  public boolean equalsToText(String text, PsiElement context) {
    return true; //no text result will be provided anyway
  }

  public String toString() {
    return "";
  }

  @Override
  public void handleFocused(final PsiFile psiFile, final Document document, final int segmentStart, final int segmentEnd) {
    myAction.run();
  }
}