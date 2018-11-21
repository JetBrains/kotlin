/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.ast.Mutability
import org.jetbrains.kotlin.j2k.copyTreeAndDetach
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.load.java.JvmAbi

class FieldToPropertyConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    private data class PropertyInfo(
        val name: String,
        var setter: AccessorInfo? = null,
        var getter: AccessorInfo? = null,
        var field: JKJavaField? = null
    )

    private data class AccessorInfo(
        var method: JKMethod,
        val target: JKJavaField?
    )

    private fun AccessorInfo.isTrivialFor(field: JKJavaField) =
        target == field

    private fun <T : JKTreeElement> T.renamed(
        fromName: String,
        toName: String,
        type: JKType,
        renameType: RenameType
    ): T =
        also {
            Renamer(fromName, toName, type, renameType).runConversion(it, context)
        }

    private fun PropertyInfo.toKtProperty(): JKKtProperty? {
        val propertyField = field
            ?: getter?.method
                ?.takeIf { propertyNameFromGet(it) != null }
                ?.let {
                    JKJavaFieldImpl(
                        JKModifierListImpl(),
                        JKTypeElementImpl(it.returnType.type),
                        JKNameIdentifierImpl(propertyNameFromGet(it)!!),
                        JKStubExpressionImpl()
                    )
                }
            ?: return null

        val ktGetter = getter?.let {
            val method = it.method
            val block = if (it.isTrivialFor(propertyField)) JKBodyStub else method::block.detached()
            JKKtGetterOrSetterImpl(
                JKBlockStatementImpl(block)
                    .renamed(propertyField.name.value, "field", propertyField.type.type, RenameType.RENAME_FIELD_ACCESS),
                JKModifierListImpl(JKAccessModifierImpl(method.modifierList.visibility)),
                JKKtGetterOrSetter.Kind.GETTER
            )
        } ?: JKKtGetterOrSetterImpl(
            JKBlockStatementImpl(JKBodyStub),
            JKModifierListImpl(JKAccessModifierImpl(propertyField.modifierList.visibility)),
            JKKtGetterOrSetter.Kind.GETTER
        )

        val ktSetter = setter?.let {
            val method = it.method
            val block = if (it.isTrivialFor(propertyField)) JKBodyStub else method::block.detached()
            JKKtGetterOrSetterImpl(
                JKBlockStatementImpl(block)
                    .renamed(
                        method.parameters.single().name.value,
                        "value",
                        method.parameters.single().type.type,
                        RenameType.RENAME_PARAMETER_ACCESS
                    )
                    .renamed(propertyField.name.value, "field", propertyField.type.type, RenameType.RENAME_FIELD_ACCESS),
                JKModifierListImpl(JKAccessModifierImpl(method.modifierList.visibility)),
                JKKtGetterOrSetter.Kind.SETTER
            )
        } ?: JKKtEmptyGetterOrSetterImpl()
        propertyField.invalidate()

        return JKKtPropertyImpl(
            propertyField.modifierList,
            propertyField.type,
            propertyField.name,
            propertyField.initializer,
            ktGetter,
            ktSetter
        ).also {
            it.modifierList.mutability =
                    if (it.modifierList.visibilityOrNull() == JKAccessModifier.Visibility.PRIVATE && ktSetter is JKKtEmptyGetterOrSetter)
                        Mutability.NonMutable
                    else Mutability.Mutable

            it.modifierList.visibility =
                    listOf(
                        ktGetter.modifierList.visibility,
                        ktGetter.modifierList.visibility
                    ).min() ?: JKAccessModifier.Visibility.PUBLIC
        }
    }

    private fun JKJavaMethod.isGetterOrSetter() =
        propertyNameFromGet(this) != null || propertyNameFromSet(this) != null

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return applyRecursive(element, this::applyToElement)

        val propertyInfos = mutableMapOf<String, PropertyInfo>()

        fun propertyInfoFor(name: String): PropertyInfo {
            return propertyInfos.getOrPut(name) {
                PropertyInfo(name)
            }
        }

        val declarations = element.declarationList
        declarations.forEach {
            when (it) {
                is JKJavaField ->
                    propertyInfoFor(it.name.value).field = it
                is JKJavaMethod -> {
                    propertyNameFromGet(it)?.let { fieldName ->
                        propertyInfoFor(fieldName).getter = AccessorInfo(it, it.fieldFromGetter())
                    } ?: propertyNameFromSet(it)?.let { fieldName ->
                        propertyInfoFor(fieldName).setter = AccessorInfo(it, it.fieldFromSetter())
                    }
                }
            }
        }

        element.declarationList =
                declarations.mapNotNull { declaration ->
                    when (declaration) {
                        is JKJavaField -> propertyInfoFor(declaration.name.value).toKtProperty()
                        is JKJavaMethod ->
                            if (declaration.isGetterOrSetter()) {
                                propertyNameFromGet(declaration)?.let { fieldName ->
                                    propertyInfoFor(fieldName)
                                }?.takeIf { it.field == null }?.toKtProperty()
                            } else declaration
                        else -> declaration
                    }
                }
        return recurse(element)
    }


    private enum class RenameType {
        RENAME_FIELD_ACCESS,
        RENAME_PARAMETER_ACCESS
    }

    private inner class Renamer(
        private val fromName: String,
        private val toName: String,
        private val type: JKType,
        private val renameType: RenameType
    ) : RecursiveApplicableConversionBase() {
        override fun applyToElement(element: JKTreeElement): JKTreeElement =
            recurse(
                when (renameType) {
                    RenameType.RENAME_FIELD_ACCESS -> renameFieldAccessExpression(element)
                    RenameType.RENAME_PARAMETER_ACCESS -> renameParameter(element)
                } ?: element
            )

        private fun renameFieldAccessExpression(element: JKTreeElement): JKFieldAccessExpressionImpl? =
            (element as? JKExpression)?.unboxFieldReference()?.let {
                if (it.identifier.name == fromName) JKFieldAccessExpressionImpl(createSymbol())
                else null
            }


        private fun renameParameter(element: JKTreeElement): JKFieldAccessExpressionImpl? {
            if (element !is JKFieldAccessExpression) return null
            val target = element.identifier.target as? JKParameter ?: return null
            return if (target.name.value == fromName) {
                JKFieldAccessExpressionImpl(createSymbol())
            } else null
        }

        private fun createSymbol() = object : JKFieldSymbol {
            override val target: String = toName
            override val declaredIn: JKSymbol? = null
            override val name: String = toName
            override val fqName: String = toName
            override val fieldType: JKType = type
        }
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

    private fun JKMethod.fieldFromGetter(): JKJavaField? {
        if (this !is JKJavaMethod) return null
        val returnStatement = block.statements.singleOrNull() as? JKReturnStatement ?: return null
        val fieldAccess = returnStatement.expression.unboxFieldReference() ?: return null
        return fieldAccess.identifier.target as? JKJavaField
    }

    private fun JKMethod.fieldFromSetter(): JKJavaField? {
        if (this !is JKJavaMethod) return null
        val expressionStatement = this.block.statements.singleOrNull() as? JKExpressionStatement ?: return null
        val assignment = expressionStatement.expression as? JKJavaAssignmentExpression ?: return null
        val lhs = assignment.field
        val fieldAccess = lhs.unboxFieldReference()
        return fieldAccess?.identifier?.target as? JKJavaField
    }
}
