/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*


class FieldInitializersInPrimaryFromParamsConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        val primaryConstructorConversion = PrimaryConstructorConversion()
        primaryConstructorConversion.runConversion(element, context)
        if (primaryConstructorConversion.initDeclarationStatements.isNotEmpty()) {
            element.getOrCreateInitDeclaration().block = JKBlockImpl(primaryConstructorConversion.initDeclarationStatements)
        }
        element.classBody.declarations -= primaryConstructorConversion.declarationsToRemove
        return recurse(element)
    }

    private class PrimaryConstructorConversion : RecursiveApplicableConversionBase() {
        val initDeclarationStatements = mutableListOf<JKStatement>()
        val declarationsToRemove = mutableListOf<JKDeclaration>()

        override fun applyToElement(element: JKTreeElement): JKTreeElement {
            if (element !is JKKtPrimaryConstructor) return recurse(element)
            val containingClass = element.parentOfType<JKClass>() ?: return recurse(element)
            val removedStatements = mutableListOf<JKStatement>()
            for (constructorStatement in element.block.statements) {
                val assignmentExpression =
                    (constructorStatement as? JKExpressionStatement)?.expression as? JKJavaAssignmentExpression ?: continue
                //TODO check if operator is `=`
                val smartField = assignmentExpression.smartField() ?: continue
                val fieldTarget = smartField.identifier.target as? JKJavaFieldImpl ?: continue
                val parameter =
                    (assignmentExpression.expression as? JKFieldAccessExpression)?.identifier?.target as? JKParameter ?: continue
                if (element.parameters.contains(parameter)) {
                    val fieldDeclaration = containingClass.declarationList.find {
                        (it as? JKVariable)?.name?.value == fieldTarget.name.value
                    } as? JKVariable ?: continue
                    if (!fieldDeclaration.type.type.equalsByName(parameter.type.type)) continue//TODO better way to compare types??
//                    parameter.modifierList = fieldDeclaration::modifierList.detached()
                    declarationsToRemove += fieldDeclaration

                    if (parameter.name.value != fieldTarget.name.value) {
                        parameter.name = JKNameIdentifierImpl(fieldTarget.name.value)
                    }
                    removedStatements += constructorStatement
                }
            }
            initDeclarationStatements.addAll(element.block.statements - removedStatements)
            element.block.statements = emptyList()
            return element
        }

        private fun JKJavaAssignmentExpression.smartField(): JKFieldAccessExpression? {
            val field = this.field
            return when (field) {
                is JKQualifiedExpression ->
                    (field.selector as? JKFieldAccessExpression)?.takeIf { field.receiver is JKThisExpression }
                is JKFieldAccessExpression -> field
                else -> null
            }
        }
    }
}
