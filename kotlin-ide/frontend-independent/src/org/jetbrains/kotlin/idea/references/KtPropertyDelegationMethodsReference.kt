/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.util.OperatorNameConventions

abstract class KtPropertyDelegationMethodsReference(element: KtPropertyDelegate) : KtMultiReference<KtPropertyDelegate>(element) {
    override fun getRangeInElement(): TextRange {
        val byKeywordNode = expression.byKeywordNode
        val offset = byKeywordNode.psi!!.startOffsetInParent
        return TextRange(offset, offset + byKeywordNode.textLength)
    }

       override val resolvesByNames: Collection<Name> get() = NAMES

    companion object {
        private val NAMES = listOf(
            OperatorNameConventions.GET_VALUE,
            OperatorNameConventions.SET_VALUE,
            OperatorNameConventions.PROVIDE_DELEGATE
        )
    }
}
