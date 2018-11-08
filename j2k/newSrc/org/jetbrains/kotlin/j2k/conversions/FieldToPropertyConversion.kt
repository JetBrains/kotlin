/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ast.Mutability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKBlockImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKKtPropertyImpl
import org.jetbrains.kotlin.j2k.tree.impl.mutability
import org.jetbrains.kotlin.j2k.tree.impl.visibility
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FieldToPropertyConversion : RecursiveApplicableConversionBase() {

    data class PropertyInfo(
        val name: String,
        var setter: JKMethod? = null,
        var getter: JKMethod? = null,
        var field: JKJavaField? = null
    )

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return applyRecursive(element, this::applyToElement)

        val propertyInfos = mutableMapOf<String, PropertyInfo>()

        fun propertyInfoFor(name: String): PropertyInfo {
            return propertyInfos.getOrPut(name) {
                PropertyInfo(name)
            }
        }

        val declarations = element.declarationList.toMutableList()
        declarations.forEach {
            when (it) {
                is JKJavaField -> propertyInfoFor(it.name.value).field = it
                is JKJavaMethod -> {
                    propertyNameFromGet(it)?.let { fieldName -> propertyInfoFor(fieldName).getter = it }
                        ?: propertyNameFromSet(it)?.let { fieldName -> propertyInfoFor(fieldName).setter = it }
                }
            }
        }


        for ((_, info) in propertyInfos) {
            val field = info.field

            if (field != null) {
                // TODO: proper accessors
                field.invalidate()
                val property =
                    JKKtPropertyImpl(field.modifierList, field.type, field.name, field.initializer, JKBlockImpl(), JKBlockImpl())

                declarations.remove(field)
                property.modifierList.mutability = Mutability.NonMutable
                val getter = info.getter
                if (getter?.fieldFromGetter(field.name.value) == field) {
                    declarations.remove(getter)
                    property.modifierList.mutability = Mutability.NonMutable
                    property.modifierList.visibility = minOf(getter.modifierList.visibility, property.modifierList.visibility)
                }
                val setter = info.setter
                if (setter?.fieldFromSetter(field.name.value) == field) {
                    declarations.remove(setter)
                    property.modifierList.mutability = Mutability.Mutable
                    property.modifierList.visibility = minOf(setter.modifierList.visibility, property.modifierList.visibility)
                }
                declarations.add(property)
            }
        }

        element.declarationList = declarations

        return element
    }


    private fun propertyNameFromGet(method: JKMethod): String? {
        if (!JvmAbi.isGetterName(method.name.value)) return null
        if (method.parameters.isNotEmpty()) return null
        if (method !is JKJavaMethod) return null
        return method.name.value
            .removePrefix("get")
            .removePrefix("is")
            .decapitalize()
    }

    private fun propertyNameFromSet(method: JKMethod): String? {
        if (!JvmAbi.isSetterName(method.name.value)) return null
        if (method.parameters.size != 1) return null
        if (method !is JKJavaMethod) return null
        return method.name.value
            .removePrefix("set")
            .decapitalize()
    }

    private fun JKExpression.unboxFieldReference(): JKFieldAccessExpression? = when {
        this is JKFieldAccessExpression -> this
        this is JKQualifiedExpression && receiver is JKThisExpression -> selector as? JKFieldAccessExpression
        else -> null
    }

    private fun JKMethod.fieldFromGetter(propertyName: String): JKJavaField? {
        if (this !is JKJavaMethod) return null
        val returnStatement = block.statements.singleOrNull() as? JKReturnStatement ?: return null
        val fieldAccess = returnStatement.expression.unboxFieldReference() ?: return null
        val field = fieldAccess.identifier.target as? JKJavaField ?: return null
        if (propertyName != field.name.value) return null
        return field
    }

    private fun JKMethod.fieldFromSetter(propertyName: String): JKJavaField? {
        if (this !is JKJavaMethod) return null
        val expressionStatement = this.block.statements.singleOrNull() as? JKExpressionStatement ?: return null
        val assignment = expressionStatement.expression as? JKJavaAssignmentExpression ?: return null
        val lhs = assignment.field
        val fieldAccess = lhs.unboxFieldReference()
        val target = fieldAccess?.identifier?.target as? JKJavaField ?: return null
        if (propertyName != target.name.value) return null
        return target
    }


    private data class AccessorInfo(
        val method: JKMethod,
        val field: JKField?,
        val propertyName: String
    )
}
