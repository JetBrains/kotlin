// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation

import com.intellij.ide.util.EditSourceUtil
import com.intellij.navigation.NavigationTarget
import com.intellij.navigation.TargetPopupPresentation
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus.Experimental

@Experimental
class PsiElementNavigationTarget(private val myElement: PsiElement) : NavigationTarget {

  private val myNavigatable by lazy {
    if (myElement is Navigatable) myElement else EditSourceUtil.getDescriptor(myElement)
  }

  private val myPresentation by lazy {
    PsiElementTargetPopupPresentation(myElement)
  }

  override fun isValid(): Boolean = myElement.isValid

  override fun getNavigatable(): Navigatable? = myNavigatable

  override fun getTargetPresentation(): TargetPopupPresentation = myPresentation

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PsiElementNavigationTarget

    if (myElement != other.myElement) return false

    return true
  }

  override fun hashCode(): Int {
    return myElement.hashCode()
  }
}
