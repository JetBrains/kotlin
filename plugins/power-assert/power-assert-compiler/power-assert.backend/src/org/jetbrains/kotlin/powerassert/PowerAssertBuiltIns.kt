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
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
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
import org.jetbrains.kotlin.powerassert.PowerAssertNames.CALL_EXPLANATION_ARGUMENT_CLASS_ID
import org.jetbrains.kotlin.powerassert.PowerAssertNames.CALL_EXPLANATION_ARGUMENT_KIND_CLASS_ID
import org.jetbrains.kotlin.powerassert.PowerAssertNames.CALL_EXPLANATION_CLASS_ID
import org.jetbrains.kotlin.powerassert.PowerAssertNames.EQUALITY_EXPRESSION_CLASS_ID
import org.jetbrains.kotlin.powerassert.PowerAssertNames.EXPLANATION_TO_DEFAULT_MESSAGE_CALLABLE_ID
import org.jetbrains.kotlin.powerassert.PowerAssertNames.EXPRESSION_CLASS_ID
import org.jetbrains.kotlin.powerassert.PowerAssertNames.LITERAL_EXPRESSION_CLASS_ID
import org.jetbrains.kotlin.powerassert.PowerAssertNames.POWER_ASSERT_CLASS_ID
import org.jetbrains.kotlin.powerassert.PowerAssertNames.POWER_ASSERT_EXPLANATION_CALLABLE_ID
import org.jetbrains.kotlin.powerassert.PowerAssertNames.VALUE_EXPRESSION_CLASS_ID

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
                error("Power-Assert plugin runtime dependency error: $message")
            } else {
                error("Power-Assert plugin runtime dependency error.")
            }
        }

        private fun DeclarationFinder.findClassOrError(classId: ClassId): IrClassSymbol =
            findClass(classId) ?: dependencyError("class not found '$classId'")

        private fun DeclarationFinder.findFunctionOrError(callableId: CallableId): IrSimpleFunctionSymbol {
            val found = findFunctions(callableId)
            return when (found.size) {
                0 -> dependencyError("function not found '$callableId'")
                1 -> found.single()
                else -> dependencyError("multiple functions found for '$callableId': $found")
            }
        }

        private fun DeclarationFinder.findPropertyOrError(callableId: CallableId): IrPropertySymbol {
            val found = findProperties(callableId)
            return when (found.size) {
                0 -> dependencyError("property not found '$callableId'")
                1 -> found.single()
                else -> dependencyError("multiple properties found for '$callableId': $found")
            }
        }

        private val IrClassSymbol.primaryConstructorOrError: IrConstructorSymbol
            get() = owner.primaryConstructor?.symbol ?: dependencyError()
    }

    private val powerAssertExplanationProperty = finder.findPropertyOrError(POWER_ASSERT_EXPLANATION_CALLABLE_ID)
    val powerAssertExplanationGetter = powerAssertExplanationProperty.owner.getter?.symbol ?: dependencyError()

    private val expressionClass = finder.findClassOrError(EXPRESSION_CLASS_ID)
    val expressionType = expressionClass.defaultType

    private val valueExpressionClass = finder.findClassOrError(VALUE_EXPRESSION_CLASS_ID)
    val valueExpressionConstructor = valueExpressionClass.primaryConstructorOrError

    private val literalExpressionClass = finder.findClassOrError(LITERAL_EXPRESSION_CLASS_ID)
    val literalExpressionConstructor = literalExpressionClass.primaryConstructorOrError

    private val equalityExpressionClass by lazy { finder.findClassOrError(EQUALITY_EXPRESSION_CLASS_ID) }
    val equalityExpressionConstructor = equalityExpressionClass.primaryConstructorOrError

    private val callExplanationClass = finder.findClassOrError(CALL_EXPLANATION_CLASS_ID)
    val callExplanationType = callExplanationClass.defaultType
    val callExplanationConstructor = callExplanationClass.primaryConstructorOrError
    val function0CallExplanationType = irBuiltIns.functionN(0).typeWith(callExplanationType)

    private val argumentClass = finder.findClassOrError(CALL_EXPLANATION_ARGUMENT_CLASS_ID)
    val argumentType = argumentClass.defaultType
    val argumentConstructor = argumentClass.primaryConstructorOrError

    val argumentKindClass = finder.findClassOrError(CALL_EXPLANATION_ARGUMENT_KIND_CLASS_ID)
    val argumentKindType = argumentKindClass.defaultType

    val toDefaultMessageFunction = finder.findFunctionOrError(EXPLANATION_TO_DEFAULT_MESSAGE_CALLABLE_ID)

    // -----

    val listOfFunction = finder.findFunctions(CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("listOf")))
        .singleOrNull { it.owner.parameters.firstOrNull()?.isVararg == true } ?: dependencyError()

    val jvmSyntheticAnnotation = finder.findConstructors(JvmStandardClassIds.JVM_SYNTHETIC_ANNOTATION_CLASS_ID)
        .single()

    val function0invoke = irBuiltIns.functionN(0).invokeFun ?: dependencyError()
}