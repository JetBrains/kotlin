/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.ConversionContext
import org.jetbrains.kotlin.nj2k.isEquals
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKFieldSymbol
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtSpreadOperator
import org.jetbrains.kotlin.nj2k.tree.impl.JKTypeElementImpl
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class LowerNullabilityInFunctionParametersConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKMethod && element !is JKLambdaExpression) return recurse(element)
        val (parameters, scopes) =
            when (element) {
                is JKMethod -> element.parameters to
                        listOfNotNull(
                            element.block,
                            (element as? JKKtConstructor)?.delegationCall
                        ) + element.parameters.mapNotNull { if (it.initializer !is JKStubExpression) it.initializer else null }
                is JKLambdaExpression -> element.parameters to listOf(element.statement)
                else -> return recurse(element)
            }

        for (parameter in parameters) {
            if (parameter.type.type.nullability != Nullability.Default) continue
            if (parameter.hasNotNullUsages(scopes)) {
                parameter.type = JKTypeElementImpl(parameter.type.type.updateNullability(Nullability.NotNull))
            }
        }
        return recurse(element)
    }

    private fun JKParameter.hasNotNullUsages(scopes: List<JKTreeElement>): Boolean =
        scopes.any {
            val searcher = HasNotNullUsagesSearcher(context.symbolProvider.provideUniverseSymbol(this))
            scopes.any { searcher.runConversion(it, context) }
            searcher.found
        }

    private inner class HasNotNullUsagesSearcher(private val parameterSymbol: JKFieldSymbol) : RecursiveApplicableConversionBase() {
        var found: Boolean = false
        override fun applyToElement(element: JKTreeElement): JKTreeElement {
            when (element) {
                is JKQualifiedExpression -> {
                    val receiver = element.receiver as? JKFieldAccessExpression ?: return recurse(element)
                    if (receiver.identifier == parameterSymbol) {
                        found = true
                        return element
                    }
                }
                is JKPrefixExpression -> {
                    if (element.expression.safeAs<JKFieldAccessExpression>()?.identifier == parameterSymbol
                        && element.operator is JKKtSpreadOperator
                    ) {
                        found = true
                        return element
                    }
                }
                is JKFieldAccessExpression -> {
                    if (element.identifier == parameterSymbol
                        && element.parent is JKBinaryExpression
                        && element.parent.cast<JKBinaryExpression>().let {
                            !it.operator.isEquals()
                                    && (it.left.type(context.symbolProvider)?.asPrimitiveType() != null
                                    || it.right.type(context.symbolProvider)?.asPrimitiveType() != null)
                        }
                    ) {
                        found = true
                        return element
                    }
                }
            }
            return recurse(element)
        }
    }

}