/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameterCopy
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.UnsafePluginApi
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.MutableJavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeTypeMismatch
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.processAllCallables
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.LombokNames.TABLE_ID
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations.AbstractBuilder
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations.Singular
import org.jetbrains.kotlin.lombok.config.LombokService
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.lombok.generators.kotlin.buildJvmStaticAnnotationCallOrError
import org.jetbrains.kotlin.lombok.generators.kotlin.createConstructorIfGeneratedCompanion
import org.jetbrains.kotlin.lombok.generators.kotlin.findAnnotationOnPropertyOrField
import org.jetbrains.kotlin.lombok.generators.kotlin.isCompanionNeeded
import org.jetbrains.kotlin.lombok.generators.kotlin.needsConstructorIfGeneratedCompanion
import org.jetbrains.kotlin.lombok.java.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class BuilderDeclarationType {
    sealed class Class : BuilderDeclarationType() {
        object Companion : Class()
        object Builder : Class()
    }

    object Field : BuilderDeclarationType()

    class DefaultFlagField(val fieldName: Name) : BuilderDeclarationType()

    sealed class Function : BuilderDeclarationType() {
        object Setter : Function()

        /**
         * [entitySymbol] links this generated `build()` to the declaration `@Builder` was placed on, so that
         * IR invokes exactly the entity constructor this `build()` belongs to. That is needed because a class
         * may carry several `@Builder`-annotated secondary constructors, whose fields the FIR side merges
         * into a single builder class.
         *
         * For a class-level `@Builder` this is the class's own symbol, which matches no constructor - and
         * rightly so, as the entity's primary/only constructor is unambiguous there.
         *
         * fir2ir records this very symbol on the declaration it produces, so the two sides are tied together
         * by identity rather than by declaration names or source offsets.
         */
        class Build(val entitySymbol: FirBasedSymbol<*>) : Function()
        object Builder : Function()
        object ToBuilder : Function()
    }

    sealed class SingularFunction(val fieldName: Name) : Function() {
        class AddSingle(fieldName: Name) : SingularFunction(fieldName)
        class AddAll(fieldName: Name) : SingularFunction(fieldName)
        class Clear(fieldName: Name) : SingularFunction(fieldName)
    }
}

class BuilderGeneratorKey(val type: BuilderDeclarationType) : LombokDeclarationKey()

