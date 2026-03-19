/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.extensions.DeclarationFinder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.invokeFun
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.powerassert.PowerAssertNames.CALL_EXPLANATION_CLASS_ID
import org.jetbrains.kotlin.powerassert.PowerAssertNames.POWER_ASSERT_CLASS_ID
import org.jetbrains.kotlin.powerassert.PowerAssertNames.POWER_ASSERT_PACKAGE_FQ_NAME

class PowerAssertBuiltIns private constructor(
    irBuiltIns: IrBuiltIns,
    finder: DeclarationFinder,
    val metadata: PowerAssertMetadata,
) {
    companion object {
        fun from(context: IrPluginContext): PowerAssertBuiltIns? {
            val finder = context.finderForBuiltins()
            finder.findClass(POWER_ASSERT_CLASS_ID) ?: return null
            return PowerAssertBuiltIns(
                context.irBuiltIns,
                finder,
                PowerAssertMetadata(context.languageVersionSettings.languageVersion),
            )
        }

        const val PLUGIN_ID = "org.jetbrains.kotlin.powerassert"

        private fun dependencyError(message: String? = null): Nothing {
            if (message != null) {
                error("Power-Assert plugin runtime dependency was not found: $message")
            } else {
                error("Power-Assert plugin runtime dependency was not found.")
            }
        }

        private fun classId(identifier: String): ClassId =
            ClassId(POWER_ASSERT_PACKAGE_FQ_NAME, Name.identifier(identifier))

        private fun classId(parent: ClassId, identifier: String): ClassId =
            parent.createNestedClassId(Name.identifier(identifier))

        private fun callableId(identifier: String): CallableId =
            CallableId(POWER_ASSERT_PACKAGE_FQ_NAME, Name.identifier(identifier))


        private fun DeclarationFinder.findClassOrError(classId: ClassId): IrClassSymbol =
            findClass(classId) ?: dependencyError(classId.toString())

        private fun DeclarationFinder.findFunctionOrError(callableId: CallableId): IrSimpleFunctionSymbol =
            findFunctions(callableId).singleOrNull() ?: dependencyError()

        private val IrClassSymbol.primaryConstructorOrError: IrConstructorSymbol
            get() = owner.primaryConstructor?.symbol ?: dependencyError()

        private val argumentClassId = classId(CALL_EXPLANATION_CLASS_ID, "Argument")
        private val kindClassId = classId(argumentClassId, "Kind")
    }

    val expressionClass = finder.findClassOrError(classId("Expression"))
    val expressionType = expressionClass.defaultType

    val valueExpressionClass = finder.findClassOrError(classId("ValueExpression"))
    val valueExpressionConstructor = valueExpressionClass.primaryConstructorOrError

    val literalExpressionClass = finder.findClassOrError(classId("LiteralExpression"))
    val literalExpressionConstructor = literalExpressionClass.primaryConstructorOrError

    val equalityExpressionClass = finder.findClassOrError(classId("EqualityExpression"))
    val equalityExpressionConstructor = equalityExpressionClass.primaryConstructorOrError

    val callExplanationClass = finder.findClassOrError(CALL_EXPLANATION_CLASS_ID)
    val callExplanationType = callExplanationClass.defaultType
    val callExplanationConstructor = callExplanationClass.primaryConstructorOrError
    val function0CallExplanationType = irBuiltIns.functionN(0).typeWith(callExplanationType)
    val toDefaultMessageFunction = finder.findFunctionOrError(callableId("toDefaultMessage"))

    val argumentClass = finder.findClassOrError(argumentClassId)
    val argumentType = argumentClass.defaultType
    val argumentConstructor = argumentClass.primaryConstructorOrError

    val argumentKindClass = finder.findClassOrError(kindClassId)
    val argumentKindType = argumentKindClass.defaultType

    // -----

    val listOfFunction = finder.findFunctions(CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("listOf")))
        .singleOrNull { it.owner.parameters.firstOrNull()?.isVararg == true } ?: dependencyError()

    val jvmSyntheticAnnotation = finder.findConstructors(JvmStandardClassIds.JVM_SYNTHETIC_ANNOTATION_CLASS_ID)
        .single()

    val function0invoke = irBuiltIns.functionN(0).invokeFun ?: dependencyError()
}