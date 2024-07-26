/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.formver.*
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.callables.*
import org.jetbrains.kotlin.formver.embeddings.expression.*
import org.jetbrains.kotlin.formver.linearization.Linearizer
import org.jetbrains.kotlin.formver.linearization.SeqnBuilder
import org.jetbrains.kotlin.formver.linearization.SharedLinearizationState
import org.jetbrains.kotlin.formver.names.*
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.formver.viper.mangled
import org.jetbrains.kotlin.utils.addIfNotNull
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
    private val fields: MutableSet<FieldEmbedding> = mutableSetOf()

    // Cast is valid since we check that values are not null. We specify the type for `filterValues` explicitly to ensure there's no
    // loss of type information earlier.
    @Suppress("UNCHECKED_CAST")
    val debugExpEmbeddings: Map<MangledName, ExpEmbedding>
        get() = methods
            .mapValues { (it.value as? UserFunctionEmbedding)?.body?.debugExpEmbedding }
            .filterValues { value: ExpEmbedding? -> value != null } as Map<MangledName, ExpEmbedding>

    override val whileIndexProducer = indexProducer()
    override val catchLabelNameProducer = simpleFreshEntityProducer { CatchLabelName(it) }
    override val tryExitLabelNameProducer = simpleFreshEntityProducer { TryExitLabelName(it) }
    override val scopeIndexProducer = indexProducer()

    // The type annotation is necessary for the code to compile.
    override val anonVarProducer = FreshEntityProducer { n, type: TypeEmbedding -> AnonymousVariableEmbedding(n, type) }
    override val returnTargetProducer = FreshEntityProducer { n, type: TypeEmbedding -> ReturnTarget(n, type) }

    val program: Program
        get() = Program(
            domains = listOf(RuntimeTypeDomain(classes.values.toList())),
            // We need to deduplicate fields since public fields with the same name are represented differently
            // at `FieldEmbedding` level but map to the same Viper.
            fields = SpecialFields.all.map { it.toViper() } +
                    fields.distinctBy { it.name.mangled }.map { it.toViper() },
            functions = SpecialFunctions.all,
            methods = SpecialMethods.all + methods.values.mapNotNull { it.viperMethod }.distinctBy { it.name.mangled },
            predicates = classes.values.flatMap { listOf(it.details.sharedPredicate, it.details.uniquePredicate) }
        )

    fun registerForVerification(declaration: FirSimpleFunction) {
        val signature = embedFullSignature(declaration.symbol)
        // Note: it is important that `body` is only set after `embedUserFunction` is complete, as we need to
        // place the embedding in the map before processing the body.
        embedUserFunction(declaration.symbol, signature).apply {
            body = convertMethodWithBody(declaration, signature)
        }
    }

    fun embedUserFunction(symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature): UserFunctionEmbedding {
        (methods[signature.name] as? UserFunctionEmbedding)?.also { return it }
        val new = UserFunctionEmbedding(processCallable(symbol, signature))
        methods[signature.name] = new
        return new
    }

    private fun embedGetterFunction(symbol: FirPropertySymbol): FunctionEmbedding {
        val name = symbol.embedGetterName(this)
        return methods.getOrPut(name) {
            val signature = GetterFunctionSignature(name, symbol)
            UserFunctionEmbedding(
                NonInlineNamedFunction(signature)
            )
        }
    }

    private fun embedSetterFunction(symbol: FirPropertySymbol): FunctionEmbedding {
        val name = symbol.embedSetterName(this)
        return methods.getOrPut(name) {
            val signature = SetterFunctionSignature(name, symbol)
            UserFunctionEmbedding(
                NonInlineNamedFunction(signature)
            )
        }
    }

    override fun embedFunction(symbol: FirFunctionSymbol<*>): FunctionEmbedding =
        methods.getOrPut(symbol.embedName(this)) {
            val signature = embedFullSignature(symbol)
            val callable = processCallable(symbol, signature)
            UserFunctionEmbedding(callable)
        }

    /**
     * Returns an embedding of the class type, with details set.
     */
    private fun embedClass(symbol: FirRegularClassSymbol): ClassTypeEmbedding {
        val className = symbol.classId.embedName()
        val embedding = classes.getOrPut(className) {
            buildType {
                klass { withName(className) }
            } as ClassTypeEmbedding
        }
        if (embedding.hasDetails) return embedding

        val newDetails = ClassEmbeddingDetails(embedding, symbol.classKind == ClassKind.INTERFACE)
        embedding.initDetails(newDetails)

        // The full class embedding is necessary to process the signatures of the properties of the class, since
        // these take the class as a parameter. We thus do this in three phases:
        // 1. Initialise the supertypes (including running this whole four-step process on each)
        // 2. Initialise the fields
        // 3. Process the properties of the class.
        //
        // With respect to the embedding, each phase is pure by itself, and only updates the class embedding at the end.
        // This ensures the code never sees half-built supertype or field data. The phases can, however, modify the
        // `ProgramConverter`.

        // Phase 1
        newDetails.initSuperTypes(symbol.resolvedSuperTypes.map(::embedType))

        // Phase 2
        val properties = symbol.propertySymbols
        newDetails.initFields(properties.mapNotNull { processBackingField(it, symbol) }.toMap())

        // Phase 3
        properties.forEach { processProperty(it, newDetails) }

        return embedding
    }

    override fun embedType(type: ConeKotlinType): TypeEmbedding = when {
        type is ConeErrorType -> error("Encountered an erroneous type: $type")
        type is ConeTypeParameterType -> buildType { isNullable = true; any() }
        type.isUnit -> buildType { unit() }
        type.isInt -> buildType { int() }
        type.isBoolean -> buildType { boolean() }
        type.isNothing -> buildType { nothing() }
        type.isSomeFunctionType(session) -> {
            val receiverType: TypeEmbedding? = type.receiverType(session)?.let { embedType(it) }
            val paramTypes: List<TypeEmbedding> = type.valueParameterTypesWithoutReceivers(session).map(::embedType)
            val returnType: TypeEmbedding = embedType(type.returnType(session))
            val signature = CallableSignatureData(receiverType, paramTypes, returnType)
            FunctionTypeEmbedding(signature)
        }
        type.isNullable -> NullableTypeEmbedding(embedType(type.withNullability(ConeNullability.NOT_NULL, session.typeContext)))
        type.isAny -> buildType { any() }
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
        embedCustomProperty(symbol)
    } else {
        // Ensure that the class has been processed.
        embedType(symbol.dispatchReceiverType!!)
        properties.getOrPut(symbol.embedMemberPropertyName()) {
            check(symbol is FirIntersectionOverridePropertySymbol) {
                "Unknown property ${symbol.callableId}."
            }
            embedCustomProperty(symbol)
        }
    }

    private fun <R> FirPropertySymbol.withConstructorParam(action: FirPropertySymbol.(FirValueParameterSymbol) -> R): R? =
        correspondingValueParameterFromPrimaryConstructor?.let { param ->
            action(param)
        }

    private fun extractConstructorParamsAsFields(symbol: FirFunctionSymbol<*>): Map<FirValueParameterSymbol, FieldEmbedding> {
        if (symbol !is FirConstructorSymbol || !symbol.isPrimary)
            return emptyMap()
        val constructedClassSymbol = symbol.resolvedReturnType.toRegularClassSymbol(session) ?: return emptyMap()
        val constructedClass = embedClass(constructedClassSymbol)

        return constructedClassSymbol.propertySymbols.mapNotNull { propertySymbol ->
            propertySymbol.withConstructorParam { paramSymbol ->
                constructedClass.details.findField(callableId.embedUnscopedPropertyName())?.let { paramSymbol to it }
            }
        }.toMap()
    }

    override fun embedFunctionSignature(symbol: FirFunctionSymbol<*>): FunctionSignature {
        val retType = embedType(symbol.resolvedReturnTypeRef.type)
        val receiverType = symbol.receiverType
        val isReceiverUnique = symbol.receiverParameter?.isUnique(session) ?: false
        val isReceiverBorrowed = symbol.receiverParameter?.isBorrowed(session) ?: false
        return object : FunctionSignature {
            // TODO: figure out whether we want a symbol here and how to get it.
            override val receiver =
                receiverType?.let { PlaceholderVariableEmbedding(ThisReceiverName, embedType(it), isReceiverUnique, isReceiverBorrowed) }
            override val params = symbol.valueParameterSymbols.map {
                FirVariableEmbedding(it.embedName(), embedType(it.resolvedReturnType), it, it.isUnique(session), it.isBorrowed(session))
            }
            override val returnType = retType
            override val returnsUnique = symbol.isUnique(session) || symbol is FirConstructorSymbol
        }
    }

    private val FirRegularClassSymbol.propertySymbols: List<FirPropertySymbol>
        get() = this.declarationSymbols.filterIsInstance<FirPropertySymbol>()

    private fun embedFullSignature(symbol: FirFunctionSymbol<*>): FullNamedFunctionSignature {
        val subSignature = object : NamedFunctionSignature, FunctionSignature by embedFunctionSignature(symbol) {
            override val name = symbol.embedName(this@ProgramConverter)
            override val sourceName: String?
                get() = super<NamedFunctionSignature>.sourceName
        }
        val constructorParamSymbolsToFields = extractConstructorParamsAsFields(symbol)
        val contractVisitor = ContractDescriptionConversionVisitor(this@ProgramConverter, subSignature)

        return object : FullNamedFunctionSignature, NamedFunctionSignature by subSignature {
            // TODO (inhale vs require) Decide if `predicateAccessInvariant` should be required rather than inhaled in the beginning of the body.
            override fun getPreconditions(returnVariable: VariableEmbedding) = buildList {
                subSignature.formalArgs.forEach {
                    addAll(it.pureInvariants())
                    addAll(it.accessInvariants())
                    if (it.isUnique) {
                        addIfNotNull(it.type.uniquePredicateAccessInvariant()?.fillHole(it))
                    }
                }
                addAll(subSignature.stdLibPreconditions())
            }

            override fun getPostconditions(returnVariable: VariableEmbedding) = buildList {
                subSignature.formalArgs.forEach {
                    addAll(it.accessInvariants())
                    if (it.isUnique && it.isBorrowed) {
                        addIfNotNull(it.type.uniquePredicateAccessInvariant()?.fillHole(it))
                    }
                }
                addAll(returnVariable.pureInvariants())
                addAll(returnVariable.provenInvariants())
                addAll(returnVariable.allAccessInvariants())
                if (subSignature.returnsUnique) {
                    addIfNotNull(returnVariable.uniquePredicateAccessInvariant())
                }
                addAll(contractVisitor.getPostconditions(ContractVisitorContext(returnVariable, symbol)))
                addAll(subSignature.stdLibPostconditions(returnVariable))
                addIfNotNull(primaryConstructorInvariants(returnVariable))
            }

            fun primaryConstructorInvariants(returnVariable: VariableEmbedding): ExpEmbedding? {
                val invariants = params.mapNotNull { param ->
                    constructorParamSymbolsToFields[param.symbol]?.let { field ->
                        (field.accessPolicy == AccessPolicy.ALWAYS_READABLE).ifTrue {
                            EqCmp(PrimitiveFieldAccess(returnVariable, field), param)
                        }
                    }
                }
                return if (invariants.isEmpty()) null
                else UnfoldingClassPredicateEmbedding(returnVariable, invariants.toConjunction())
            }

            override val declarationSource: KtSourceElement? = symbol.source
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
    private fun processBackingField(
        symbol: FirPropertySymbol,
        classSymbol: FirRegularClassSymbol,
    ): Pair<SimpleKotlinName, FieldEmbedding>? {
        val embedding = embedClass(classSymbol)
        val unscopedName = symbol.callableId.embedUnscopedPropertyName()
        val scopedName = symbol.callableId.embedMemberBackingFieldName(
            Visibilities.isPrivate(symbol.visibility)
        )
        val fieldIsAllowed = symbol.hasBackingField
                && !symbol.isCustom
                && (symbol.isFinal || classSymbol.isFinal)
        val backingField = scopedName.specialEmbedding(embedding) ?: fieldIsAllowed.ifTrue {
            UserFieldEmbedding(
                scopedName,
                embedType(symbol.resolvedReturnType),
                symbol,
                symbol.isUnique(session),
                embedding,
            )
        }
        return backingField?.let { unscopedName to it }
    }

    /**
     * Construct and register the property embedding (i.e. getter + setter) for this property.
     *
     * Note that the property either has associated Viper field (and then it is used to access the value) or not (in this case methods are used).
     * The field is only used for final properties with default getter and default setter (if any).
     */
    private fun processProperty(symbol: FirPropertySymbol, embedding: ClassEmbeddingDetails) {
        val unscopedName = symbol.callableId.embedUnscopedPropertyName()
        val backingField = embedding.findField(unscopedName)
        backingField?.let { fields.add(it) }
        properties[symbol.embedMemberPropertyName()] = embedProperty(symbol, backingField)
    }

    private fun embedCustomProperty(symbol: FirPropertySymbol) = embedProperty(symbol, null)

    private fun embedProperty(symbol: FirPropertySymbol, backingField: FieldEmbedding?) =
        PropertyEmbedding(embedGetter(symbol, backingField),
                          symbol.isVar.ifTrue { embedSetter(symbol, backingField) })

    private fun embedGetter(symbol: FirPropertySymbol, backingField: FieldEmbedding?): GetterEmbedding =
        if (backingField != null) {
            BackingFieldGetter(backingField)
        } else {
            CustomGetter(embedGetterFunction(symbol))
        }

    private fun embedSetter(symbol: FirPropertySymbol, backingField: FieldEmbedding?): SetterEmbedding =
        if (backingField != null) {
            BackingFieldSetter(backingField)
        } else {
            CustomSetter(embedSetterFunction(symbol))
        }

    @OptIn(SymbolInternals::class)
    private fun processCallable(symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature): RichCallableEmbedding {
        val body = symbol.fir.body
        return if (symbol.isInline && body != null) {
            InlineNamedFunction(signature, symbol, body)
        } else {
            // We generate a dummy method header here to ensure all required types are processed already. If we skip this, any types
            // that are used only in contracts cause an error because they are not processed until too late.
            // TODO: fit this into the flow in some logical way instead.
            NonInlineNamedFunction(signature).also { it.toViperMethodHeader() }
        }
    }

    private fun convertMethodWithBody(declaration: FirSimpleFunction, signature: FullNamedFunctionSignature): FunctionBodyEmbedding? {
        val firBody = declaration.body ?: return null
        val returnTarget = returnTargetProducer.getFresh(signature.returnType)
        val methodCtx =
            MethodConverter(
                this,
                signature,
                RootParameterResolver(this, signature, signature.sourceName, returnTarget),
                scopeDepth = scopeIndexProducer.getFresh(),
            )
        val stmtCtx = StmtConverter(methodCtx)
        val body = stmtCtx.convert(firBody)

        // In the end we ensure that returned value is of some type even if that type is Unit.
        // However, for Unit we don't assign the result to any value.
        // One of the simplest solutions is to do is directly in the beginning of the body.
        val unitExtendedBody: ExpEmbedding =
            if (signature.returnType != UnitTypeEmbedding) body
            else Block(Assign(stmtCtx.defaultResolvedReturnTarget.variable, UnitLit), body)
        val bodyExp = FunctionExp(signature, unitExtendedBody, returnTarget.label)
        val seqnBuilder = SeqnBuilder(declaration.source)
        val linearizer = Linearizer(SharedLinearizationState(anonVarProducer), seqnBuilder, declaration.source)
        bodyExp.toViperUnusedResult(linearizer)
        return FunctionBodyEmbedding(seqnBuilder.block, returnTarget, bodyExp)
    }

    private fun unimplementedTypeEmbedding(type: ConeKotlinType): TypeEmbedding =
        when (config.behaviour) {
            UnsupportedFeatureBehaviour.THROW_EXCEPTION ->
                throw NotImplementedError("The embedding for type $type is not yet implemented.")
            UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
                errorCollector.addMinorError("Requested type $type, for which we do not yet have an embedding.")
                buildType { unit() }
            }
        }
}