abstract class AbstractBuilderGenerator<T : AbstractBuilder>(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val TO_BUILDER = Name.identifier("toBuilder")
    }

    protected val lombokService: LombokService
        get() = session.lombokService

    protected data class BuilderKey(val owner: FirClassSymbol<*>, val name: Name)

    protected val builderClassesCache: FirCache<BuilderKey, FirRegularClassSymbol?, Nothing?> =
        session.firCachesFactory.createCache(::createAndInitializeBuilder)

    private val builderWithDeclarationsCache: FirCache<FirClassSymbol<*>, List<BuilderWithDeclaration<T>>?, Nothing?> =
        session.firCachesFactory.createCache(::extractBuilderWithDeclarations)

    protected class GeneratedCallables(
        val functions: Map<Name, FirNamedFunctionSymbol>,
        val variables: Map<Name, FirVariableSymbol<*>>,
    )

    // Lombok doesn't add a new function/field/property if a declaration with the same name already exists disregarding parameters.
    // It means the multimap with several declarations on the same name is unnecessary.
    // But we have to distinguish between functions and fields/properties because it's allowed to have a function and a field/property with the same name.
    private val callablesCache: FirCache<FirClassSymbol<*>, GeneratedCallables?, MemberGenerationContext?> =
        session.firCachesFactory.createCache(::createCallables)

    protected abstract val builderModality: Modality

    protected abstract val annotationClassId: ClassId

    protected abstract fun getBuilder(symbol: FirBasedSymbol<*>): T?

    protected abstract fun getExtraTypeArguments(): List<ConeTypeProjection>

    protected abstract fun getBuilderType(builderSymbol: FirClassSymbol<*>): ConeKotlinType?

    protected abstract fun MutableMap<Name, FirNamedFunctionSymbol>.addSpecialBuilderMethods(
        builder: T,
        builderSymbol: FirClassSymbol<*>,
        builderDeclaration: FirDeclaration,
        substitutor: ConeSubstitutor,
        existingFunctionNames: Set<Name>,
    )

    protected abstract fun FirRegularClassBuilder.completeBuilder(
        classSymbol: FirClassSymbol<*>,
        builderSymbol: FirClassSymbol<*>,
        builder: T,
    )

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return buildSet {
            if (classSymbol.needsConstructorIfGeneratedCompanion<BuilderGeneratorKey>()) {
                add(SpecialNames.INIT)
            }

            callablesCache.getValue(classSymbol, context)?.let {
                addAll(it.functions.keys)
                addAll(it.variables.keys)
            }
        }
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        return buildSet {
            if (isCompanionNeeded(classSymbol, context) && needsCompanionForStaticBuilder(classSymbol)) {
                add(DEFAULT_NAME_FOR_COMPANION_OBJECT)
            }

            addAll(getBuilderNames(classSymbol))
        }
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val classSymbol = context?.owner ?: return emptyList()
        return callablesCache.getValue(classSymbol, context)?.let {
            it.functions[callableId.callableName]?.let { function -> listOf(function) }
        } ?: emptyList()
    }

    @UnsafePluginApi
    override fun generateFields(callableId: CallableId, context: MemberGenerationContext?): List<FirFieldSymbol> {
        return generateVariables<FirFieldSymbol>(callableId, context)
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        return generateVariables<FirPropertySymbol>(callableId, context)
    }

    private inline fun <reified V : FirVariableSymbol<*>> generateVariables(callableId: CallableId, context: MemberGenerationContext?): List<V> {
        val classSymbol = context?.owner ?: return emptyList()
        return callablesCache.getValue(classSymbol, context)?.let {
            (it.variables[callableId.callableName] as? V)?.let { variable -> listOf(variable) }
        } ?: emptyList()
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        return buildList {
            createConstructorIfGeneratedCompanion<BuilderGeneratorKey>(context.owner)?.let {
                add(it)
            }
        }
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        if (name == DEFAULT_NAME_FOR_COMPANION_OBJECT) {
            return runIf(needsCompanionForStaticBuilder(owner)) {
                createCompanionObject(owner, BuilderGeneratorKey(BuilderDeclarationType.Class.Companion)).symbol
            }
        }

        return builderClassesCache.getValue(BuilderKey(owner, name))
    }

    /**
     * Whether [classSymbol] needs a generated companion object to host its `builder()` factories. Only a static
     * builder needs one — a `@Builder` method's factory is an instance method on the entity itself, so a class
     * carrying nothing but method builders must not grow an otherwise empty companion.
     */
    private fun needsCompanionForStaticBuilder(classSymbol: FirClassSymbol<*>): Boolean =
        builderWithDeclarationsCache.getValue(classSymbol)?.any { it.declaration.isStaticDeclaration } == true

    /**
     * The same class can have both builder and entity methods in case of names clashing.
     * That's why we need to use both [addBuilderCallables] and [addEntityMethods].
     */
    private fun createCallables(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext?): GeneratedCallables? {
        val existingFunctionNames = mutableSetOf<Name>()
        val existingVariableNames = mutableSetOf<Name>()

        context?.declaredScope?.processAllCallables {
            when (it) {
                is FirNamedFunctionSymbol -> {
                    existingFunctionNames.add(it.name)
                }
                is FirFieldSymbol,
                is FirPropertySymbol,
                    -> {
                    existingVariableNames.add(it.name)
                }
            }
        }

        val generatedFunctions = mutableMapOf<Name, FirNamedFunctionSymbol>()
        val generatedVariables = mutableMapOf<Name, FirVariableSymbol<*>>()

        val classWithBuilderAnnotations = if (classSymbol.isCompanion) {
            // `@Builder` never applies to an object, so a companion is never an entity itself: it merely hosts
            // the `@JvmStatic` `builder()` functions of its containing entity, emulating Java's static factory
            // methods. Hence the annotations are looked up on that entity — `extractBuilderWithDeclarations`
            // collects both the entity's own ones and any declared inside this very companion.
            classSymbol.getContainingClassSymbol() as FirClassSymbol<*>
        } else {
            // Builder functions always belong to builder class in Java and Kotlin.
            context(generatedFunctions, generatedVariables) {
                addBuilderCallables(classSymbol, existingFunctionNames, existingVariableNames)
            }
            classSymbol
        }

        builderWithDeclarationsCache.getValue(classWithBuilderAnnotations)?.let { builderWithDeclarations ->
            generatedFunctions.addEntityMethods(
                builderWithDeclarations = builderWithDeclarations,
                entitySymbol = classWithBuilderAnnotations,
                existingFunctionNames = existingFunctionNames,
                containingClassSymbol = classSymbol,
            )
        }

        return runIf(generatedFunctions.isNotEmpty() || generatedVariables.isNotEmpty()) {
            GeneratedCallables(generatedFunctions, generatedVariables)
        }
    }

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    context(generatedFunctions: MutableMap<Name, FirNamedFunctionSymbol>, generatedVariables: MutableMap<Name, FirVariableSymbol<*>>)
    private fun addBuilderCallables(
        builderSymbol: FirClassSymbol<*>,
        existingFunctionNames: Set<Name>,
        existingVariableNames: Set<Name>,
    ) {
        val containingClassSymbol = builderSymbol.getContainingClassSymbol() as? FirClassSymbol<*> ?: return
        // `containingClassSymbol` is always the entity, never its companion — `createEmptyBuilderClass` always
        // nests the builder under the entity — so this covers companion-declared `@Builder` functions too.
        val builderWithDeclarations = builderWithDeclarationsCache.getValue(containingClassSymbol) ?: return
        val builderName = builderSymbol.classId.shortClassName.asString()
        val builderFir = builderSymbol.fir as? FirRegularClass
        val entityClass = containingClassSymbol.fir as FirRegularClass

        val nestedClassifierScope =
            containingClassSymbol.fir.scopeProvider.getNestedClassifierScope(containingClassSymbol.fir, session, ScopeSession())
        var builderSymbolAlreadyExists = false // TODO: distinguish explicit/generated builders via origin, it's blocked by KT-79778
        nestedClassifierScope?.processClassifiersByName(builderSymbol.name) {
            builderSymbolAlreadyExists = true
        }

        for ((builder, declaration) in builderWithDeclarations) {
            val containingClassBuilderName = builder.getBuilderClassShortName(declaration)
            // Make sure the current class is really a builder of the containing parent
            if (builderName != containingClassBuilderName) continue

            val typeParametersMapping =
                declaration.extractTypeParametersMapping(newContainingDeclarationSymbol = builderSymbol, existingDeclaration = true)
            val substitutor = substitutorByMap(typeParametersMapping.entries.associate { it.key.symbol to it.value.toConeType() }, session)

            if (builderSymbolAlreadyExists && builderFir is FirJavaClass) {
                // For already existing explicit builders, initialize and populate type parameters to link generated functions with them.
                // Unfortunately, we can't do it on the nested classes generation step because scope is being traversed recursively (that would lead to StackOverflow)
                // For Lombok-generated builders, createEmptyBuilderClass already sets up the correct mapping
                builderFir.classJavaTypeParameterStack.populateTypeParametersMapping(typeParametersMapping)
            }

            generatedFunctions.addSpecialBuilderMethods(
                builder,
                builderSymbol = builderSymbol,
                builderDeclaration = declaration,
                substitutor = substitutor,
                existingFunctionNames = existingFunctionNames
            )

            val items: List<FirVariable> = when (declaration) {
                is FirRegularClass -> {
                    val isJavaClass = entityClass is FirJavaClass
                    if (isJavaClass && entityClass.isRecord) {
                        entityClass.primaryConstructorIfAny(session)?.valueParameterSymbols?.map { it.fir } ?: emptyList()
                    } else {
                        entityClass.declarations.mapNotNull { declaration ->
                            // A static field is never a builder field in Lombok. On the Kotlin side these are what a
                            // `companion { }` block declares (KT-88367); on the Java side they are plain `static`
                            // fields, which real Lombok leaves out of the builder just the same.
                            if (isJavaClass) {
                                (declaration as? FirJavaField)?.takeIf { !it.isStatic }
                            } else {
                                (declaration as? FirProperty)?.takeIf { it.hasBackingField && !it.isStatic }
                            }
                        }
                    }
                }
                is FirConstructor -> declaration.valueParameters
                is FirNamedFunction -> declaration.valueParameters
                else -> emptyList()
            }
            for (item in items) {
                val singularAnnotation = item.getAnnotationByClassId(LombokNames.SINGULAR_ID, session)
                    ?: (item.symbol as? FirPropertySymbol)?.backingFieldSymbol?.getAnnotationByClassId(LombokNames.SINGULAR_ID, session)
                val singular: Singular? = singularAnnotation?.let { Singular.extract(it, session) }
                val hasBuilderDefault = (item.symbol as? FirPropertySymbol)
                    ?.findAnnotationOnPropertyOrField(LombokNames.BUILDER_DEFAULT_ID, session) != null

                generatedVariables.addIfNonClashing(item.name, existingVariableNames) {
                    if (builderSymbol.hasJavaOrigin) {
                        buildJavaField {
                            isFromSource = true
                            lazyHasConstantInitializer = lazy { false }
                            lazyHasInitializer = lazy { false }
                            this@buildJavaField.containingClassSymbol = builderSymbol

                            moduleData = containingClassSymbol.moduleData
                            status = FirResolvedDeclarationStatusImpl(
                                Visibilities.Private, Modality.OPEN, Visibilities.Private.toEffectiveVisibility(builderSymbol)
                            )
                            isLocal = false
                            returnTypeRef = item.returnTypeRef
                            name = it
                            isVar = false
                            symbol = FirFieldSymbol(CallableId(builderSymbol.classId, it))
                            dispatchReceiverType = builderSymbol.defaultType()
                        }.symbol
                    } else {
                        val substitutedType = substitutor.substituteOrSelf(item.returnTypeRef.coneType)
                        // A `@Singular` builder field always holds a nullable, mutable collection (`null` == "not yet
                        // created"), regardless of the entity property's own type/nullability — mirrors Lombok's
                        // Java codegen, which always backs a `@Singular` field with a lazily initialized mutable collection.
                        val fieldType = if (singular != null) {
                            substitutedType.toBackingMutableCollectionType()
                        } else {
                            substitutedType
                        }
                        createMemberProperty(
                            owner = builderSymbol,
                            key = BuilderGeneratorKey(BuilderDeclarationType.Field),
                            name = it,
                            returnType = fieldType,
                            isVal = false,
                        ) {
                            modality = Modality.FINAL
                            visibility = Visibilities.Private
                        }.symbol
                    }
                }

                if (!builderSymbol.hasJavaOrigin && hasBuilderDefault && singular == null) {
                    generatedVariables.addIfNonClashing(Name.identifier(item.name.identifier + $$"$set"), existingVariableNames) {
                        createMemberProperty(
                            owner = builderSymbol,
                            key = BuilderGeneratorKey(BuilderDeclarationType.DefaultFlagField(item.name)),
                            name = it,
                            returnType = session.builtinTypes.booleanType.coneType,
                            isVal = false,
                        ) {
                            modality = Modality.FINAL
                            visibility = Visibilities.Private
                        }.symbol
                    }
                }

                when (singular) {
                    null -> {
                        generatedFunctions.addSetterMethod(
                            builder,
                            item,
                            substitutor,
                            builderSymbol = builderSymbol,
                            existingFunctionNames = existingFunctionNames
                        )
                    }
                    else -> {
                        generatedFunctions.addMethodsForSingularFields(
                            builder,
                            singular,
                            item,
                            substitutor,
                            builderSymbol = builderSymbol,
                            existingFunctionNames = existingFunctionNames
                        )
                    }
                }
            }
        }
    }

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun MutableMap<Name, FirNamedFunctionSymbol>.addEntityMethods(
        builderWithDeclarations: List<BuilderWithDeclaration<T>>,
        entitySymbol: FirClassSymbol<*>,
        existingFunctionNames: Set<Name>,
        containingClassSymbol: FirClassSymbol<*>,
    ) {
        for ((val builder, val builderDeclaration = declaration) in builderWithDeclarations) {
            val visibility = builder.accessLevel.toVisibility(containingClassSymbol) ?: continue
            val entityClassId = entitySymbol.classId
            val builderClassName = Name.identifier(builder.getBuilderClassShortName(builderDeclaration) ?: continue)
            val builderClassId = entityClassId.createNestedClassId(builderClassName)

            /**
             * Accessing `scopeProvider` on Kotlin classes here triggers infinite recursion, whereas it is safe for Java classes.
             * Conversely, Java FIR declarations do not include nested classes, while Kotlin FIR declarations do.
             * Thus, different strategies are required to check if a builder already exists.
             */
            var existingBuilder: FirClassSymbol<*>? = null
            val entityFir = entitySymbol.fir
            if (entityFir is FirJavaClass) {
                val nestedClassifierScope = entityFir.scopeProvider.getNestedClassifierScope(entityFir, session, ScopeSession())
                nestedClassifierScope?.processClassifiersByName(builderClassName) {
                    if (existingBuilder == null && it is FirClassSymbol<*>) {
                        existingBuilder = it
                    }
                }
            } else {
                // Matched by name rather than "first nested class" because the entity may also have an
                // unrelated nested class declared first — e.g. an explicit companion object hosting the
                // `@Builder`-annotated factory function itself.
                existingBuilder = entityFir.declarations.firstNotNullOfOrNull {
                    runIf(it is FirRegularClass && it.name == builderClassName) {
                        it.symbol
                    }
                }
            }

            fun createBuilderType(typeParameterSymbols: Collection<FirTypeParameter>): ConeClassLikeType {
                // A builder generated for a non-static method is an inner class, so its type arguments are its
                // own followed by the entity class's (see `createEmptyBuilderClass`) — `Entity<E>.Builder<P>`.
                val outerTypeArguments = runIf(!builderDeclaration.isStaticDeclaration) {
                    @OptIn(SymbolInternals::class)
                    entitySymbol.typeParameterSymbols.map { it.fir.toConeType() }
                }.orEmpty()
                val newTypeArguments = typeParameterSymbols.map { it.toConeType() } + outerTypeArguments + getExtraTypeArguments()
                val existingBuilderType = existingBuilder?.defaultType()
                val existingBuilderTypeArguments = existingBuilderType?.typeArguments
                val resultType = builderClassId.constructClassLikeType(newTypeArguments.toTypedArray())

                // Create an error type if the existing builder has a type that doesn't conform to the expected.
                // It satisfies compiler expectations and prevents crashing.
                return if (existingBuilderTypeArguments == null || existingBuilderTypeArguments.size == newTypeArguments.size) {
                    resultType
                } else {
                    ConeErrorType(ConeTypeMismatch(existingBuilderType, resultType))
                }
            }

            val isStaticBuilderFunction = builderDeclaration.isStaticDeclaration

            // Where the `builder()` factory belongs: for a static builder it has to be callable without an
            // instance, which in Kotlin means the entity's companion object and in Java a static method on the
            // entity; for a `@Builder` method it is an instance method on the entity, since the builder it
            // returns captures that very instance.
            val shouldAddBuilderFactory = when {
                containingClassSymbol.hasJavaOrigin -> true
                isStaticBuilderFunction -> containingClassSymbol.isCompanion
                else -> !containingClassSymbol.isCompanion
            }

            if (shouldAddBuilderFactory) {
                addIfNonClashing(Name.identifier(builder.builderMethodName), existingFunctionNames) { name ->
                    if (containingClassSymbol.hasJavaOrigin) {
                        val methodSymbol = FirNamedFunctionSymbol(CallableId(entitySymbol.classId, name))
                        val methodTypeParameters =
                            builderDeclaration.extractTypeParametersMapping(methodSymbol, existingDeclaration = false).values

                        entitySymbol.createJavaMethod(
                            name,
                            valueParameters = emptyList(),
                            returnTypeRef = createBuilderType(methodTypeParameters).toFirResolvedTypeRef(),
                            visibility = visibility,
                            modality = Modality.OPEN,
                            dispatchReceiverType = if (isStaticBuilderFunction) null else builderDeclaration.dispatchReceiverType,
                            isStatic = isStaticBuilderFunction,
                            methodSymbol = methodSymbol,
                            methodTypeParameters = methodTypeParameters,
                        ).symbol
                    } else {
                        val builderTypeParameters = when (builderDeclaration) {
                            is FirClass -> builderDeclaration.typeParameters
                            is FirConstructor -> builderDeclaration.typeParameters
                            is FirNamedFunction -> builderDeclaration.typeParameters
                            else -> emptyList() // Use the fallback just in case, although it's normally unreachable
                        }

                        createMemberFunction(
                            owner = containingClassSymbol,
                            key = BuilderGeneratorKey(BuilderDeclarationType.Function.Builder),
                            name = name,
                            returnTypeProvider = {
                                createBuilderType(it)
                            },
                            config = {
                                this.visibility = visibility
                                builderTypeParameters.forEach { typeParameter ->
                                    val typeParameterSymbol = typeParameter.symbol
                                    typeParameter(typeParameterSymbol.name, typeParameterSymbol.variance) {
                                        typeParameterSymbol.resolvedBounds.forEach { bound(it.coneType) }
                                    }
                                }
                            }
                        ).apply {
                            if (isStaticBuilderFunction) {
                                replaceAnnotations(listOf(symbol.buildJvmStaticAnnotationCallOrError(session)))
                            }
                        }.symbol
                    }
                }
            }

            if (builder.requiresToBuilder && !containingClassSymbol.isCompanion) {
                addIfNonClashing(TO_BUILDER, existingFunctionNames) { name ->
                    // toBuilder() is always an instance method, so the class type parameters are
                    // already provided by the dispatch receiver. The method must not introduce its
                    // own independent type parameters — otherwise call-site inference would fail.
                    createJavaOrKotlinMemberFunction(
                        owner = containingClassSymbol,
                        name = name,
                        valueParameters = emptyList(),
                        returnTypeRef = @OptIn(SymbolInternals::class) createBuilderType(entitySymbol.typeParameterSymbols.map { it.fir }).toFirResolvedTypeRef(),
                        visibility = visibility,
                        modality = Modality.OPEN,
                        createKey = {
                            BuilderGeneratorKey(BuilderDeclarationType.Function.ToBuilder)
                        }
                    )
                }
            }
        }
    }

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    protected fun getBuilderNames(classSymbol: FirClassSymbol<*>): Set<Name> = buildSet {
        val builderWithDeclarations = builderWithDeclarationsCache.getValue(classSymbol)

        if (builderWithDeclarations != null) {
            /**
             * Existing classifier names are extracted differently for Java and Kotlin:
             *  - For Java: Names are not present in FIR declarations but can be safely retrieved
             *    via `getNestedClassifierScope`.
             *  - For Kotlin: Names exist in FIR declarations, but calling scope functions here
             *    triggers infinite recursion.
             *
             * `context.declaredScope?.getClassifierNames()` cannot be used because:
             *  - [getBuilderNames] can be called on a supertype where `context.owner`
             *    is bound to its subclass, leading to incorrect scope resolution.
             *  - It does not account for declarations already explicitly defined in source.
             */
            val classFir = classSymbol.fir
            val existingClassifierNames = if (classFir is FirJavaClass) {
                val nestedClassifierScope = classFir.scopeProvider.getNestedClassifierScope(classFir, session, ScopeSession())
                nestedClassifierScope?.getClassifierNames()?.toSet() ?: emptySet()
            } else {
                buildSet {
                    classSymbol.fir.declarations.mapNotNullTo(this) { (it as? FirClassLikeDeclaration)?.nameOrSpecialName }
                }
            }

            for ((val builder, val builderDeclaration = declaration) in builderWithDeclarations) {
                if (builder.accessLevel.toVisibility(classSymbol) == null) continue
                val builderName = Name.identifier(builder.getBuilderClassShortName(builderDeclaration) ?: continue)

                // Don't generate classes if they already exist
                if (!existingClassifierNames.contains(builderName)) {
                    add(builderName)
                }
            }
        }
    }

    private fun createAndInitializeBuilder(key: BuilderKey): FirRegularClassSymbol? {
        val [owner, name] = key

        val builderWithDeclarations = builderWithDeclarationsCache.getValue(owner) ?: return null

        for ((val builder, val builderDeclaration = declaration) in builderWithDeclarations) {
            val visibility = builder.accessLevel.toVisibility(owner) ?: continue
            val builderName = Name.identifier(builder.getBuilderClassShortName(builderDeclaration) ?: continue)

            if (builderName == name) {
                return owner.createEmptyBuilderClass(
                    session,
                    builderName,
                    visibility,
                    builderDeclaration,
                    builder,
                ).symbol
            }
        }

        return null
    }

    /**
     * All `@Builder`-with-declaration pairs relevant to [classSymbol] as an entity: its own
     * class/constructor/member-function annotations, plus — since a function declared directly inside its
     * companion object is the Kotlin analogue of a Java static factory method — any `@Builder`-annotated
     * function declared in that companion.
     */
    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun extractBuilderWithDeclarations(classSymbol: FirClassSymbol<*>): List<BuilderWithDeclaration<T>>? {
        // A companion object is never an entity in its own right: the Builder class always nests under the
        // containing entity class, never under its companion, so the companion's own `@Builder`-annotated
        // functions are only ever collected as part of the containing class's view (below).
        if (classSymbol.isCompanion) return null

        val annotationSymbol = annotationClassId.toSymbol(session) as? FirRegularClassSymbol ?: return emptyList()
        val allowedTargets = annotationSymbol.fir.getAllowedAnnotationTargets(session)

        fun MutableList<BuilderWithDeclaration<T>>.addAnnotatedCallables(owner: FirClassSymbol<*>) {
            for (declarationSymbol in owner.declarationSymbols) {
                if (declarationSymbol is FirConstructorSymbol && allowedTargets.contains(KotlinTarget.CONSTRUCTOR) ||
                    declarationSymbol is FirFunctionSymbol<*> && allowedTargets.contains(KotlinTarget.FUNCTION)
                ) {
                    // Left alone; `FirLombokBuilderChecker` reports it as
                    // `BUILDER_WITH_RECEIVER_OR_CONTEXT_PARAMETERS`.
                    if (declarationSymbol.hasReceiverOrContextParameters) continue
                    getBuilder(declarationSymbol)?.let { add(BuilderWithDeclaration(it, declarationSymbol.fir)) }
                }
            }
        }

        return buildList {
            // Only the class-level `@Builder` is dropped: a `@Builder`-annotated member function of an interface,
            // an enum class or an object still builds whatever that function returns, and is a legitimate case.
            if (allowedTargets.contains(KotlinTarget.CLASS) && classSymbol.isPlainClass) {
                getBuilder(classSymbol)?.let { add(BuilderWithDeclaration(it, classSymbol.fir)) }
            }

            addAnnotatedCallables(classSymbol)

            // The raw (non-lazy-resolving) symbol, not `resolvedCompanionObjectSymbol`: this runs as part of a
            // `FirDeclarationGenerationExtension` callback, and `resolvedCompanionObjectSymbol` would try to
            // `lazyResolveToPhase(COMPANION_GENERATION)` — a phase this callback is itself running under, which
            // FIR's lazy-resolve contract forbids (`FirLazyResolveContractViolationException`). An explicitly
            // source-declared companion (the only kind relevant here — we never generate one) already has its
            // raw symbol populated well before this phase, so the fast path suffices.
            (classSymbol as? FirRegularClassSymbol)?.companionObjectSymbol?.let { addAnnotatedCallables(it) }
        }.takeIf { it.isNotEmpty() }
    }

    private data class BuilderWithDeclaration<T>(val builder: T, val declaration: FirDeclaration)

    private fun MutableMap<Name, FirNamedFunctionSymbol>.addSetterMethod(
        builder: AbstractBuilder,
        item: FirVariable,
        substitutor: ConeSubstitutor,
        builderSymbol: FirClassSymbol<*>,
        existingFunctionNames: Set<Name>,
    ) {
        val fieldName = item.name
        val setterName = fieldName.toMethodName(builder)
        val builderType = getBuilderType(builderSymbol) ?: return
        val visibility = builder.builderFunctionsAccessLevel.toVisibility(builderSymbol) ?: return

        addIfNonClashing(setterName, existingFunctionNames) {
            createJavaOrKotlinMemberFunction(
                owner = builderSymbol,
                name = it,
                valueParameters = listOf(
                    ConeLombokValueParameter(
                        name = fieldName,
                        // For Kotlin, `item.returnTypeRef` is already resolved, and its type directly references the
                        // entity class's type-parameter symbols, which differ from the builder's own copied symbols
                        // (see `extractTypeParametersMapping`), so we must substitute them explicitly.
                        // For Java, `item.returnTypeRef` is an `FirJavaTypeRef` that resolves type variables
                        // by name via the owning declaration's `javaTypeParameterStack`, which is populated with the
                        // builder's copied type parameters (see `populateTypeParametersMapping`), so no substitution is needed.
                        typeRef = if (builderSymbol.hasJavaOrigin)
                            item.returnTypeRef
                        else
                            substitutor.substituteOrSelf(item.returnTypeRef.coneType).toFirResolvedTypeRef()
                    )
                ),
                returnTypeRef = builderType.toFirResolvedTypeRef(),
                visibility = visibility,
                modality = Modality.OPEN,
                createKey = {
                    BuilderGeneratorKey(BuilderDeclarationType.Function.Setter)
                }
            )
        }
    }

    /**
     * Type kind used in AddAll method signature for a field with `@Singular` annotation.
     */
    private enum class SingularAddAllParameterType {
        Iterable,
        Collection,
        Map,
        Table,
    }

    /** Returns a type of mutable collection a `@Singular` builder field is backed by, for Kotlin-origin builders. */
    private fun ConeKotlinType.toBackingMutableCollectionType(): ConeKotlinType {
        val mutableCollectionClassId =
            when (classId) {
                StandardClassIds.List, StandardClassIds.MutableList,
                StandardClassIds.Collection, StandardClassIds.MutableCollection,
                StandardClassIds.Iterable, StandardClassIds.MutableIterable,
                LombokNames.IMMUTABLE_LIST_ID, LombokNames.IMMUTABLE_COLLECTION_ID,
                    -> StandardClassIds.MutableList
                StandardClassIds.Set, StandardClassIds.MutableSet,
                LombokNames.IMMUTABLE_SET_ID, LombokNames.IMMUTABLE_SORTED_SET_ID,
                    -> StandardClassIds.MutableSet
                StandardClassIds.Map, StandardClassIds.MutableMap,
                LombokNames.IMMUTABLE_MAP_ID, LombokNames.IMMUTABLE_BI_MAP_ID, LombokNames.IMMUTABLE_SORTED_MAP_ID,
                    -> StandardClassIds.MutableMap
                LombokNames.IMMUTABLE_TABLE_ID -> TABLE_ID
                else -> null
            }
        return mutableCollectionClassId?.constructClassLikeType(
            (this as ConeClassLikeType).typeArguments,
            isMarkedNullable = true,
        ) ?: this
    }

    private fun MutableMap<Name, FirNamedFunctionSymbol>.addMethodsForSingularFields(
        builder: AbstractBuilder,
        singular: Singular,
        item: FirVariable,
        substitutor: ConeSubstitutor,
        builderSymbol: FirClassSymbol<*>,
        existingFunctionNames: Set<Name>,
    ) {
        val typeRef = item.returnTypeRef

        val typeId = when (typeRef) {
            is FirJavaTypeRef -> ((typeRef.type as? JavaClassifierType)?.classifier as? JavaClass)?.fqName?.let(ClassId::topLevel)
            is FirResolvedTypeRef -> typeRef.coneType.classId
            else -> return
        }

        // Early return and report `CANNOT_SINGULARIZE_NAME` if explicit name isn't specified and singularization fails
        val nameInSingularForm = (singular.singularName
            ?: runIf(session.lombokService.config.singularAuto) { Singulars.autoSingularize(item.name.identifier) })
            ?.let(Name::identifier)
            ?: return

        val valueParameters: List<ConeLombokValueParameter>
        val collectionType: SingularAddAllParameterType
        val typeArgumentRefs: List<FirTypeRef>

        when (typeId) {
            in LombokNames.SUPPORTED_COLLECTION_IDS -> {
                val parameterTypeRef = typeRef.parameterType(0, substitutor) ?: return
                valueParameters = listOf(
                    ConeLombokValueParameter(nameInSingularForm, parameterTypeRef)
                )

                collectionType = when (typeId) {
                    in LombokNames.SUPPORTED_GUAVA_COLLECTION_IDS -> SingularAddAllParameterType.Iterable
                    else -> SingularAddAllParameterType.Collection
                }
                typeArgumentRefs = listOf(parameterTypeRef)
            }

            in LombokNames.SUPPORTED_MAP_IDS -> {
                val keyTypeRef = typeRef.parameterType(0, substitutor) ?: return
                val valueTypeRef = typeRef.parameterType(1, substitutor) ?: return
                valueParameters = listOf(
                    ConeLombokValueParameter(Name.identifier("key"), keyTypeRef),
                    ConeLombokValueParameter(Name.identifier("value"), valueTypeRef),
                )

                collectionType = SingularAddAllParameterType.Map
                typeArgumentRefs = listOf(keyTypeRef, valueTypeRef)
            }

            in LombokNames.SUPPORTED_TABLE_IDS -> {
                val rowKeyTypeRef = typeRef.parameterType(0, substitutor) ?: return
                val columnKeyTypeRef = typeRef.parameterType(1, substitutor) ?: return
                val valueType = typeRef.parameterType(2, substitutor) ?: return

                valueParameters = listOf(
                    ConeLombokValueParameter(Name.identifier("rowKey"), rowKeyTypeRef),
                    ConeLombokValueParameter(Name.identifier("columnKey"), columnKeyTypeRef),
                    ConeLombokValueParameter(Name.identifier("value"), valueType),
                )

                collectionType = SingularAddAllParameterType.Table
                typeArgumentRefs = listOf(rowKeyTypeRef, columnKeyTypeRef, valueType)
            }

            else -> return // Early return and report `UNSUPPORTED_SINGULAR_TYPE`
        }

        val builderType = getBuilderType(builderSymbol)?.toFirResolvedTypeRef() ?: return

        // Early return in case of `AccessLevel.NONE` is used (it means not generating anything at all)
        val visibility = builder.builderFunctionsAccessLevel.toVisibility(builderSymbol) ?: return

        addIfNonClashing(nameInSingularForm.toMethodName(builder), existingFunctionNames) {
            createJavaOrKotlinMemberFunction(
                owner = builderSymbol,
                name = it,
                valueParameters = valueParameters,
                returnTypeRef = builderType,
                visibility = visibility,
                modality = Modality.OPEN,
                createKey = { BuilderGeneratorKey(BuilderDeclarationType.SingularFunction.AddSingle(item.name)) },
            )
        }

        addIfNonClashing(item.name.toMethodName(builder), existingFunctionNames) { name ->
            val addMultipleParameterTypeRef: FirTypeRef = if (builderSymbol.hasJavaOrigin) {
                // Lombok uses runtime checks instead of @Nullable/@NotNull annotations.
                // We include these annotations to enable static analysis and improve the developer experience.
                val annotations = listOf(
                    if (singular.allowNull) NullabilityJavaAnnotation.Nullable else NullabilityJavaAnnotation.NotNull
                )
                val baseType = when (collectionType) {
                    SingularAddAllParameterType.Iterable -> JavaClasses.Iterable
                    SingularAddAllParameterType.Collection -> JavaClasses.Collection
                    SingularAddAllParameterType.Map -> JavaClasses.Map
                    SingularAddAllParameterType.Table -> JavaClasses.Table
                }
                // `? extends T` for every argument, mirroring the `Collection<? extends T>` Lombok itself
                // generates: an invariant argument rejects a collection of a subtype with `JAVA_TYPE_MISMATCH`.
                val wildcardArguments = typeArgumentRefs.map { DummyJavaExtendsWildcardType((it as FirJavaTypeRef).type) }
                DummyJavaClassType(baseType, wildcardArguments, annotations).toRef(source = null)
            } else {
                val baseType = when (collectionType) {
                    SingularAddAllParameterType.Iterable -> StandardClassIds.Iterable
                    SingularAddAllParameterType.Collection -> StandardClassIds.Collection
                    SingularAddAllParameterType.Map -> StandardClassIds.Map
                    SingularAddAllParameterType.Table -> TABLE_ID
                }
                baseType.constructClassLikeType(
                    typeArgumentRefs.map { (it as FirResolvedTypeRef).coneType }.toTypedArray(),
                    isMarkedNullable = singular.allowNull,
                ).toFirResolvedTypeRef()
            }

            createJavaOrKotlinMemberFunction(
                owner = builderSymbol,
                name = name,
                valueParameters = listOf(ConeLombokValueParameter(item.name, addMultipleParameterTypeRef)),
                returnTypeRef = builderType,
                visibility = visibility,
                modality = Modality.OPEN,
                createKey = { BuilderGeneratorKey(BuilderDeclarationType.SingularFunction.AddAll(item.name)) },
            )
        }

        addIfNonClashing(Name.identifier("clear${item.name.identifier.capitalize()}"), existingFunctionNames) {
            createJavaOrKotlinMemberFunction(
                owner = builderSymbol,
                name = it,
                valueParameters = emptyList(),
                returnTypeRef = builderType,
                visibility = visibility,
                modality = Modality.OPEN,
                createKey = { BuilderGeneratorKey(BuilderDeclarationType.SingularFunction.Clear(item.name)) },
            )
        }
    }

    /**
     * Lombok doesn't add a generated function/field if a class already has a declaration with the same name.
     * The number and types of parameters don't matter, see https://projectlombok.org/features/Builder#overview
     * "Each listed generated element will be silently skipped if that element already exists (disregarding parameter counts and looking only at names)"
     */
    protected inline fun <K : FirCallableSymbol<*>> MutableMap<Name, K>.addIfNonClashing(
        name: Name,
        existingNames: Set<Name>,
        createCallable: (name: Name) -> K
    ) {
        if (name !in existingNames) {
            val _ = getOrPut(name) { createCallable(name) }
        }
    }

    private fun FirClassSymbol<*>.createEmptyBuilderClass(
        session: FirSession,
        name: Name,
        visibility: Visibility,
        builderDeclaration: FirDeclaration,
        builder: T,
    ): FirRegularClass {
        val containingClass = this
        val classId = classId.createNestedClassId(name)
        val builderSymbol = FirRegularClassSymbol(classId)
        val effectiveVisibility: EffectiveVisibility

        val typeParametersMapping = builderDeclaration.extractTypeParametersMapping(builderSymbol, existingDeclaration = false)

        val builderBuilder = if (hasJavaOrigin) {
            effectiveVisibility = containingClass.effectiveVisibility.lowerBound(
                visibility.toEffectiveVisibility(this@createEmptyBuilderClass, forClass = true),
                session.typeContext
            )

            FirJavaClassBuilder().apply {
                containingClassSymbol = containingClass
                isFromSource = true

                // Remap Java type parameters from the containing declaration to the newly created type parameters to make the Java resolve work.
                // Don't care about outer type parameters because builder classes are always static (nested).
                javaTypeParameterStack = MutableJavaTypeParameterStack().apply {
                    populateTypeParametersMapping(typeParametersMapping)
                }

                scopeProvider = JavaScopeProvider
            }
        } else {
            effectiveVisibility = visibility.toEffectiveVisibility(this)

            FirRegularClassBuilder().apply {
                origin = FirDeclarationOrigin.Plugin(BuilderGeneratorKey(BuilderDeclarationType.Class.Builder))
                scopeProvider = session.kotlinScopeProvider
            }
        }

        return builderBuilder.apply {
            moduleData = containingClass.moduleData
            symbol = builderSymbol
            this.name = name

            classKind = ClassKind.CLASS

            this.superTypeRefs += superTypeRefs

            status = FirResolvedDeclarationStatusImpl(
                visibility,
                builderModality,
                effectiveVisibility
            ).apply {
                // A builder for a non-static method is an inner class, mirroring Lombok: `build()` has to
                // invoke the method on the very instance `builder()` was called on, and the method's
                // parameters may refer to the entity class's own type parameters. Both come for free from
                // the outer instance. Every other builder is a plain nested class.
                this.isInner = !builderDeclaration.isStaticDeclaration
                isCompanion = false
                isData = false
                isInline = false
                isFun = classKind == ClassKind.INTERFACE
            }

            typeParametersMapping.mapTo(typeParameters) { it.value }

            // An inner class also lists the outer class's type parameters, after its own — that's what makes
            // them usable in the builder's members, and cone types of the builder index their arguments in
            // that same order (see `createBuilderType`).
            if (status.isInner) {
                containingClass.typeParameterSymbols.mapTo(typeParameters) { buildOuterClassTypeParameterRef { symbol = it } }
            }

            completeBuilder(this@createEmptyBuilderClass, builderSymbol, builder)
        }.build()
    }

    private fun MutableJavaTypeParameterStack.populateTypeParametersMapping(
        extractedTypeParameters: Map<FirTypeParameter, FirTypeParameter>
    ) {
        for ([oldTypeParameter, newTypeParameter] in extractedTypeParameters) {
            addParameter((oldTypeParameter as FirJavaTypeParameter).javaTypeParameter, newTypeParameter.symbol)
        }
    }

    /**
     * Given the following generic class with `@Builder`:
     *
     * ```java
     * @lombok.Builder
     * public class C<T> {
     *     private final T value;
     * }
     * ```
     *
     * That has the following generated builder:
     *
     * ```java
     * import lombok.Generated;
     *
     * public class C<T> {
     *     private final T value;
     *
     *     @Generated
     *     C(T value) {
     *         this.value = value;
     *     }
     *
     *     @Generated
     *     public static <T> CBuilder<T> builder() {
     *         return new CBuilder<T>();
     *     }
     *
     *     @Generated
     *     public static class CBuilder<T> {
     *         @Generated
     *         private T value;
     *
     *         @Generated
     *         CBuilder() {
     *         }
     *
     *         @Generated
     *         public CBuilder<T> value(T value) {
     *             this.value = value;
     *             return this;
     *         }
     *
     *         @Generated
     *         public C<T> build() {
     *             return new C<T>(this.value);
     *         }
     *
     *         @Generated
     *         public String toString() {
     *             return "C.CBuilder(value=" + String.valueOf(this.value) + ")";
     *         }
     *     }
     * }
     * ```
     *
     * We have to initialize the new type parameters for static `builder` (T -> T2) to make Java resolve robust:
     *
     * ```java
     * public static <T2> CBuilder<T2> builder() {
     *     return new CBuilder<T2>();
     * }
     * ```
     *
     * And new type parameters for `CBuilder<T>` with its `build` method (T -> T3);
     *
     * ```java
     * public static class CBuilder<T3> {
     *     ...
     *     @Generated
     *     public CBuilder<T3> value(T3 value) {
     *         this.value = value;
     *         return this;
     *     }
     *     @Generated
     *     public C<T3> build() {
     *         return new C<T3>(this.value);
     *     }
     *     ...
     * }
     * ```
     *
     * The function also handles type parameters on explicitly declared declarations.
     *
     * @return a map used for remapping type parameters on a Java stack
     */
    @OptIn(SymbolInternals::class)
    private fun FirDeclaration.extractTypeParametersMapping(
        newContainingDeclarationSymbol: FirBasedSymbol<*>,
        existingDeclaration: Boolean,
    ): Map<FirTypeParameter, FirTypeParameter> {
        val typeParameters: List<FirTypeParameter> = when (this) {
            is FirClass -> typeParameters.map { it.symbol.fir }
            is FirConstructor -> typeParameters.map { it.symbol.fir }
            is FirNamedFunction -> typeParameters
            else -> emptyList() // Use the fallback just in case, although it's normally unreachable
        }
        return buildMap {
            typeParameters.forEachIndexed { index, typeParameter ->
                this[typeParameter] = runIf(existingDeclaration) {
                    newContainingDeclarationSymbol.typeParameterSymbols?.getOrNull(index)?.fir
                } ?: buildTypeParameterCopy(typeParameter.symbol.fir) {
                    symbol = FirTypeParameterSymbol()
                    containingDeclarationSymbol = newContainingDeclarationSymbol
                }
            }
        }
    }

    /**
     * The short name of the builder class to generate for [builderDeclaration], or `null` if it cannot be
     * inferred at all — in which case no builder is generated.
     */
    private fun T.getBuilderClassShortName(builderDeclaration: FirDeclaration): String? {
        val refinedBuilderClassName = builderClassName ?: session.lombokService.config.builderClassName

        if (hasSpecifiedBuilderClassName) {
            return refinedBuilderClassName
        }

        val builderClassNamePart: String = when (builderDeclaration) {
            is FirRegularClass -> builderDeclaration.name.asString()
            is FirConstructor -> builderDeclaration.nameOrSpecialName.asString()
            is FirNamedFunction -> {
                // If the builder class name is not specified explicitly, infer the name from the method's return type
                // according to Lombok rules.
                //
                // A branch per type-ref kind is needed because this runs twice over the same declaration, at
                // different resolution stages: first from `getNestedClassifiersNames`/`generateNestedClassLikeDeclaration`,
                // as early as SUPERTYPES, where a Kotlin return type is still an unresolved `FirUserTypeRef`, and later
                // from `generateFunctions`, where it has been replaced by a `FirResolvedTypeRef`. Both runs must yield
                // the same name — otherwise `builder()` would reference a builder class that was never generated under
                // that name.
                when (val returnTypeRef = builderDeclaration.returnTypeRef) {
                    is FirJavaTypeRef -> when (val returnType = returnTypeRef.type) {
                        is JavaPrimitiveType -> returnType.type?.typeName?.identifier ?: "Void"
                        is JavaClassifierType -> returnType.classifier?.name?.asString() ?: returnType.presentableText
                        else -> null // Skip generation for unsupported types
                    }
                    is FirUserTypeRef -> returnTypeRef.qualifier.lastOrNull()?.name?.asString()
                    is FirResolvedTypeRef -> {
                        // A bare type parameter (e.g. `fun <M> method(m: M): M`) has no `classId`
                        val coneType = returnTypeRef.coneType
                        coneType.classId?.shortClassName?.asString()
                            ?: (coneType as? ConeTypeParameterType)?.lookupTag?.name?.asString()
                    }
                    // An implicit return type: there is nothing to infer the name from, so no builder is
                    // generated at all. `FirLombokBuilderChecker` reports it as
                    // `BUILDER_REQUIRES_EXPLICIT_RETURN_TYPE`.
                    else -> null
                }
            }
            else -> null // Normally unreachable, but skip generation just in case
        } ?: return null

        return refinedBuilderClassName.replace("*", builderClassNamePart)
    }

    private fun Name.toMethodName(builder: AbstractBuilder): Name {
        val prefix = builder.setterPrefix
        return if (prefix.isNullOrBlank()) {
            this
        } else {
            Name.identifier("${prefix}${identifier.capitalize()}")
        }
    }

    private fun FirTypeRef.parameterType(index: Int, substitutor: ConeSubstitutor): FirTypeRef? {
        return when (this) {
            is FirJavaTypeRef -> {
                // Java type variables are resolved by name via the owning declaration's `javaTypeParameterStack`,
                // which is already populated with the builder's copied type parameters (see
                // `populateTypeParametersMapping`), so no substitution is needed here.
                val javaClassifierType = type as? JavaClassifierType
                if (javaClassifierType != null) {
                    val type = javaClassifierType.typeArguments.getOrNull(index) ?: runIf(javaClassifierType.isRaw) {
                        DummyJavaClassType.ObjectType
                    }
                    type?.toRef(source = null)
                } else {
                    null
                }
            }
            // For Kotlin, the type argument directly references the entity class's type-parameter symbols,
            // which differ from the builder's own copied symbols (see `extractTypeParametersMapping`),
            // so we must substitute them explicitly, exactly like `addSetterMethod` does for regular fields.
            is FirResolvedTypeRef -> coneType.typeArguments.getOrNull(index)?.type?.let(substitutor::substituteOrSelf)
                ?.toFirResolvedTypeRef()
            else -> null
        }
    }

    @OptIn(ExperimentalContracts::class)
    protected val FirDeclaration.isStaticDeclaration: Boolean
        get() {
            contract {
                returns(false) implies (this@isStaticDeclaration is FirNamedFunction)
            }
            // A function declared directly inside a companion object is the Kotlin analogue of a Java
            // static factory method, so it's treated as "static" too (e.g. for `@JvmStatic`-marking the
            // generated `builder()`), even though Kotlin never sets `FirNamedFunction.isStatic` itself.
            return this !is FirNamedFunction || isStatic || symbol.getContainingClassSymbol()?.isCompanion == true
        }
}
