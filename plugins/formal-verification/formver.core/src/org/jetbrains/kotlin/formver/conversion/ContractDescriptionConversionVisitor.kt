/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.description.ConeContractConstantValues
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.effects
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.callables.NamedFunctionSignature

interface ContractDescriptionVisitorContext {
    val effectSource: KtSourceElement?
}

/**
 * NOTE: this comments explains how we assign source information to the generated Viper's Abstract Syntax Tree nodes to warn users.
 *
 * A contract has [several effects](https://kotlinlang.org/spec/kotlin-spec.html#function-contracts)
 * defined within it. We are interested in assigning a source info to each one of these effects, in a way we are able
 * to warn the user what effect is not verifying correctly.
 * The effects we have are the following: `callsInPlace`, `returns(bool) implies (value-type-implication)`.
 * Specifically, we embed the source position into Viper's node for the following effects:
 * 1. `returns()`
 * 2. `returns(...) implies (...)`
 * 3. `callsInPlace`
 */

class ContractDescriptionConversionVisitor(
    private val ctx: ProgramConversionContext,
    private val signature: NamedFunctionSignature,
) : KtContractDescriptionVisitor<ExpEmbedding, ContractDescriptionVisitorContext, ConeKotlinType, ConeDiagnostic>() {
    private val parameterIndices = signature.params.indices.toSet() + setOfNotNull(signature.receiver?.let { -1 })

    fun getPreconditions(symbol: FirFunctionSymbol<*>): List<ExpEmbedding> {
        val callsInPlaceIndices = symbol.effects
            .mapNotNull { (it.effect as? KtCallsEffectDeclaration<*, *>)?.valueParameterReference?.parameterIndex }
            .toSet()

        // All parameters of function type that are not callsInPlace should be marked duplicable.
        return (parameterIndices - callsInPlaceIndices)
            .map { embeddedVarByIndex(it) }
            .filter { it.type is FunctionTypeEmbedding }
            .map { DuplicableCall(it, it.source) }
    }

    fun getPostconditions(symbol: FirFunctionSymbol<*>): List<ExpEmbedding> {
        return symbol.effects.map {
            it.effect.accept(this, object : ContractDescriptionVisitorContext {
                override val effectSource: KtSourceElement? = it.source
            })
        }
    }


    override fun visitBooleanConstantDescriptor(
        booleanConstantDescriptor: KtBooleanConstantReference<ConeKotlinType, ConeDiagnostic>,
        data: ContractDescriptionVisitorContext,
    ): ExpEmbedding = when (booleanConstantDescriptor) {
        ConeContractConstantValues.TRUE -> BooleanLit(true)
        ConeContractConstantValues.FALSE -> BooleanLit(false)
        else -> throw IllegalArgumentException("Unexpected boolean constant: $booleanConstantDescriptor")
    }

    override fun visitReturnsEffectDeclaration(
        returnsEffect: KtReturnsEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
        data: ContractDescriptionVisitorContext,
    ): ExpEmbedding {
        val retVar = signature.returnVar
        return when (returnsEffect.value) {
            ConeContractConstantValues.WILDCARD -> BooleanLit(true)
            /* NOTE: in a function that has a non-nullable return type, the compiler will not complain if there is an effect like
             * returnsNotNull(). So it is necessary to take care of these cases in order to avoid comparison between non-nullable
             * values and null. In a function that has a non-nullable return type, returnsNotNull() is mapped to true and returns(null)
             * is mapped to false
             */
            ConeContractConstantValues.NULL -> retVar.nullCmp(false, data.effectSource)
            ConeContractConstantValues.NOT_NULL -> retVar.nullCmp(true, data.effectSource)
            ConeContractConstantValues.TRUE -> EqCmp(retVar, BooleanLit(true), data.effectSource)
            ConeContractConstantValues.FALSE -> EqCmp(retVar, BooleanLit(false), data.effectSource)
            else -> throw IllegalArgumentException("Unexpected constant: ${returnsEffect.value}")
        }
    }

    override fun visitValueParameterReference(
        valueParameterReference: KtValueParameterReference<ConeKotlinType, ConeDiagnostic>,
        data: ContractDescriptionVisitorContext,
    ): ExpEmbedding = valueParameterReference.embeddedVar()

    override fun visitIsNullPredicate(
        isNullPredicate: KtIsNullPredicate<ConeKotlinType, ConeDiagnostic>,
        data: ContractDescriptionVisitorContext,
    ): ExpEmbedding {
        /* NOTE: useless comparisons like x != null with x non-nullable will compile with just a warning.
         * So it is necessary to take care of these cases in order to avoid comparison between non-nullable
         * values and null. Let x be a non-nullable variable, then x == null is mapped to false and x != null is mapped to true
         */
        val param = isNullPredicate.arg.embeddedVar()
        return param.nullCmp(isNullPredicate.isNegated)
    }

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: KtBinaryLogicExpression<ConeKotlinType, ConeDiagnostic>,
        data: ContractDescriptionVisitorContext,
    ): ExpEmbedding {
        val left = binaryLogicExpression.left.accept(this, data)
        val right = binaryLogicExpression.right.accept(this, data)
        return when (binaryLogicExpression.kind) {
            LogicOperationKind.AND -> And(left, right)
            LogicOperationKind.OR -> Or(left, right)
        }
    }

    override fun visitLogicalNot(
        logicalNot: KtLogicalNot<ConeKotlinType, ConeDiagnostic>,
        data: ContractDescriptionVisitorContext,
    ): ExpEmbedding {
        val arg = logicalNot.arg.accept(this, data)
        return Not(arg)
    }

    override fun visitConditionalEffectDeclaration(
        conditionalEffect: KtConditionalEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
        data: ContractDescriptionVisitorContext,
    ): ExpEmbedding {
        val effect = conditionalEffect.effect.accept(this, data)
        val cond = conditionalEffect.condition.accept(this, data)
        return Implies(effect, cond, data.effectSource)
    }

    override fun visitCallsEffectDeclaration(
        callsEffect: KtCallsEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
        data: ContractDescriptionVisitorContext,
    ): ExpEmbedding {
        val param = callsEffect.valueParameterReference.accept(this, data)
        val callsFieldAccess = FieldAccess(param, SpecialFields.FunctionObjectCallCounterField)
        return when (callsEffect.kind) {
            // NOTE: case not supported for contracts
            EventOccurrencesRange.ZERO -> EqCmp(
                callsFieldAccess,
                Old(callsFieldAccess),
                data.effectSource
            )
            EventOccurrencesRange.AT_MOST_ONCE -> LeCmp(
                callsFieldAccess,
                Add(Old(callsFieldAccess), IntLit(1)),
                data.effectSource
            )
            EventOccurrencesRange.EXACTLY_ONCE -> EqCmp(
                callsFieldAccess,
                Add(Old(callsFieldAccess), IntLit(1)),
                data.effectSource
            )
            EventOccurrencesRange.AT_LEAST_ONCE -> GtCmp(
                callsFieldAccess,
                Old(callsFieldAccess),
                data.effectSource
            )
            // NOTE: case not supported for contracts
            EventOccurrencesRange.MORE_THAN_ONCE -> GtCmp(
                callsFieldAccess,
                Add(Old(callsFieldAccess), IntLit(1)),
                data.effectSource
            )
            EventOccurrencesRange.UNKNOWN -> BooleanLit(true, data.effectSource)
        }
    }

    override fun visitIsInstancePredicate(
        isInstancePredicate: KtIsInstancePredicate<ConeKotlinType, ConeDiagnostic>,
        data: ContractDescriptionVisitorContext,
    ): ExpEmbedding {
        val argVar = isInstancePredicate.arg.embeddedVar()
        val subtypeRel =
            IsSubtypeCall(TypeOfCall(argVar), ExpWrapper(ctx.embedType(isInstancePredicate.type).runtimeType, TypeInfoTypeEmbedding))
        return if (isInstancePredicate.isNegated) Not(subtypeRel) else subtypeRel
    }

    private fun KtValueParameterReference<ConeKotlinType, ConeDiagnostic>.embeddedVar(): VariableEmbedding =
        embeddedVarByIndex(parameterIndex)

    private fun embeddedVarByIndex(ix: Int): VariableEmbedding =
        if (ix == -1) signature.receiver!! else signature.params[ix]

    private fun VariableEmbedding.nullCmp(isNegated: Boolean, source: KtSourceElement? = null): ExpEmbedding =
        if (type is NullableTypeEmbedding) {
            if (isNegated) {
                NeCmp(this, type.nullVal(), source)
            } else {
                EqCmp(this, type.nullVal(), source)
            }
        } else {
            BooleanLit(isNegated, source)
        }
}


