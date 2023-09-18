/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.formver.ErrorCollector
import org.jetbrains.kotlin.formver.PluginConfiguration
import org.jetbrains.kotlin.formver.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.domains.*
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.callables.*
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Method
import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse

/**
 * Tracks the top-level information about the program.
 * Conversions for global entities like types should generally be
 * performed via this context to ensure they can be deduplicated.
 * We need the FirSession to get access to the TypeContext.
 */
class ProgramConverter(val session: FirSession, override val config: PluginConfiguration, override val errorCollector: ErrorCollector) :
    ProgramConversionContext {
    private val methods: MutableMap<MangledName, FunctionEmbedding> = mutableMapOf()
    private val classes: MutableMap<MangledName, ClassTypeEmbedding> = mutableMapOf()
    private val fields: MutableMap<MangledName, FieldEmbedding> = mutableMapOf()

    val program: Program
        get() = Program(
            domains = listOf(UnitDomain, NullableDomain, CastingDomain, TypeOfDomain, TypeDomain(classes.values.toList()), AnyDomain),
            fields = SpecialFields.all + fields.values.map { it.toViper() },
            functions = SpecialFunctions.all,
            methods = SpecialMethods.all + methods.values.mapNotNull { it.viperMethod }.toList(),
        )

    fun registerForVerification(declaration: FirSimpleFunction) {
        val signature = embedFullSignature(declaration.symbol)
        // Note: it is important that `viperMethod` is only set later, as we need to
        // place the embedding in the map before processing the body.
        val embedding = embedUserFunction(declaration.symbol, signature)
        embedding.viperMethod = convertMethodWithBody(declaration, signature)
    }

    fun embedUserFunction(symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature): UserFunctionEmbedding {
        (methods[signature.name] as? UserFunctionEmbedding)?.also { return it }
        val new = UserFunctionEmbedding(processCallable(symbol, signature))
        methods[signature.name] = new
        return new
    }

    override fun embedFunction(symbol: FirFunctionSymbol<*>): FunctionEmbedding =
        methods.getOrPut(symbol.embedName(this)) {
            val signature = embedFullSignature(symbol)
            val embedding = UserFunctionEmbedding(processCallable(symbol, signature))
            embedding.viperMethod = convertMethodWithoutBody(symbol, signature)
            embedding
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
        type is ConeErrorType -> throw IllegalArgumentException("Encountered an erroneous type: $type")
        type is ConeTypeParameterType -> NullableTypeEmbedding(AnyTypeEmbedding)
        type.isUnit -> UnitTypeEmbedding
        type.isInt -> IntTypeEmbedding
        type.isBoolean -> BooleanTypeEmbedding
        type.isNothing -> NothingTypeEmbedding
        type.isSomeFunctionType(session) -> {
            val receiverType: TypeEmbedding? = type.receiverType(session)?.let { embedType(it) }
            val paramTypes: List<TypeEmbedding> = type.valueParameterTypesWithoutReceivers(session).map(::embedType)
            val returnType: TypeEmbedding = embedType(type.returnType(session))
            val signature = CallableSignatureData(receiverType, paramTypes, returnType)
            FunctionTypeEmbedding(signature)
        }
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

    // Note: keep in mind that this function is necessary to resolve the name of the function!
    override fun embedType(symbol: FirFunctionSymbol<*>): TypeEmbedding = FunctionTypeEmbedding(embedFunctionSignature(symbol).asData)

    override fun getField(field: FirPropertySymbol): FieldEmbedding? = fields[field.callableId.embedMemberPropertyName()]

    private var nextAnonVarNumber = 0
    override fun newAnonName(): AnonymousName = AnonymousName(++nextAnonVarNumber)

    private var nextWhileIndex = 0
    override fun newWhileIndex() = ++nextWhileIndex

    override fun embedFunctionSignature(symbol: FirFunctionSymbol<*>): FunctionSignature {
        val retType = symbol.resolvedReturnTypeRef.type
        val receiverType = symbol.receiverType
        return object : FunctionSignature {
            override val receiver = receiverType?.let { VariableEmbedding(ThisReceiverName, embedType(it)) }
            override val params = symbol.valueParameterSymbols.map {
                VariableEmbedding(it.embedName(), embedType(it.resolvedReturnType))
            }
            override val returnType = embedType(retType)
        }
    }

    private fun embedFullSignature(symbol: FirFunctionSymbol<*>): FullNamedFunctionSignature {
        val subSignature = object : NamedFunctionSignature, FunctionSignature by embedFunctionSignature(symbol) {
            override val name = symbol.embedName(this@ProgramConverter)
        }
        val contractVisitor = ContractDescriptionConversionVisitor(this@ProgramConverter, subSignature)

        return object : FullNamedFunctionSignature, NamedFunctionSignature by subSignature {
            override val preconditions = subSignature.formalArgs.flatMap { it.invariants() } +
                    subSignature.formalArgs.flatMap { it.accessInvariants() } +
                    contractVisitor.getPreconditions(symbol)

            override val postconditions = subSignature.formalArgs.flatMap { it.accessInvariants() } +
                    subSignature.params.flatMap { it.dynamicInvariants() } +
                    subSignature.returnVar.invariants() +
                    subSignature.returnVar.provenInvariants() +
                    contractVisitor.getPostconditions(symbol)
        }
    }

    private val FirFunctionSymbol<*>.receiverType: ConeKotlinType?
        get() = when (this) {
            is FirPropertyAccessorSymbol -> propertySymbol.dispatchReceiverType
            else -> dispatchReceiverType ?: resolvedReceiverTypeRef?.type
        }

    private fun processClass(symbol: FirRegularClassSymbol) {
        symbol.declarationSymbols
            .filterIsInstance<FirPropertySymbol>()
            .filter { it.hasBackingField }
            .forEach {
                val fieldName = it.callableId.embedMemberPropertyName()
                fields[fieldName] = FieldEmbedding(it.callableId.embedMemberPropertyName(), embedType(it.resolvedReturnType))
            }
    }

    private fun processCallable(symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature): CallableEmbedding =
        if (symbol.isInline) {
            InlineNamedFunction(signature, symbol)
        } else {
            NonInlineNamedFunction(signature)
        }

    private fun convertMethodWithBody(declaration: FirSimpleFunction, signature: FullNamedFunctionSignature): Method {
        val body = declaration.body?.let {
            val methodCtx = object : MethodConversionContext, ProgramConversionContext by this {
                override val signature: FullNamedFunctionSignature = signature
                override val nameMangler = NameMangler()
                override fun getLambdaOrNull(name: Name): SubstitutionLambda? = null
            }

            val stmtCtx = StmtConverter(methodCtx, SeqnBuilder(), NoopResultTrackerFactory, scopeDepth = 0)
            signature.formalArgs.forEach { arg ->
                // Ideally we would want to assume these rather than inhale them to prevent inconsistencies with permissions.
                // Unfortunately Silicon for some reason does not allow Assumes. However, it doesn't matter as long as the
                // provenInvariants don't contain permissions.
                arg.provenInvariants().forEach { invariant ->
                    stmtCtx.addStatement(Stmt.Inhale(invariant))
                }
            }
            stmtCtx.addDeclaration(methodCtx.returnLabel.toDecl())
            stmtCtx.convert(it)
            stmtCtx.addStatement(methodCtx.returnLabel.toStmt())
            stmtCtx.block
        }

        return signature.toViperMethod(body)
    }

    private fun convertMethodWithoutBody(symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature): Method? =
        symbol.isInline.ifFalse {
            signature.toViperMethod(null)
        }

    private fun unimplementedTypeEmbedding(type: ConeKotlinType): TypeEmbedding =
        when (config.behaviour) {
            UnsupportedFeatureBehaviour.THROW_EXCEPTION ->
                throw NotImplementedError("The embedding for type $type is not yet implemented.")
            UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
                errorCollector.addMinorError("Requested type $type, for which we do not yet have an embedding.")
                UnitTypeEmbedding
            }
        }
}
