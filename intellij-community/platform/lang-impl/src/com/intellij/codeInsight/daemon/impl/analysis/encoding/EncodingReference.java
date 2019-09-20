/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.daemon.impl.analysis.encoding;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
public class EncodingReference implements PsiReference, EmptyResolveMessageProvider {
  private final PsiElement myElement;

  private final String myCharsetName;
  private final TextRange myRangeInElement;

  public EncodingReference(PsiElement element, final String charsetName, final TextRange rangeInElement) {
    myElement = element;
    myCharsetName = charsetName;
    myRangeInElement = rangeInElement;
  }

  @NotNull
  @Override
  public PsiElement getElement() {
    return myElement;
  }

  @NotNull
  @Override
  public TextRange getRangeInElement() {
    return myRangeInElement;
  }

  @Override
  @Nullable
  public PsiElement resolve() {
    return CharsetToolkit.forName(myCharsetName) == null ? null : myElement;
    //if (ApplicationManager.getApplication().isUnitTestMode()) return myValue; // tests do not have full JDK
    //String fqn = charset.getClass().getName();
    //return myValue.getManager().findClass(fqn, GlobalSearchScope.allScope(myValue.getProject()));
  }

  @Override
  @NotNull
  public String getCanonicalText() {
    return myCharsetName;
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    return null;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    return null;
  }

  @Override
  public boolean isReferenceTo(@NotNull PsiElement element) {
    return false;
  }

  @Override
  @NotNull
  public Object[] getVariants() {
    Charset[] charsets = CharsetToolkit.getAvailableCharsets();
    List<LookupElement> suggestions = new ArrayList<>(charsets.length);
    for (Charset charset : charsets) {
      suggestions.add(LookupElementBuilder.create(charset.name()).withCaseSensitivity(false));
    }
    return suggestions.toArray(LookupElement.EMPTY_ARRAY);
  }

  @Override
  public boolean isSoft() {
    return false;
  }

  @Override
  @NotNull
  public String getUnresolvedMessagePattern() {
    //noinspection UnresolvedPropertyKey
    return CodeInsightBundle.message("unknown.encoding.0");
  }
}
