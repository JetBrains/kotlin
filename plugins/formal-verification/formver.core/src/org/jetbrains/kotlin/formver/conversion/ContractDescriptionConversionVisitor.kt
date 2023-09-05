/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.description.ConeContractConstantValues
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.domains.TypeDomain
import org.jetbrains.kotlin.formver.domains.TypeOfDomain
import org.jetbrains.kotlin.formver.embeddings.NullableTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.*

object ContractDescriptionConversionVisitor : KtContractDescriptionVisitor<Exp, MethodConversionContext, ConeKotlinType, ConeDiagnostic>() {
    private fun KtValueParameterReference<ConeKotlinType, ConeDiagnostic>.embeddedVar(data: MethodConversionContext): VariableEmbedding =
        data.signature.params[parameterIndex]


    private fun VariableEmbedding.nullCmp(isNegated: Boolean): Exp =
        if (type is NullableTypeEmbedding) {
            if (isNegated) {
                NeCmp(toLocalVar(), type.nullVal)
            } else {
                EqCmp(toLocalVar(), type.nullVal)
            }
        } else {
            BoolLit(isNegated)
        }

    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: KtBooleanConstantReference<ConeKotlinType, ConeDiagnostic>,
        data: MethodConversionContext,
    ): Exp = when (booleanConstantDescriptor) {
        ConeContractConstantValues.TRUE -> BoolLit(true)
        ConeContractConstantValues.FALSE -> BoolLit(false)
        else -> throw Exception("Unexpected boolean constant: $booleanConstantDescriptor")
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
        val param = isNullPredicate.arg.embeddedVar(data)
        return param.nullCmp(isNullPredicate.isNegated)
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

    override fun visitCallsEffectDeclaration(
        callsEffect: KtCallsEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
        data: MethodConversionContext,
    ): Exp {
        val param = callsEffect.valueParameterReference.accept(this, data)
        val callsFieldAccess = FieldAccess(param, SpecialFields.FunctionObjectCallCounterField)
        return when (callsEffect.kind) {
            // NOTE: case not supported for contracts
            EventOccurrencesRange.ZERO -> EqCmp(
                callsFieldAccess,
                Old(callsFieldAccess)
            )
            EventOccurrencesRange.AT_MOST_ONCE -> LeCmp(
                callsFieldAccess,
                Add(Old(callsFieldAccess), IntLit(1))
            )
            EventOccurrencesRange.EXACTLY_ONCE -> EqCmp(
                callsFieldAccess,
                Add(Old(callsFieldAccess), IntLit(1))
            )
            EventOccurrencesRange.AT_LEAST_ONCE -> GtCmp(
                callsFieldAccess,
                Old(callsFieldAccess)
            )
            // NOTE: case not supported for contracts
            EventOccurrencesRange.MORE_THAN_ONCE -> GtCmp(
                callsFieldAccess,
                Add(Old(callsFieldAccess), IntLit(1))
            )
            EventOccurrencesRange.UNKNOWN -> BoolLit(true)
        }
    }

    override fun visitIsInstancePredicate(
        isInstancePredicate: KtIsInstancePredicate<ConeKotlinType, ConeDiagnostic>,
        data: MethodConversionContext
    ): Exp {
        val argVar = isInstancePredicate.arg.embeddedVar(data)
        return TypeDomain.isSubtype(TypeOfDomain.typeOf(argVar.toLocalVar()), data.embedType(isInstancePredicate.type).kotlinType)
    }
}