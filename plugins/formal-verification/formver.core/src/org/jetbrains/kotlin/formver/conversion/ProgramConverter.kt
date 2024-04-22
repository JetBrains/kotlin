/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.formver.ErrorCollector
import org.jetbrains.kotlin.formver.PluginConfiguration
import org.jetbrains.kotlin.formver.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.domains.*
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.callables.*
import org.jetbrains.kotlin.formver.embeddings.expression.*
import org.jetbrains.kotlin.formver.linearization.Linearizer
import org.jetbrains.kotlin.formver.linearization.SeqnBuilder
import org.jetbrains.kotlin.formver.linearization.SharedLinearizationState
import org.jetbrains.kotlin.formver.names.*
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Program
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
            fields = SpecialFields.all.map { it.toViper() } +
                    classes.values.flatMap { it.flatMapUniqueFields { _, field -> listOf(field.toViper()) } }.distinctBy { it.name },
            functions = SpecialFunctions.all,
            methods = SpecialMethods.all + methods.values.mapNotNull { it.viperMethod }.toList(),
            predicates = classes.values.map { it.predicate }
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

    override fun embedFunction(symbol: FirFunctionSymbol<*>): FunctionEmbedding =
        methods.getOrPut(symbol.embedName(this)) {
            val signature = embedFullSignature(symbol)
            val callable = processCallable(symbol, signature)
            UserFunctionEmbedding(callable)
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
                val newEmbedding = ClassTypeEmbedding(className, symbol.classKind == ClassKind.INTERFACE)
                classes[className] = newEmbedding

                // Phase 2
                newEmbedding.initSuperTypes(symbol.resolvedSuperTypes.map(::embedType))

                // Phase 3
                val properties = symbol.propertySymbols
                newEmbedding.initFields(properties.mapNotNull { processBackingFields(it, newEmbedding) }.toMap())

                // Phase 4
                properties.forEach { processProperty(it, newEmbedding) }
                newEmbedding
            }
            else -> existingEmbedding
        }
    }

    override fun embedType(type: ConeKotlinType): TypeEmbedding = when {
        type is ConeErrorType -> error("Encountered an erroneous type: $type")
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
        properties[symbol.callableId.embedMemberPropertyName()] ?: error("Unknown property ${symbol.callableId}")
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
                constructedClass.findField(callableId.embedUnscopedPropertyName())?.let { paramSymbol to it }
            }
        }.toMap()
    }

    override fun embedFunctionSignature(symbol: FirFunctionSymbol<*>): FunctionSignature {
        val retType = embedType(symbol.resolvedReturnTypeRef.type)
        val receiverType = symbol.receiverType
        return object : FunctionSignature {
            // TODO: figure out whether we want a symbol here and how to get it.
            override val receiver =
                receiverType?.let { PlaceholderVariableEmbedding(ThisReceiverName, embedType(it)) }
            override val params = symbol.valueParameterSymbols.map {
                FirVariableEmbedding(it.embedName(), embedType(it.resolvedReturnType), it)
            }
            override val returnType = retType
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
            override fun getPreconditions(returnVariable: VariableEmbedding) =
                subSignature.formalArgs.flatMap { it.pureInvariants() } +
                        subSignature.formalArgs.flatMap { it.accessInvariants() } +
                        contractVisitor.getPreconditions(ContractVisitorContext(returnVariable, symbol)) +
                        subSignature.stdLibPreConditions()

            override fun getPostconditions(returnVariable: VariableEmbedding) =
                subSignature.formalArgs.flatMap { it.accessInvariants() } +
                        subSignature.params.flatMap { it.dynamicInvariants() } +
                        returnVariable.pureInvariants() +
                        returnVariable.provenInvariants() +
                        returnVariable.accessInvariants() +
                        contractVisitor.getPostconditions(ContractVisitorContext(returnVariable, symbol)) +
                        subSignature.stdLibPostConditions(returnVariable) +
                        primaryConstructorInvariants(returnVariable)

            fun primaryConstructorInvariants(returnVariable: VariableEmbedding) =
                params.mapNotNull { param ->
                    constructorParamSymbolsToFields[param.symbol]?.let { field ->
                        (field.accessPolicy == AccessPolicy.ALWAYS_READABLE).ifTrue {
                            EqCmp(PrimitiveFieldAccess(returnVariable, field), param)
                        }
                    }
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
    private fun processBackingFields(symbol: FirPropertySymbol, embedding: ClassTypeEmbedding): Pair<SimpleKotlinName, FieldEmbedding>? {
        val unscopedName = symbol.callableId.embedUnscopedPropertyName()
        val name = symbol.callableId.embedMemberPropertyName()
        embedding.findAncestorField(unscopedName)?.let { return null }
        val backingField = name.specialEmbedding() ?: symbol.hasBackingField.ifTrue {
            UserFieldEmbedding(
                name,
                embedType(symbol.resolvedReturnType),
                symbol
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
                scopeIndexProducer.getFresh(),
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
        val linearizer = Linearizer(SharedLinearizationState(anonVarProducer), SeqnBuilder(declaration.source), declaration.source)
        bodyExp.toViperUnusedResult(linearizer)
        return FunctionBodyEmbedding(linearizer.block, returnTarget, bodyExp)
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
