/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMethodDescriptor
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.symbols.JKUniverseMethodSymbol
import org.jetbrains.kotlin.nj2k.tree.JKMethod
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement
import org.jetbrains.kotlin.nj2k.tree.JKTypeElement


import org.jetbrains.kotlin.nj2k.types.isCollectionType
import org.jetbrains.kotlin.nj2k.types.JKCapturedType
import org.jetbrains.kotlin.nj2k.types.JKParametrizedType
import org.jetbrains.kotlin.nj2k.types.JKStarProjectionType
import org.jetbrains.kotlin.nj2k.types.JKType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class AddElementsInfoConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        when (element) {
            is JKTypeElement -> addInfoForTypeElement(element)
            is JKMethod -> addInfoForFunction(element)
        }

        return recurse(element)
    }

    private fun addInfoForTypeElement(typeElement: JKTypeElement) {
        typeElement.type.forAllInnerTypes { type ->
            val hasUnknownNullability =
                type.nullability == Nullability.Default || type.safeAs<JKCapturedType>()?.wildcardType is JKStarProjectionType
            val hasUnknownMutability = type.isCollectionType
            context.elementsInfoStorage.addEntry(type, JKTypeInfo(hasUnknownNullability, hasUnknownMutability))
        }
    }

    private fun addInfoForFunction(function: JKMethod) {
        val superMethods = function.superMethods() ?: return
        val superDescriptorsInfo = superMethods.map { superDescriptor ->
            val superPsi = superDescriptor.original.findPsi()
            when (val symbol = symbolProvider.symbolsByPsi[superPsi]) {
                is JKUniverseMethodSymbol ->
                    InternalSuperFunctionInfo(context.elementsInfoStorage.getOrCreateInfoForElement(symbol.target))
                else -> ExternalSuperFunctionInfo(superDescriptor)
            }
        }
        context.elementsInfoStorage.addEntry(function, FunctionInfo(superDescriptorsInfo))
    }

    private fun JKMethod.superMethods(): Collection<FunctionDescriptor>? {
        val psiMethod = psi<PsiMethod>() ?: return null
        psiMethod.getJavaMethodDescriptor()?.let { descriptor ->
            return descriptor.overriddenDescriptors
        }
        return psiMethod.findSuperMethods().mapNotNull { superMethod ->
            when (superMethod) {
                is KtLightMethod -> superMethod.kotlinOrigin?.resolveToDescriptorIfAny()?.safeAs()
                else -> superMethod.getJavaMethodDescriptor()
            }
        }
    }

    private fun JKType.forAllInnerTypes(action: (JKType) -> Unit) {
        action(this)
        if (this is JKParametrizedType) {
            parameters.forEach { it.forAllInnerTypes(action) }
        }
    }
}