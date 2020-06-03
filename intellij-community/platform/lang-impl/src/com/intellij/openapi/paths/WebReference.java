/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.paths;

import com.intellij.ide.BrowserUtil;
import com.intellij.model.psi.PsiExternalReferenceHost;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.FakePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public class WebReference extends PsiReferenceBase<PsiElement> {
  @Nullable private final String myUrl;
  
  public WebReference(@NotNull PsiElement element) {
    this(element, (String)null);
  }
  
  public WebReference(@NotNull PsiElement element, @Nullable String url) {
    super(element, true);
    myUrl = url;
  }

  public WebReference(@NotNull PsiElement element, @NotNull TextRange textRange) {
    this(element, textRange, null);
  }

  public WebReference(@NotNull PsiElement element, TextRange textRange, @Nullable String url) {
    super(element, textRange, true);
    myUrl = url;
  }

  @Override
  public PsiElement resolve() {
    return new MyFakePsiElement();
  }

  public String getUrl() {
    return myUrl != null ? myUrl : getValue();
  }

  class MyFakePsiElement extends FakePsiElement implements SyntheticElement {
    @Override
    public PsiElement getParent() {
      return myElement;
    }

    @Override
    public void navigate(boolean requestFocus) {
      BrowserUtil.browse(getUrl());
    }

    @Override
    public String getPresentableText() {
      return getUrl();
    }


    @Override
    public String getName() {
      return getUrl();
    }

    @Override
    public TextRange getTextRange() {
      final TextRange rangeInElement = getRangeInElement();
      final TextRange elementRange = myElement.getTextRange();
      return elementRange != null ? rangeInElement.shiftRight(elementRange.getStartOffset()) : rangeInElement;
    }
  }

  /**
   * Optimization method to greatly reduce frequency of potentially expensive {@link PsiElement#getReferences()} calls
   * @return true if the element is able to contain WebReference
   */
  public static boolean isWebReferenceWorthy(@NotNull PsiElement element) {
    return element instanceof HintedReferenceHost || element instanceof ContributedReferenceHost || element instanceof PsiExternalReferenceHost;
  }
}
