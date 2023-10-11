/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
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
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

/**
 * Tracks the top-level information about the program.
 * Conversions for global entities like types should generally be
 * performed via this context to ensure they can be deduplicated.
 * We need the FirSession to get access to the TypeContext.
 */
class ProgramConverter(val session: FirSession, override val config: PluginConfiguration, override val errorCollector: ErrorCollector) :
    ProgramConversionContext {
    private val methods: MutableMap<MangledName, FunctionEmbedding> = SpecialKotlinFunctions.byName.toMutableMap()
    private val classes: MutableMap<MangledName, ClassTypeEmbedding> = mutableMapOf()
    private val properties: MutableMap<MangledName, PropertyEmbedding> = mutableMapOf()

    override val anonNameProducer = FreshEntityProducer { AnonymousName(it) }
    override val whileIndexProducer = FreshEntityProducer { it }
    override val returnLabelNameProducer = FreshEntityProducer { ReturnLabelName(it) }
    override val catchLabelNameProducer = FreshEntityProducer { CatchLabelName(it) }
    override val tryExitLabelNameProducer = FreshEntityProducer { TryExitLabelName(it) }

    val program: Program
        get() = Program(
            domains = listOf(UnitDomain, NullableDomain, CastingDomain, TypeOfDomain, TypeDomain(classes.values.toList()), AnyDomain),
            fields = SpecialFields.all + classes.values.flatMap { it.flatMapUniqueFields { _, field -> listOf(field.toViper()) } }
                .distinctBy { it.name },
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
                // The full class embedding is necessary to process the signatures of the properties of the class, since
                // these take the class as a parameter. We thus do this in three phases:
                // 1. Provide a class embedding in the `classes` map (necessary for embedType to not call this recursively).
                // 2. Initialise the supertypes (including running this whole four-step process on each)
                // 3. Initialise the fields
                // 4. Process the properties of the class.
                //
                // With respect to the embedding, each phase is pure by itself, and only updates the class embedding at the end.
                // This ensures the code never sees half-built supertype or field data. The phases can, however, modify the
                // `ProgramConverter`.

                // Phase 1
                val newEmbedding = ClassTypeEmbedding(className)
                classes[className] = newEmbedding

                // Phase 2
                newEmbedding.initSuperTypes(symbol.resolvedSuperTypes.map(::embedType))

                // Phase 3
                val properties = symbol.declarationSymbols.filterIsInstance<FirPropertySymbol>()
                newEmbedding.initFields(properties.mapNotNull { processBackingFields(it, newEmbedding) }.toMap())

                // Phase 4
                properties.forEach { processProperty(it, newEmbedding) }
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

    override fun embedProperty(symbol: FirPropertySymbol): PropertyEmbedding = if (symbol.isExtension) {
        PropertyEmbedding(
            symbol.getterSymbol?.let { embedGetter(it, null) },
            symbol.setterSymbol?.let { embedSetter(it, null) },
        )
    } else {
        // Ensure that the class has been processed.
        embedType(symbol.dispatchReceiverType!!)
        properties[symbol.callableId.embedMemberPropertyName()] ?: throw IllegalStateException("Unknown property ${symbol.callableId}")
    }

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
            override val sourceName: String?
                get() = super<NamedFunctionSignature>.sourceName
        }
        val contractVisitor = ContractDescriptionConversionVisitor(this@ProgramConverter, subSignature)

        return object : FullNamedFunctionSignature, NamedFunctionSignature by subSignature {
            override val preconditions = subSignature.formalArgs.flatMap { it.pureInvariants() } +
                    subSignature.formalArgs.flatMap { it.accessInvariants() } +
                    contractVisitor.getPreconditions(symbol) +
                    subSignature.stdLibPreConditions()

            override val postconditions = subSignature.formalArgs.flatMap { it.accessInvariants() } +
                    subSignature.params.flatMap { it.dynamicInvariants() } +
                    subSignature.returnVar.pureInvariants() +
                    subSignature.returnVar.provenInvariants() +
                    subSignature.returnVar.accessInvariants() +
                    contractVisitor.getPostconditions(symbol) +
                    subSignature.stdLibPostConditions()
        }
    }

    private val FirFunctionSymbol<*>.receiverType: ConeKotlinType?
        get() {
            val symbol = when (this) {
                is FirPropertyAccessorSymbol -> propertySymbol
                else -> this
            }
            return symbol.dispatchReceiverType ?: symbol.resolvedReceiverTypeRef?.type
        }

    /**
     * Construct and register the field embedding for this property's backing field, if any exists.
     */
    private fun processBackingFields(symbol: FirPropertySymbol, embedding: ClassTypeEmbedding): Pair<SimpleKotlinName, FieldEmbedding>? {
        val unscopedName = symbol.callableId.embedUnscopedPropertyName()
        // This field is already registered in the supertype: we don't need to know about it.
        if (embedding.findAncestorField(unscopedName) != null) return null
        val name = symbol.callableId.embedMemberPropertyName()
        val backingField = name.specialEmbedding() ?: symbol.hasBackingField.ifTrue {
            UserFieldEmbedding(
                name,
                embedType(symbol.resolvedReturnType),
                symbol.isVal
            )
        }
        return backingField?.let { unscopedName to it }
    }

    /**
     * Construct and register the property embedding (i.e. getter + setter) for this property.
     */
    private fun processProperty(symbol: FirPropertySymbol, embedding: ClassTypeEmbedding) {
        val unscopedName = symbol.callableId.embedUnscopedPropertyName()
        val name = symbol.callableId.embedMemberPropertyName()
        val backingField = embedding.findField(unscopedName)
        val getter: GetterEmbedding? = symbol.getterSymbol?.let { embedGetter(it, backingField) }
        val setter: SetterEmbedding? = symbol.setterSymbol?.let { embedSetter(it, backingField) }
        properties[name] = PropertyEmbedding(getter, setter)
    }

    @OptIn(SymbolInternals::class)
    private fun embedGetter(symbol: FirPropertyAccessorSymbol, backingField: FieldEmbedding?): GetterEmbedding =
        if (symbol.fir is FirDefaultPropertyGetter && backingField != null) {
            BackingFieldGetter(backingField)
        } else {
            CustomGetter(embedFunction(symbol))
        }

    @OptIn(SymbolInternals::class)
    private fun embedSetter(symbol: FirPropertyAccessorSymbol, backingField: FieldEmbedding?): SetterEmbedding =
        if (symbol.fir is FirDefaultPropertySetter && backingField != null) {
            BackingFieldSetter(backingField)
        } else {
            CustomSetter(embedFunction(symbol))
        }

    private fun processCallable(symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature): CallableEmbedding =
        if (symbol.isInline) {
            InlineNamedFunction(signature, symbol)
        } else {
            NonInlineNamedFunction(signature)
        }

    private fun convertMethodWithBody(declaration: FirSimpleFunction, signature: FullNamedFunctionSignature): Method {
        val body = declaration.body?.let {
            val methodCtx =
                MethodConverter(
                    this,
                    signature,
                    RootParameterResolver(this, returnLabelNameProducer.getFresh()),
                    0,
                    returnPointName = signature.sourceName
                )
            val stmtCtx = StmtConverter(methodCtx, SeqnBuilder(), NoopResultTrackerFactory)
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
