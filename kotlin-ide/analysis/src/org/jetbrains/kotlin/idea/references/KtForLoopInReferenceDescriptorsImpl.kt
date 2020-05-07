/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.util.OperatorNameConventions

class KtForLoopInReferenceDescriptorsImpl(
    element: KtForExpression
) : KtForLoopInReference(element), KtDescriptorsBasedReference {
    override fun isReferenceTo(element: PsiElement): Boolean =
        super<KtDescriptorsBasedReference>.isReferenceTo(element)

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val loopRange = expression.loopRange ?: return emptyList()
        return LOOP_RANGE_KEYS.mapNotNull { key -> context.get(key, loopRange)?.candidateDescriptor }
    }

    companion object {
        private val LOOP_RANGE_KEYS = arrayOf(
            BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL,
            BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL,
            BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL
        )
    }
}
