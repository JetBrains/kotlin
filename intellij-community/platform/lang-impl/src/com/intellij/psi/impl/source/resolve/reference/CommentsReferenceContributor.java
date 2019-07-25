/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.psi.impl.source.resolve.reference;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.ReferenceProviderType;
import org.jetbrains.annotations.NotNull;

public class CommentsReferenceContributor extends PsiReferenceContributor {
  /**
   * Use this provider type if your element is not PsiComment but you want to fill it 
   * with the same references as PsiComment (e.g. it's used for PsiDocToken).
   * 
   * Also you can register your own reference provider as 'comment reference provider' and it will be
   * applied to all PsiComment and other elements that uses this provider type for retrieving references.
   */
  public static final ReferenceProviderType COMMENTS_REFERENCE_PROVIDER_TYPE = new ReferenceProviderType("commentsReferenceProvider");
  
  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(PsiComment.class), COMMENTS_REFERENCE_PROVIDER_TYPE.getProvider());
  }
}
