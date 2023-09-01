/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.formver.PluginConfiguration
import org.jetbrains.kotlin.formver.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Method
import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.formver.viper.domains.CastingDomain
import org.jetbrains.kotlin.formver.viper.domains.NullableDomain
import org.jetbrains.kotlin.formver.viper.domains.UnitDomain

/**
 * Tracks the top-level information about the program.
 * Conversions for global entities like types should generally be
 * performed via this context to ensure they can be deduplicated.
 * We need the FirSession to get access to the TypeContext.
 */
class ProgramConverter(val session: FirSession, override val config: PluginConfiguration) : ProgramConversionContext {
    private val methods: MutableMap<MangledName, Method> = mutableMapOf()
    private val classes: MutableMap<ClassName, ClassEmbedding> = mutableMapOf()

    val program: Program
        get() = Program(
            domains = listOf(UnitDomain, NullableDomain, CastingDomain),
            fields = SpecialFields.all + classes.values.flatMap { it.fields }.map { it.toField() },
            methods = SpecialMethods.all + methods.values.toList(),
        )

    fun registerForVerification(declaration: FirSimpleFunction) {
        processFunction(declaration.symbol, declaration.body)
    }

    override fun embedFunction(symbol: FirFunctionSymbol<*>): MethodSignatureEmbedding {
        return processFunction(symbol, null)
    }

    override fun embedClass(symbol: FirRegularClassSymbol): ClassEmbedding {

        val className = ClassName(symbol.classId.packageFqName, symbol.classId.shortClassName)
        // If the class name is not contained in the classes hashmap, then add a new embedding.
        return classes.getOrPut(className) {
            // Get classes fields
            val concreteFields = symbol.declarationSymbols
                .filterIsInstance<FirPropertySymbol>()
                .filter { it.hasBackingField }
                .map { VariableEmbedding(it.callableId.embedName(), embedType(it.resolvedReturnType)) }

            ClassEmbedding(className, concreteFields)
        }
    }

    override fun embedType(type: ConeKotlinType): TypeEmbedding = when {
        type.isUnit -> UnitTypeEmbedding
        type.isInt -> IntTypeEmbedding
        type.isBoolean -> BooleanTypeEmbedding
        type.isNothing -> NothingTypeEmbedding
        type.isSomeFunctionType(session) -> FunctionTypeEmbedding
        type.isNullable -> NullableTypeEmbedding(embedType(type.withNullability(ConeNullability.NOT_NULL, session.typeContext)))
        type is ConeClassLikeType -> {
            val classLikeSymbol = type.toClassSymbol(session)
            if (classLikeSymbol is FirRegularClassSymbol) {
                embedClass(classLikeSymbol)
            } else {
                unimplementedTypeEmbedding(type)
            }
        }
        else -> unimplementedTypeEmbedding(type)
    }

    private fun <D : FirFunction> embedSignature(symbol: FirFunctionSymbol<D>): MethodSignatureEmbedding {
        val retType = symbol.resolvedReturnTypeRef.type
        val params = symbol.valueParameterSymbols.map {
            VariableEmbedding(it.embedName(), embedType(it.resolvedReturnType))
        }
        val receiver = symbol.dispatchReceiverType?.let { VariableEmbedding(ThisReceiverName, embedType(it)) }
        return MethodSignatureEmbedding(
            symbol.callableId.embedName(),
            receiver,
            params,
            embedType(retType)
        )
    }

    private fun <D : FirFunction> processFunction(symbol: FirFunctionSymbol<D>, body: FirBlock?): MethodSignatureEmbedding {
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
                val ctx = StmtConverter(methodCtx, SeqnBuilder())
                ctx.convert(body)
                ctx.block
            }

            val postconditions = symbol.resolvedContractDescription?.effects?.map {
                it.effect.accept(ContractDescriptionConversionVisitor, methodCtx)
            } ?: emptyList()

            signature.toMethod(listOf(), postconditions, seqn)
        }
        return signature
    }

    private fun unimplementedTypeEmbedding(type: ConeKotlinType): TypeEmbedding =
        when (config.behaviour) {
            UnsupportedFeatureBehaviour.THROW_EXCEPTION ->
                throw NotImplementedError("The embedding for type $type is not yet implemented.")
            UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
                System.err.println("Requested type $type, for which we do not yet have an embedding.")
                UnitTypeEmbedding
            }
        }
}
