// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
   *
   * @see com.intellij.model.psi.UrlReferenceHost
   */
  public static final ReferenceProviderType COMMENTS_REFERENCE_PROVIDER_TYPE = new ReferenceProviderType("commentsReferenceProvider");

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(PsiComment.class), COMMENTS_REFERENCE_PROVIDER_TYPE.getProvider());
  }
}
