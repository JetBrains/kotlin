/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.formver.PluginConfiguration
import org.jetbrains.kotlin.formver.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.domains.*
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Field
import org.jetbrains.kotlin.formver.viper.ast.Method
import org.jetbrains.kotlin.formver.viper.ast.Program

/**
 * Tracks the top-level information about the program.
 * Conversions for global entities like types should generally be
 * performed via this context to ensure they can be deduplicated.
 * We need the FirSession to get access to the TypeContext.
 */
class ProgramConverter(val session: FirSession, override val config: PluginConfiguration) : ProgramConversionContext {
    private val methods: MutableMap<MangledName, MethodEmbedding> = mutableMapOf()
    private val classes: MutableMap<ClassName, ClassTypeEmbedding> = mutableMapOf()
    private val fields: MutableList<Field> = mutableListOf()

    val program: Program
        get() = Program(
            domains = listOf(UnitDomain, NullableDomain, CastingDomain, TypeOfDomain, TypeDomain(classes.values.toList()), AnyDomain),
            fields = SpecialFields.all + fields,
            methods = SpecialMethods.all + methods.values.map { it.viperMethod }.toList(),
        )

    fun registerForVerification(declaration: FirSimpleFunction) {
        processFunction(declaration.symbol, declaration.body)
    }

    override fun embedFunction(symbol: FirFunctionSymbol<*>): MethodEmbedding {
        return processFunction(symbol, null)
    }

    private fun embedClass(symbol: FirRegularClassSymbol): ClassTypeEmbedding {
        val className = symbol.classId.embedName()
        return when (val existingEmbedding = classes[className]) {
            null -> {
                val newEmbedding = ClassTypeEmbedding(className)
                classes[className] = newEmbedding
                processClass(symbol)
                newEmbedding
            }
            else -> existingEmbedding
        }
    }

    override fun embedType(type: ConeKotlinType): TypeEmbedding = when {
        type.isUnit -> UnitTypeEmbedding
        type.isInt -> IntTypeEmbedding
        type.isBoolean -> BooleanTypeEmbedding
        type.isNothing -> NothingTypeEmbedding
        type.isSomeFunctionType(session) -> FunctionTypeEmbedding
        type.isNullable -> NullableTypeEmbedding(embedType(type.withNullability(ConeNullability.NOT_NULL, session.typeContext)))
        type.isAny -> AnyTypeEmbedding
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

    private fun embedSignature(symbol: FirFunctionSymbol<*>): MethodSignatureEmbedding {
        val retType = symbol.resolvedReturnTypeRef.type
        val params = symbol.valueParameterSymbols.map {
            VariableEmbedding(it.embedName(), embedType(it.resolvedReturnType))
        }
        val receiver = symbol.receiverType?.let { VariableEmbedding(ThisReceiverName, embedType(it)) }
        return object : MethodSignatureEmbedding {
            override val name = symbol.callableId.embedName()
            override val receiver = receiver
            override val params = params
            override val returnType = embedType(retType)
        }
    }

    private val FirFunctionSymbol<*>.receiverType: ConeKotlinType?
        get() = dispatchReceiverType ?: resolvedReceiverTypeRef?.type

    private fun processFunction(symbol: FirFunctionSymbol<*>, body: FirBlock?): MethodEmbedding {
        val signature = embedSignature(symbol)
        // NOTE: we have a problem here if we initially specify a method without a body,
        // and then later decide to add a body anyway.  It's not a problem for now, but
        // worth being aware of.
        return methods.getOrPut(signature.name) {
            val methodCtx = object : MethodConversionContext, ProgramConversionContext by this {
                override val signature: MethodSignatureEmbedding = signature

                override val preconditions =
                    signature.formalArgs.flatMap { it.invariants() } + signature.formalArgs.flatMap { it.accessInvariants() }

                private val contractPostconditions = symbol.resolvedContractDescription?.effects?.map {
                    it.effect.accept(ContractDescriptionConversionVisitor, this)
                } ?: emptyList()

                override val postconditions = signature.formalArgs.flatMap { it.accessInvariants() } +
                        signature.params.flatMap { it.dynamicInvariants() } +
                        signature.returnVar.invariants() +
                        contractPostconditions

                private var nextAnonVarNumber = 0
                override fun newAnonVar(type: TypeEmbedding): VariableEmbedding =
                    VariableEmbedding(AnonymousName(++nextAnonVarNumber), type)
            }

            val bodySeqn = body?.let {
                val ctx = StmtConverter(methodCtx, SeqnBuilder(), NoopResultTrackerFactory)
                ctx.convert(body)
                ctx.block
            }

            MethodEmbedding(signature, methodCtx.preconditions, methodCtx.postconditions, bodySeqn)
        }
    }

    private fun processClass(symbol: FirRegularClassSymbol) {
        val concreteFields = symbol.declarationSymbols
            .filterIsInstance<FirPropertySymbol>()
            .filter { it.hasBackingField }
            .map { Field(it.callableId.embedName(), embedType(it.resolvedReturnType).viperType) }
        fields += concreteFields
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
