/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.contracts.description.KtCallsEffectDeclaration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isInline
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
import org.jetbrains.kotlin.formver.viper.ast.*

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
            functions = SpecialFunctions.all,
            methods = SpecialMethods.all + methods.values.filter { it.shouldIncludeInProgram }.map { it.viperMethod }.toList(),
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
                val superTypes = symbol.resolvedSuperTypes.map(::embedType)
                val newEmbedding = ClassTypeEmbedding(className, superTypes)
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

    private var nextAnonVarNumber = 0
    override fun newAnonName(): AnonymousName = AnonymousName(++nextAnonVarNumber)

    private var nextWhileIndex = 0
    override fun newWhileIndex() = ++nextWhileIndex

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
            val contractVisitor = ContractDescriptionConversionVisitor(this@ProgramConverter, signature)
            val parameterIndices = (signature.params.indices.toSet() + setOfNotNull(signature.receiver?.let { -1 })).toMutableSet()
            val nonDuplicableIndices = symbol.resolvedContractDescription?.effects?.mapNotNull { decl ->
                (decl.effect as? KtCallsEffectDeclaration<*, *>)?.valueParameterReference?.parameterIndex
            }?.toSet() ?: emptySet()

            val duplicableParameters =
                (parameterIndices - nonDuplicableIndices).map { contractVisitor.embeddedVarByIndex(it) }.filter { it.type is FunctionTypeEmbedding }
                    .map { DuplicableFunction.toFuncApp(listOf(it.toLocalVar())) }
            val contractPostconditions =
                symbol.resolvedContractDescription?.effects?.map {
                    it.effect.accept(contractVisitor, Unit)
                } ?: emptyList()

            val methodCtx = object : MethodConversionContext, ProgramConversionContext by this {
                override val signature: MethodSignatureEmbedding = signature

                // It seems like Viper will propagate the weakest precondition through the label correctly even in the absence of
                // explicit invariants; we only need to add those if we want to make a stronger claim.
                override val returnLabel: Label = Label(ReturnLabelName, listOf())
                override val returnVar: VariableEmbedding = VariableEmbedding(ReturnVariableName, signature.returnType)

                override val preconditions =
                    signature.formalArgs.flatMap { it.invariants() } + signature.formalArgs.flatMap { it.accessInvariants() } + duplicableParameters

                override val postconditions = signature.formalArgs.flatMap { it.accessInvariants() } +
                        signature.params.flatMap { it.dynamicInvariants() } +
                        signature.returnVar.invariants() +
                        signature.returnVar.provenInvariants() +
                        contractPostconditions

                override fun resolveName(name: MangledName): MangledName = name
            }

            val bodySeqn = body?.let {
                val ctx = StmtConverter(methodCtx, SeqnBuilder(), NoopResultTrackerFactory)
                signature.formalArgs.forEach { arg ->
                    arg.provenInvariants().forEach {
                        // Ideally we would want to assume these rather than inhale them to prevent inconsistencies with permissions.
                        // Unfortunately Silicon for some reason does not allow Assumes. However, it doesn't matter as long as the
                        // provenInvariants don't contain permissions.
                        ctx.addStatement(Stmt.Inhale(it))
                    }
                }
                ctx.addDeclaration(methodCtx.returnLabel.toDecl())
                ctx.convert(body)
                ctx.addStatement(methodCtx.returnLabel.toStmt())
                ctx.block
            }

            MethodEmbedding(signature, methodCtx.preconditions, methodCtx.postconditions, bodySeqn, symbol.isInline)
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
                config.addMinorError("Requested type $type, for which we do not yet have an embedding.")
                UnitTypeEmbedding
            }
        }
}
