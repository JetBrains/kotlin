/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKJavaFieldImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKNameIdentifierImpl


class FieldInitializersInPrimaryFromParamsConversion : TransformerBasedConversion() {
    override fun visitTreeElement(treeElement: JKTreeElement) {
        treeElement.acceptChildren(this, null)
    }

    override fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor) {
        for (constructorStatement in ktPrimaryConstructor.block.statements) {
            val assignmentExpression =
                (constructorStatement as? JKExpressionStatement)?.expression as? JKJavaAssignmentExpression ?: continue
            //TODO check if operator is `=`
            val smartField = assignmentExpression.smartField() ?: continue
            val fieldTarget = smartField.identifier.target as? JKJavaFieldImpl ?: continue
            val parameter =
                (assignmentExpression.expression as? JKFieldAccessExpression)?.identifier?.target as? JKParameter ?: continue
            if (ktPrimaryConstructor.parameters.contains(parameter)) {
                val containingClass = ktPrimaryConstructor.parentOfType<JKClass>() ?: continue
                val fieldDeclaration = containingClass.declarationList.find {
                    (it as? JKField)?.name?.value == fieldTarget.name.value
                } ?: continue
                parameter.modifierList = (fieldDeclaration as JKField).modifierList.also { it.detach(it.parent!!) }
                containingClass.declarationList -= fieldDeclaration

                if (parameter.name.value != fieldTarget.name.value) {
                    parameter.name = JKNameIdentifierImpl(fieldTarget.name.value)
                }
                somethingChanged = true
            }
        }
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
