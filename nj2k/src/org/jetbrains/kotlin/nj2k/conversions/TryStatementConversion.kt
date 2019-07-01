/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.copyTreeAndDetach
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.nj2k.useExpression


class TryStatementConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaTryStatement) return recurse(element)
        return if (element.resourceDeclarations.isEmpty())
            recurse(convertNoResourcesTryStatement(element))
        else recurse(convertTryStatementWithResources(element))
    }

    private fun convertNoResourcesTryStatement(tryStatement: JKJavaTryStatement): JKStatement =
        JKExpressionStatementImpl(
            JKKtTryExpressionImpl(
                tryStatement::tryBlock.detached(),
                tryStatement::finallyBlock.detached(),
                tryStatement.catchSections.flatMap(::convertCatchSection)
            )
        ).withNonCodeElementsFrom(tryStatement)

    private fun convertTryStatementWithResources(tryStatement: JKJavaTryStatement): JKStatement {
        val body =
            resourceDeclarationsToUseExpression(
                tryStatement.resourceDeclarations,
                JKBlockStatementImpl(tryStatement::tryBlock.detached())
            )
        return if (tryStatement.finallyBlock !is JKBodyStub || tryStatement.catchSections.isNotEmpty()) {
            JKExpressionStatementImpl(
                JKKtTryExpressionImpl(
                    JKBlockImpl(listOf(body)),
                    tryStatement::finallyBlock.detached(),
                    tryStatement.catchSections.flatMap(::convertCatchSection)
                )
            ).withNonCodeElementsFrom(tryStatement)
        } else body
    }

    private fun resourceDeclarationsToUseExpression(
        resourceDeclarations: List<JKDeclaration>,
        innerStatement: JKStatement
    ): JKStatement =
        resourceDeclarations
            .reversed()
            .fold(innerStatement) { inner, variable ->
                JKExpressionStatementImpl(
                    useExpression(
                        receiver = (variable as JKLocalVariable)::initializer.detached(),
                        variableIdentifier = variable::name.detached(),
                        body = inner,
                        symbolProvider = context.symbolProvider
                    )
                )
            }

    private fun convertCatchSection(javaCatchSection: JKJavaTryCatchSection): List<JKKtTryCatchSection> {
        javaCatchSection.block.detach(javaCatchSection)
        return javaCatchSection.parameter.type.type.let {
            (it as? JKJavaDisjunctionType)?.disjunctions ?: listOf(it)
        }.map {
            val parameter = JKParameterImpl(
                JKTypeElementImpl(it.updateNullability(Nullability.NotNull)),
                javaCatchSection.parameter.name.copyTreeAndDetach()
            )
            JKKtTryCatchSectionImpl(
                parameter,
                javaCatchSection.block.copyTreeAndDetach()
            ).withNonCodeElementsFrom(javaCatchSection)
        }
    }
}