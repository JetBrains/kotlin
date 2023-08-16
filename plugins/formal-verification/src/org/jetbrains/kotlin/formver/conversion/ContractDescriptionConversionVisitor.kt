/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.description.ConeContractConstantValues
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp.*

class ContractDescriptionConversionVisitor : KtContractDescriptionVisitor<Exp, MethodConversionContext, ConeKotlinType, ConeDiagnostic>() {
    private fun KtValueParameterReference<ConeKotlinType, ConeDiagnostic>.convertedName(data: MethodConversionContext): ConvertedName {
        return data.signature.params[parameterIndex].name
    }

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: KtBooleanConstantReference<ConeKotlinType, ConeDiagnostic>,
        data: MethodConversionContext
    ): Exp {
        return when (booleanConstantDescriptor) {
            ConeContractConstantValues.TRUE -> BoolLit(true)
            ConeContractConstantValues.FALSE -> BoolLit(false)
            else -> throw Exception("Unexpected boolean constant: $booleanConstantDescriptor")
        }
    }

    override fun visitReturnsEffectDeclaration(
        returnsEffect: KtReturnsEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
        data: MethodConversionContext
    ): Exp {
        val retVar = data.returnVar.toLocalVar()
        return when (returnsEffect.value) {
            ConeContractConstantValues.WILDCARD -> BoolLit(true)
            ConeContractConstantValues.NULL -> EqCmp(retVar, NullLit())
            ConeContractConstantValues.NOT_NULL -> NeCmp(retVar, NullLit())
            ConeContractConstantValues.TRUE -> EqCmp(retVar, BoolLit(true))
            ConeContractConstantValues.FALSE -> EqCmp(retVar, BoolLit(false))
            else -> throw Exception("Unexpected constant: ${returnsEffect.value}")
        }
    }

    override fun visitBooleanValueParameterReference(
        booleanValueParameterReference: KtBooleanValueParameterReference<ConeKotlinType, ConeDiagnostic>,
        data: MethodConversionContext
    ): Exp = ConvertedVar(booleanValueParameterReference.convertedName(data), ConvertedBoolean).toLocalVar()

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: KtBinaryLogicExpression<ConeKotlinType, ConeDiagnostic>,
        data: MethodConversionContext
    ): Exp {
        val left = binaryLogicExpression.left.accept(this, data)
        val right = binaryLogicExpression.right.accept(this, data)
        return when (binaryLogicExpression.kind) {
            LogicOperationKind.AND -> And(left, right)
            LogicOperationKind.OR -> Or(left, right)
        }
    }

    override fun visitLogicalNot(logicalNot: KtLogicalNot<ConeKotlinType, ConeDiagnostic>, data: MethodConversionContext): Exp {
        val arg = logicalNot.arg.accept(this, data)
        return Not(arg)
    }

    override fun visitConditionalEffectDeclaration(
        conditionalEffect: KtConditionalEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
        data: MethodConversionContext
    ): Exp {
        val effect = conditionalEffect.effect.accept(this, data)
        val cond = conditionalEffect.condition.accept(this, data)
        return Implies(effect, cond)
    }
}