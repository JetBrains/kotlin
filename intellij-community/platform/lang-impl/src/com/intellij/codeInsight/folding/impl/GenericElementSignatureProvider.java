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
package com.intellij.codeInsight.folding.impl;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aggregates 'generic' (language-agnostic) {@link ElementSignatureProvider signature providers}.
 * <p/>
 * Thread-safe.
 * 
 * @author Denis Zhdanov
 */
public class GenericElementSignatureProvider implements ElementSignatureProvider {
  
  private static final ElementSignatureProvider[] PROVIDERS = {
    new PsiNamesElementSignatureProvider(), new OffsetsElementSignatureProvider()
  };
  
  @Override
  public String getSignature(@NotNull PsiElement element) {
    for (ElementSignatureProvider provider : PROVIDERS) {
      String result = provider.getSignature(element);
      if (result != null) {
        return result;
      } 
    }
    return null;
  }

  @Override
  public PsiElement restoreBySignature(@NotNull PsiFile file, @NotNull String signature, @Nullable StringBuilder processingInfoStorage) {
    for (ElementSignatureProvider provider : PROVIDERS) {
      PsiElement result = provider.restoreBySignature(file, signature, processingInfoStorage);
      if (result != null) {
        return result;
      } 
    }
    return null;
  }
}
