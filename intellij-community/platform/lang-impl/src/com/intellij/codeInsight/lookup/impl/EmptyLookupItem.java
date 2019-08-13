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
package com.intellij.codeInsight.lookup.impl;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
*/
public class EmptyLookupItem extends LookupElement {
  private final String myText;
  private final boolean myLoading;

  public EmptyLookupItem(final String s, boolean loading) {
    myText = s;
    myLoading = loading;
  }

  @NotNull
  @Override
  public String getLookupString() {
    return "             ";
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    presentation.setItemText(myText);
  }

  public boolean isLoading() {
    return myLoading;
  }
}
