/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMethodDescriptor
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.symbols.JKUniverseMethodSymbol
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKCapturedType
import org.jetbrains.kotlin.nj2k.tree.impl.psi
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class AddElementsInfoConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        when (element) {
            is JKTypeElement -> addInfoForTypeElement(element)
            is JKKtFunction -> addInfoForFunction(element)
        }

        return recurse(element)
    }

    private fun addInfoForTypeElement(typeElement: JKTypeElement) {
        typeElement.type.forAllInnerTypes { type ->
            if (type.nullability == Nullability.Default
                || type.safeAs<JKCapturedType>()?.wildcardType is JKStarProjectionType
            ) {
                context.elementsInfoStorage.addEntry(type, UnknownNullability)
            }
        }
    }

    private fun addInfoForFunction(function: JKKtFunction) {
        val psiMethod = function.psi<PsiMethod>() ?: return
        val descriptor = psiMethod.getJavaMethodDescriptor() ?: return
        val superDescriptorsInfo =
            descriptor.overriddenDescriptors.mapNotNull { superDescriptor ->
                val superPsi = superDescriptor.original.findPsi()
                when (val symbol = context.symbolProvider.symbolsByPsi[superPsi]) {
                    is JKUniverseMethodSymbol ->
                        InternalSuperFunctionInfo(
                            context.elementsInfoStorage.getOrCreateInfoForElement(symbol.target)
                        )
                    else -> ExternalSuperFunctionInfo(superDescriptor)
                }
            }
        context.elementsInfoStorage.addEntry(function, FunctionInfo(descriptor, superDescriptorsInfo))
    }

    private fun JKType.forAllInnerTypes(action: (JKType) -> Unit) {
        action(this)
        if (this is JKParametrizedType) {
            parameters.forEach { it.forAllInnerTypes(action) }
        }
    }
}