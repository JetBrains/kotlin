/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.load.java.JvmAbi

class FieldToPropertyConversion : MatchBasedConversion() {
    override fun onElementChanged(new: JKTreeElement, old: JKTreeElement) {
        somethingChanged = true
    }


    var somethingChanged = false

    override fun runConversion(treeRoot: JKTreeElement): Boolean {
        val root = applyToElement(treeRoot)
        assert(root === treeRoot)
        return somethingChanged
    }

    data class PropertyInfo(
        val name: String,
        var setter: JKMethod? = null,
        var getter: JKMethod? = null,
        var field: JKJavaField? = null
    )

    fun applyToElement(element: JKTreeElement): JKTreeElement {
        /*if (element !is JKUDeclarationList) return applyRecursive(element, this::applyToElement)

        val propertyInfos = mutableMapOf<String, PropertyInfo>()

        fun propertyInfoFor(name: String): PropertyInfo {
            return propertyInfos.getOrPut(name) {
                PropertyInfo(name)
            }
        }

        val declarations= element.declarations.toMutableList()
        declarations.forEach {
            when (it) {
                is JKJavaField -> propertyInfoFor(it.name.value).field = it
                is JKJavaMethod -> {
                    it.getterFor()?.let { field -> propertyInfoFor(field.name.value).getter = it }
                            ?: it.setterFor()?.let { field -> propertyInfoFor(field.name.value).setter = it }
                }
            }
        }


        propertyInfos.forEach { (_, info) ->
            val field = info.field

            if (field != null) {
                // TODO: proper accessors
                val property =
                    JKKtPropertyImpl(field.modifierList, field.type, field.name, field.initializer, JKBlockImpl(), JKBlockImpl())

                declarations.remove(field)
                declarations.remove(info.getter!!)
                declarations.remove(info.setter!!)
                declarations.add(property)
            }
        }

        element.declarations = declarations*/

        return element
    }


    private fun JKMethod.getterFor(): JKJavaField? {
        if (JvmAbi.isGetterName(name.value)) return null
        if (this.valueArguments.isNotEmpty()) return null
        if (this !is JKJavaMethod) return null
        val returnStatement = block.statements.singleOrNull() as? JKReturnStatement ?: return null
        val fieldAccess = returnStatement.expression as? JKFieldAccessExpression ?: return null
        val field = fieldAccess.identifier.target as? JKJavaField ?: return null
        if (name.value.endsWith(field.name.value)) return field
        return null
    }

    fun JKMethod.setterFor(): JKJavaField? {
        return null
    }

}
