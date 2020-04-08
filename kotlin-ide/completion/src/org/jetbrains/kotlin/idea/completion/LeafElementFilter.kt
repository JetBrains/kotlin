/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.psi.PsiElement
import com.intellij.psi.filters.ClassFilter
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType

class LeafElementFilter(private val elementType: IElementType) : ElementFilter {

    override fun isAcceptable(element: Any?, context: PsiElement?) = element is LeafPsiElement && element.elementType == elementType

    override fun isClassAcceptable(hintClass: Class<*>) = LEAF_CLASS_FILTER.isClassAcceptable(hintClass)

    companion object {
        private val LEAF_CLASS_FILTER = ClassFilter(LeafPsiElement::class.java)
    }
}
