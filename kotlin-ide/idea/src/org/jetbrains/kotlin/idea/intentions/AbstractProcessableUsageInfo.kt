/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.psi.KtElement

abstract class AbstractProcessableUsageInfo<out T : PsiElement, in D : Any>(element: T) : UsageInfo(element) {
    @Suppress("UNCHECKED_CAST")
    override fun getElement() = super.getElement() as T?

    abstract fun process(data: D, elementsToShorten: MutableList<KtElement>)
}