/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.backend.utils.defaultTypeWithoutArguments
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class PowerAssertBuiltIns(
    private val context: IrPluginContext,
) {
    companion object {
        const val PLUGIN_ID = "org.jetbrains.kotlin.powerassert"

        private fun dependencyError(): Nothing {
            error("Power-Assert plugin runtime dependency was not found.")
        }

        private fun classId(identifier: String): ClassId =
            ClassId(packageFqName, Name.identifier(identifier))

        private fun classId(parent: ClassId, identifier: String): ClassId =
            parent.createNestedClassId(Name.identifier(identifier))

        private fun callableId(identifier: String): CallableId =
            CallableId(packageFqName, Name.identifier(identifier))

        private fun IrClassSymbol.primaryConstructor(): IrConstructorSymbol =
            constructors.singleOrNull { it.owner.isPrimary } ?: dependencyError()

        val packageFqName = FqName("kotlin.explain")

        val explainFqName = packageFqName.child(Name.identifier("Explain"))
        val explainClassId = ClassId.topLevel(explainFqName)

        val explainIgnoreFqName = packageFqName.child(Name.identifier("ExplainIgnore"))
        val explainIgnoreClassId = ClassId.topLevel(explainIgnoreFqName)

        val explainCallFqName = packageFqName.child(Name.identifier("ExplainCall"))
        val explainCallClassId = ClassId.topLevel(explainCallFqName)

        private val callExplanationFqName = packageFqName.child(Name.identifier("CallExplanation"))
        private val callExplanationClassId = ClassId.topLevel(callExplanationFqName)
    }

    val metadata = PowerAssertMetadata(context.languageVersionSettings.languageVersion)

    private fun referenceClass(classId: ClassId): IrClassSymbol =
        context.referenceClass(classId) ?: dependencyError()

    private fun referenceFunction(callableId: CallableId): IrSimpleFunctionSymbol =
        context.referenceFunctions(callableId).singleOrNull() ?: dependencyError()

    val explainClass = referenceClass(explainClassId)
    val explainType = explainClass.defaultTypeWithoutArguments

    val explainIgnoreClass = referenceClass(explainIgnoreClassId)
    val explainIgnoreType = explainIgnoreClass.defaultTypeWithoutArguments

    val explainCallClass = referenceClass(explainCallClassId)
    val explainCallType = explainCallClass.defaultTypeWithoutArguments
    val explainCallConstructor = explainCallClass.primaryConstructor()

    val expressionClass = referenceClass(classId("Expression"))
    val expressionType = expressionClass.defaultTypeWithoutArguments

    val valueExpressionClass = referenceClass(classId("ValueExpression"))
    val valueExpressionType = valueExpressionClass.defaultTypeWithoutArguments
    val valueExpressionConstructor = valueExpressionClass.primaryConstructor()

    val equalityExpressionClass = referenceClass(classId("EqualityExpression"))
    val equalityExpressionType = equalityExpressionClass.defaultTypeWithoutArguments
    val equalityExpressionConstructor = equalityExpressionClass.primaryConstructor()

    val variableAccessExpressionClass = referenceClass(classId("VariableAccessExpression"))
    val variableAccessExpressionType = variableAccessExpressionClass.defaultTypeWithoutArguments
    val variableAccessExpressionConstructor = variableAccessExpressionClass.primaryConstructor()

    val callExplanationClass = referenceClass(callExplanationClassId)
    val callExplanationType = callExplanationClass.defaultTypeWithoutArguments
    val callExplanationConstructor = callExplanationClass.primaryConstructor()
    val toDefaultMessageFunction = referenceFunction(callableId("toDefaultMessage"))

    val valueArgumentClass = referenceClass(classId(callExplanationClassId, "ValueArgument"))
    val valueArgumentType = valueArgumentClass.defaultTypeWithoutArguments
    val valueArgumentConstructor = valueArgumentClass.primaryConstructor()

    val receiverClass = referenceClass(classId(callExplanationClassId, "Receiver"))
    val receiverType = receiverClass.defaultTypeWithoutArguments
    val receiverConstructor = receiverClass.primaryConstructor()

    private val variableExplanationClassId = classId("VariableExplanation")
    val variableExplanationClass = referenceClass(variableExplanationClassId)
    val variableExplanationType = variableExplanationClass.defaultTypeWithoutArguments
    val variableExplanationConstructor = variableExplanationClass.primaryConstructor()

    val initializerClass = referenceClass(classId(variableExplanationClassId, "Initializer"))
    val initializerType = initializerClass.defaultTypeWithoutArguments
    val initializerConstructor = initializerClass.primaryConstructor()

    // -----

    val stringType get() = context.irBuiltIns.stringType

    val pairClass = referenceClass(ClassId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("Pair")))
    val pairConstructor = pairClass.primaryConstructor()
    fun pairType(firstType: IrType, secondType: IrType) = pairClass.typeWith(firstType, secondType)

    val mapOfFunction = context.referenceFunctions(CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("mapOf")))
        .singleOrNull { it.owner.valueParameters.firstOrNull()?.isVararg == true } ?: dependencyError()

    val listOfFunction = context.referenceFunctions(CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("listOf")))
        .singleOrNull { it.owner.valueParameters.firstOrNull()?.isVararg == true } ?: dependencyError()
}