/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.ConversionContext
import org.jetbrains.kotlin.nj2k.findUsages
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*

class ImplicitInitializerConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {

    enum class InitializationState {
        INITIALIZED_IN_ALL_CONSTRUCTORS,
        INITIALIZED_IN_SOME_CONSTRUCTORS,
        NON_INITIALIZED
    }

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaField) return recurse(element)
        if (element.initializer !is JKStubExpression) return recurse(element)

        val initializationState = element.initializationState()
        when {
            initializationState == InitializationState.INITIALIZED_IN_ALL_CONSTRUCTORS ->
                return recurse(element)

            initializationState == InitializationState.INITIALIZED_IN_SOME_CONSTRUCTORS
                    && element.modality == Modality.FINAL ->
                return recurse(element)
        }

        val fieldType = element.type.type
        val newInitializer = when (fieldType) {
            is JKClassType -> JKNullLiteral()
            is JKJavaPrimitiveType -> createPrimitiveTypeInitializer(fieldType)
            else -> null
        }
        newInitializer?.also {
            element.initializer = it
        }
        return element
    }

    private fun JKJavaField.initializationState(): InitializationState {
        val fieldSymbol = context.symbolProvider.provideUniverseSymbol(this)
        val containingClass = parentOfType<JKClass>() ?: return InitializationState.NON_INITIALIZED
        val symbolToConstructor = containingClass.declarationList
            .filterIsInstance<JKKtConstructor>()
            .map { context.symbolProvider.provideUniverseSymbol(it) to it }
            .toMap()

        fun JKMethodSymbol.parentConstructor(): JKMethodSymbol? =
            (symbolToConstructor[this]?.delegationCall as? JKDelegationConstructorCall)
                ?.identifier

        val constructors = containingClass.declarationList
            .filterIsInstance<JKKtConstructor>()
            .map { context.symbolProvider.provideUniverseSymbol(it) to false }
            .toMap()
            .toMutableMap()

        val constructorsWithInitializers = findUsages(parentOfType<JKClass>()!!, context).mapNotNull { usage ->
            val parent = usage.parent
            val assignmentStatement =
                when {
                    parent is JKKtAssignmentStatement -> parent
                    parent is JKQualifiedExpression && parent.receiver is JKThisExpression ->
                        parent.parent  as? JKKtAssignmentStatement
                    else -> null
                } ?: return@mapNotNull null
            val constructor =
                (assignmentStatement.parent as? JKBlock)?.parent as? JKKtConstructor ?: return@mapNotNull null

            val isInitializer = when (parent) {
                is JKKtAssignmentStatement -> (parent.field as? JKFieldAccessExpression)?.identifier == fieldSymbol
                is JKQualifiedExpression -> (parent.selector as? JKFieldAccessExpression)?.identifier == fieldSymbol
                else -> false
            }
            if (!isInitializer) return@mapNotNull null
            constructor
        }

        for (constructor in constructorsWithInitializers) {
            constructors[context.symbolProvider.provideUniverseSymbol(constructor)] = true
        }


        for ((constructor, initialized) in constructors) {
            if (initialized) continue
            val parentConstructors =
                generateSequence(constructor) { it.parentConstructor() }
            if (parentConstructors.any { constructors[it] == true }) {
                parentConstructors.forEach { constructors[it] = true }
            }
        }

        val initializedInConstructorsCount = constructors.values.count { it }
        return when (initializedInConstructorsCount) {
            0 -> InitializationState.NON_INITIALIZED
            constructors.size -> InitializationState.INITIALIZED_IN_ALL_CONSTRUCTORS
            else -> InitializationState.INITIALIZED_IN_SOME_CONSTRUCTORS
        }
    }

    private fun createPrimitiveTypeInitializer(primitiveType: JKJavaPrimitiveType): JKLiteralExpression =
        when (primitiveType) {
            is JKJavaPrimitiveTypeImpl.BOOLEAN ->
                JKBooleanLiteral(false)
            else ->
                JKJavaLiteralExpressionImpl("0", JKLiteralExpression.LiteralType.INT)
        }
}