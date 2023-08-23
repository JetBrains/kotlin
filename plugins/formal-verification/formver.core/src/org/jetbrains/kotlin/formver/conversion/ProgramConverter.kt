/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.formver.domains.NullableDomain
import org.jetbrains.kotlin.formver.domains.UnitDomain
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Method
import org.jetbrains.kotlin.formver.viper.ast.Program

/**
 * Tracks the top-level information about the program.
 * Conversions for global entities like types should generally be
 * performed via this context to ensure they can be deduplicated.
 * We need the FirSession to get access to the TypeContext.
 */
class ProgramConverter(val session: FirSession) : ProgramConversionContext {
    private val methods: MutableMap<MangledName, Method> = mutableMapOf()

    val program: Program
        get() = Program(
            listOf(UnitDomain, NullableDomain), /* Domains */
            SpecialFields.all, /* Fields */
            SpecialMethods.all + methods.values.toList(), /* Methods */
        )

    fun addWithBody(declaration: FirSimpleFunction) {
        processFunction(declaration.symbol, declaration.body)
    }

    override fun add(symbol: FirNamedFunctionSymbol): MethodSignatureEmbedding {
        return processFunction(symbol, null)
    }

    override fun embedType(type: ConeKotlinType): TypeEmbedding = when {
        type.isUnit -> UnitTypeEmbedding
        type.isInt -> IntTypeEmbedding
        type.isBoolean -> BooleanTypeEmbedding
        type.isNothing -> NothingTypeEmbedding
        type.isSomeFunctionType(session) -> FunctionTypeEmbedding
        type.isNullable -> NullableTypeEmbedding(embedType(type.withNullability(ConeNullability.NOT_NULL, session.typeContext)))
        else -> throw NotImplementedError("The embedding for type $type is not yet implemented.")
    }

    private fun embedSignature(symbol: FirNamedFunctionSymbol): MethodSignatureEmbedding {
        val retType = symbol.resolvedReturnTypeRef.type
        val params = symbol.valueParameterSymbols.map {
            VariableEmbedding(it.embedName(), embedType(it.resolvedReturnType))
        }
        return MethodSignatureEmbedding(
            symbol.callableId.embedName(),
            params,
            embedType(retType)
        )
    }

    private fun processFunction(symbol: FirNamedFunctionSymbol, body: FirBlock?): MethodSignatureEmbedding {
        val signature = embedSignature(symbol)
        // NOTE: we have a problem here if we initially specify a method without a body,
        // and then later decide to add a body anyway.  It's not a problem for now, but
        // worth being aware of.
        methods.getOrPut(signature.name) {
            val methodCtx = object : MethodConversionContext, ProgramConversionContext by this {
                override val signature: MethodSignatureEmbedding = signature

                private var nextAnonVarNumber = 0
                override fun newAnonVar(type: TypeEmbedding): VariableEmbedding =
                    VariableEmbedding(AnonymousName(++nextAnonVarNumber), type)
            }

            val seqn = body?.let {
                val ctx = StmtConverter(methodCtx)
                ctx.convertAndAppend(body)
                ctx.block
            }

            val postconditions = symbol.resolvedContractDescription?.effects?.map {
                it.effect.accept(ContractDescriptionConversionVisitor(), methodCtx)
            } ?: emptyList()

            signature.toMethod(listOf(), postconditions, seqn)
        }
        return signature
    }
}
