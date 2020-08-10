/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo

class BasicUnresolvableCollisionUsageInfo(
    element: PsiElement,
    referencedElement: PsiElement,
    @NlsContexts.DialogMessage private val _description: String
) : UnresolvableCollisionUsageInfo(element, referencedElement) {
    override fun getDescription() = _description
}
