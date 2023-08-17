/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.description.ConeContractConstantValues
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.domains.NullableDomain
import org.jetbrains.kotlin.formver.embeddings.NullableTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.scala.MangledName
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp.*

class ContractDescriptionConversionVisitor : KtContractDescriptionVisitor<Exp, MethodConversionContext, ConeKotlinType, ConeDiagnostic>() {
    private fun KtValueParameterReference<ConeKotlinType, ConeDiagnostic>.embeddedVar(data: MethodConversionContext): VariableEmbedding {
        val name = data.signature.params[parameterIndex].name
        val type = data.signature.params[parameterIndex].type
        return VariableEmbedding(name, type)
    }

    private fun VariableEmbedding.nullCmp(isNegated: Boolean): Exp {
        return if (type is NullableTypeEmbedding) {
            if (isNegated) {
                NeCmp(toLocalVar(), NullableDomain.nullVal(type.elementType.type))
            } else {
                EqCmp(toLocalVar(), NullableDomain.nullVal(type.elementType.type))
            }
        } else {
            BoolLit(isNegated)
        }
    }

    private fun KtValueParameterReference<ConeKotlinType, ConeDiagnostic>.embeddedName(data: MethodConversionContext): MangledName {
        return data.signature.params[parameterIndex].name
    }

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: KtBooleanConstantReference<ConeKotlinType, ConeDiagnostic>,
        data: MethodConversionContext,
    ): Exp {
        return when (booleanConstantDescriptor) {
            ConeContractConstantValues.TRUE -> BoolLit(true)
            ConeContractConstantValues.FALSE -> BoolLit(false)
            else -> throw Exception("Unexpected boolean constant: $booleanConstantDescriptor")
        }
    }

    override fun visitReturnsEffectDeclaration(
        returnsEffect: KtReturnsEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
        data: MethodConversionContext,
    ): Exp {
        val retVar = data.signature.returnVar.toLocalVar()
        return when (returnsEffect.value) {
            ConeContractConstantValues.WILDCARD -> BoolLit(true)
            /* NOTE: in a function that has a non-nullable return type, the compiler will not complain if there is an effect like
             * returnsNotNull(). So it is necessary to take care of these cases in order to avoid comparison between non-nullable
             * values and null. In a function that has a non-nullable return type, returnsNotNull() is mapped to true and returns(null)
             * is mapped to false
             */
            ConeContractConstantValues.NULL -> data.signature.returnVar.nullCmp(false)
            ConeContractConstantValues.NOT_NULL -> data.signature.returnVar.nullCmp(true)
            ConeContractConstantValues.TRUE -> EqCmp(retVar, BoolLit(true))
            ConeContractConstantValues.FALSE -> EqCmp(retVar, BoolLit(false))
            else -> throw Exception("Unexpected constant: ${returnsEffect.value}")
        }
    }

    override fun visitValueParameterReference(
        valueParameterReference: KtValueParameterReference<ConeKotlinType, ConeDiagnostic>,
        data: MethodConversionContext,
    ): Exp = valueParameterReference.embeddedVar(data).toLocalVar()

    override fun visitIsNullPredicate(
        isNullPredicate: KtIsNullPredicate<ConeKotlinType, ConeDiagnostic>,
        data: MethodConversionContext,
    ): Exp {
        /* NOTE: useless comparisons like x != null with x non-nullable will compile with just a warning.
         * So it is necessary to take care of these cases in order to avoid comparison between non-nullable
         * values and null. Let x be a non-nullable variable, then x == null is mapped to false and x != null is mapped to true
         */
        val arg = data.signature.params[isNullPredicate.arg.parameterIndex]
        return arg.nullCmp(isNullPredicate.isNegated)
    }

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: KtBinaryLogicExpression<ConeKotlinType, ConeDiagnostic>,
        data: MethodConversionContext,
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
        data: MethodConversionContext,
    ): Exp {
        val effect = conditionalEffect.effect.accept(this, data)
        val cond = conditionalEffect.condition.accept(this, data)
        return Implies(effect, cond)
    }
}