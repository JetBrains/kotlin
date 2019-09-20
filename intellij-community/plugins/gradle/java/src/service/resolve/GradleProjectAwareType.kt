// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

// TODO track exact project id to obtain project extension data
class GradleProjectAwareType (
  private val delegate: PsiClassType,
  val buildscript: Boolean
) : PsiClassType(LanguageLevel.HIGHEST) {

  override fun isValid(): Boolean = delegate.isValid
  override fun getResolveScope(): GlobalSearchScope = delegate.resolveScope
  override fun resolve(): PsiClass? = delegate.resolve()
  override fun resolveGenerics(): ClassResolveResult = delegate.resolveGenerics()
  override fun getParameters(): Array<PsiType> = delegate.parameters
  override fun rawType(): PsiClassType = GradleProjectAwareType(delegate.rawType(), buildscript)

  override fun getClassName(): String = delegate.className
  override fun getCanonicalText(): String = delegate.canonicalText
  override fun getPresentableText(): String = delegate.presentableText
  override fun equalsToText(text: String): Boolean = delegate.equalsToText(text)

  override fun getLanguageLevel(): LanguageLevel = delegate.languageLevel
  override fun setLanguageLevel(languageLevel: LanguageLevel): PsiClassType = error("must not be called")

  fun setType(delegate: PsiClassType): GradleProjectAwareType {
    return GradleProjectAwareType(delegate, buildscript)
  }

  fun setType(delegate: PsiClassType, buildscript: Boolean): GradleProjectAwareType {
    return GradleProjectAwareType(delegate, buildscript)
  }
}
