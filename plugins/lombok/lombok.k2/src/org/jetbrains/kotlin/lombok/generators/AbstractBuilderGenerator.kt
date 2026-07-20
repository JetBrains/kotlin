/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameterCopy
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.UnsafePluginApi
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.MutableJavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
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
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations.AbstractBuilder
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations.Singular
import org.jetbrains.kotlin.lombok.config.LombokService
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.lombok.java.*
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.generators.kotlin.buildJvmStaticAnnotationCallOrError
import org.jetbrains.kotlin.lombok.generators.kotlin.createConstructorIfGeneratedCompanion
import org.jetbrains.kotlin.lombok.generators.kotlin.isCompanionNeeded
import org.jetbrains.kotlin.lombok.generators.kotlin.needsConstructorIfGeneratedCompanion
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

enum class BuilderDeclarationType {
    Companion,
    Class,
    Field,
    Setter,
    Build,
    Builder,
    ToBuilder;
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
            if (isCompanionNeeded(classSymbol, context) && getBuilder(classSymbol) != null) {
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
            return getBuilder(owner)?.let { BuilderGeneratorKey(BuilderDeclarationType.Companion) }?.let { createCompanionObject(owner, it).symbol }
        }

        return builderClassesCache.getValue(BuilderKey(owner, name))
    }

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
                is FirPropertySymbol
                    -> {
                    existingVariableNames.add(it.name)
                }
            }
        }

        val generatedFunctions = mutableMapOf<Name, FirNamedFunctionSymbol>()
        val generatedVariables = mutableMapOf<Name, FirVariableSymbol<*>>()

        val classWithBuilderAnnotations = if (classSymbol.isCompanion) {
            // Builders aren't applicable to objects (including companion ones).
            // The only case when this code is executed is when a companion object with a `@JvmStatic` builder function inside is being generated.
            // It emulates static functions in Java.
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
                existingFunctionNames = existingFunctionNames
            )

            val items: List<FirVariable> = when (declaration) {
                is FirRegularClass -> {
                    val isJavaClass = entityClass is FirJavaClass
                    if (isJavaClass && entityClass.isRecord) {
                        entityClass.primaryConstructorIfAny(session)?.valueParameterSymbols?.map { it.fir } ?: emptyList()
                    } else {
                        entityClass.declarations.mapNotNull { declaration ->
                            if (isJavaClass) {
                                declaration as? FirJavaField
                            } else {
                                (declaration as? FirProperty)?.takeIf { it.hasBackingField }
                            }
                        }
                    }
                }
                is FirConstructor -> declaration.valueParameters
                is FirNamedFunction -> declaration.valueParameters
                else -> emptyList()
            }
            for (item in items) {
                generatedVariables.addIfNonClashing(item.name, existingVariableNames) {
                    if (builderSymbol.hasJavaOrigin) {
                        buildJavaField {
                            isFromSource = true
                            lazyHasConstantInitializer = lazy { false }
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
                        createMemberProperty(
                            owner = builderSymbol,
                            key = BuilderGeneratorKey(BuilderDeclarationType.Field),
                            name = it,
                            returnType = substitutor.substituteOrSelf(item.returnTypeRef.coneType),
                            isVal = false,
                        ) {
                            modality = Modality.FINAL
                            visibility = Visibilities.Private
                        }.symbol
                    }
                }

                when (val singular = lombokService.getSingular(item.symbol)) {
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
            val visibility = builder.visibility ?: continue
            val entityClassId = entitySymbol.classId
            val builderClassName = Name.identifier(builder.getBuilderClassShortName(builderDeclaration))
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
                existingBuilder = entityFir.declarations.firstIsInstanceOrNull<FirClass>()?.symbol
            }

            fun createBuilderType(typeParameterSymbols: Collection<FirTypeParameter>): ConeClassLikeType {
                val newTypeArguments = typeParameterSymbols.map { it.toConeType() } + getExtraTypeArguments()
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

            if (containingClassSymbol.hasJavaOrigin || containingClassSymbol.isCompanion || !isStaticBuilderFunction) {
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
                            key = BuilderGeneratorKey(BuilderDeclarationType.Builder),
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
                    @OptIn(SymbolInternals::class)
                    val builderType = createBuilderType(entitySymbol.typeParameterSymbols.map { it.fir })
                    if (containingClassSymbol.hasJavaOrigin) {
                        containingClassSymbol.createJavaMethod(
                            name,
                            valueParameters = emptyList(),
                            returnTypeRef = builderType.toFirResolvedTypeRef(),
                            visibility = visibility,
                            modality = Modality.OPEN,
                            methodSymbol = FirNamedFunctionSymbol(CallableId(entitySymbol.classId, name)),
                        ).symbol
                    } else {
                        createMemberFunction(
                            owner = containingClassSymbol,
                            key = BuilderGeneratorKey(BuilderDeclarationType.ToBuilder),
                            name = name,
                            returnType = builderType,
                        ) {
                            modality = Modality.OPEN
                            this.visibility = visibility
                        }.symbol
                    }
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
                if (builder.visibility == null) continue
                val builderName = Name.identifier(builder.getBuilderClassShortName(builderDeclaration))

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
            val visibility = builder.visibility ?: continue
            val builderName = Name.identifier(builder.getBuilderClassShortName(builderDeclaration))

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

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun extractBuilderWithDeclarations(classSymbol: FirClassSymbol<*>): List<BuilderWithDeclaration<T>>? {
        val annotationSymbol = annotationClassId.toSymbol(session) as? FirRegularClassSymbol ?: return emptyList()
        return buildList {
            val allowedTargets = annotationSymbol.fir.getAllowedAnnotationTargets(session)

            if (allowedTargets.contains(KotlinTarget.CLASS)) {
                getBuilder(classSymbol)?.let { add(BuilderWithDeclaration(it, classSymbol.fir)) }
            }

            for (declarationSymbol in classSymbol.declarationSymbols) {
                if (declarationSymbol is FirConstructorSymbol && allowedTargets.contains(KotlinTarget.CONSTRUCTOR) ||
                    declarationSymbol is FirFunctionSymbol<*> && allowedTargets.contains(KotlinTarget.FUNCTION)
                ) {
                    getBuilder(declarationSymbol)?.let { add(BuilderWithDeclaration(it, declarationSymbol.fir)) }
                }
            }
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
        if (builder.visibility == null) return

        addIfNonClashing(setterName, existingFunctionNames) {
            if (builderSymbol.hasJavaOrigin) {
                builderSymbol.createJavaMethod(
                    name = it,
                    valueParameters = listOf(ConeLombokValueParameter(fieldName, item.returnTypeRef)),
                    returnTypeRef = builderType.toFirResolvedTypeRef(),
                    modality = Modality.OPEN,
                    visibility = builder.visibility
                ).symbol
            } else {
                createMemberFunction(
                    owner = builderSymbol,
                    key = BuilderGeneratorKey(BuilderDeclarationType.Setter),
                    name = fieldName,
                    returnType = builderType,
                ) {
                    valueParameter(
                        name = fieldName,
                        type = substitutor.substituteOrSelf(item.returnTypeRef.coneType),
                    )
                    modality = Modality.OPEN
                    visibility = builder.visibility
                }.symbol
            }
        }
    }

    private fun MutableMap<Name, FirNamedFunctionSymbol>.addMethodsForSingularFields(
        builder: AbstractBuilder,
        singular: Singular,
        item: FirVariable,
        builderSymbol: FirClassSymbol<*>,
        existingFunctionNames: Set<Name>,
    ) {
        val fieldJavaTypeRef = item.returnTypeRef as? FirJavaTypeRef ?: return
        val javaClassifierType = fieldJavaTypeRef.type as? JavaClassifierType ?: return
        val typeName = (javaClassifierType.classifier as? JavaClass)?.fqName?.asString() ?: return

        val nameInSingularForm = (singular.singularName ?: item.name.identifier.singularForm)?.let(Name::identifier) ?: return

        val addMultipleParameterType: FirTypeRef
        val valueParameters: List<ConeLombokValueParameter>

        val fallbackParameterType = DummyJavaClassType.ObjectType.takeIf { javaClassifierType.isRaw }
        val source = builderSymbol.source?.fakeElement(KtFakeSourceElementKind.Enhancement)

        when (typeName) {
            in LombokNames.SUPPORTED_COLLECTIONS -> {
                val parameterType = javaClassifierType.parameterType(0) ?: fallbackParameterType ?: return
                valueParameters = listOf(
                    ConeLombokValueParameter(nameInSingularForm, parameterType.toRef(source))
                )

                val baseType = when (typeName) {
                    in LombokNames.SUPPORTED_GUAVA_COLLECTIONS -> JavaClasses.Iterable
                    else -> JavaClasses.Collection
                }

                addMultipleParameterType = DummyJavaClassType(baseType, typeArguments = listOf(parameterType))
                    .withProperNullability(singular.allowNull)
                    .toRef(source)
            }

            in LombokNames.SUPPORTED_MAPS -> {
                val keyType = javaClassifierType.parameterType(0) ?: fallbackParameterType ?: return
                val valueType = javaClassifierType.parameterType(1) ?: fallbackParameterType ?: return
                valueParameters = listOf(
                    ConeLombokValueParameter(Name.identifier("key"), keyType.toRef(source)),
                    ConeLombokValueParameter(Name.identifier("value"), valueType.toRef(source)),
                )

                addMultipleParameterType = DummyJavaClassType(JavaClasses.Map, typeArguments = listOf(keyType, valueType))
                    .withProperNullability(singular.allowNull)
                    .toRef(source)
            }

            in LombokNames.SUPPORTED_TABLES -> {
                val rowKeyType = javaClassifierType.parameterType(0) ?: fallbackParameterType ?: return
                val columnKeyType = javaClassifierType.parameterType(1) ?: fallbackParameterType ?: return
                val valueType = javaClassifierType.parameterType(2) ?: fallbackParameterType ?: return

                valueParameters = listOf(
                    ConeLombokValueParameter(Name.identifier("rowKey"), rowKeyType.toRef(source)),
                    ConeLombokValueParameter(Name.identifier("columnKey"), columnKeyType.toRef(source)),
                    ConeLombokValueParameter(Name.identifier("value"), valueType.toRef(source)),
                )

                addMultipleParameterType = DummyJavaClassType(
                    JavaClasses.Table,
                    typeArguments = listOf(rowKeyType, columnKeyType, valueType)
                ).withProperNullability(singular.allowNull).toRef(source)
            }

            else -> return
        }

        val builderType = getBuilderType(builderSymbol)?.toFirResolvedTypeRef() ?: return
        val visibility = builder.visibility ?: return

        addIfNonClashing(nameInSingularForm.toMethodName(builder), existingFunctionNames) {
            builderSymbol.createJavaMethod(
                name = it,
                valueParameters,
                returnTypeRef = builderType,
                modality = Modality.OPEN,
                visibility = visibility
            ).symbol
        }

        addIfNonClashing(item.name.toMethodName(builder), existingFunctionNames) {
            builderSymbol.createJavaMethod(
                name = it,
                valueParameters = listOf(ConeLombokValueParameter(item.name, addMultipleParameterType)),
                returnTypeRef = builderType,
                modality = Modality.OPEN,
                visibility = visibility
            ).symbol
        }

        addIfNonClashing(Name.identifier("clear${item.name.identifier.capitalize()}"), existingFunctionNames) {
            builderSymbol.createJavaMethod(
                name = it,
                valueParameters = listOf(),
                returnTypeRef = builderType,
                modality = Modality.OPEN,
                visibility = visibility
            ).symbol
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
            getOrPut(name) { createCallable(name) }
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
                origin = FirDeclarationOrigin.Plugin(BuilderGeneratorKey(BuilderDeclarationType.Class))
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
                this.isInner = false // Builders are always nested classes
                isCompanion = false
                isData = false
                isInline = false
                isFun = classKind == ClassKind.INTERFACE
            }

            typeParametersMapping.mapTo(typeParameters) { it.value }

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

    private fun T.getBuilderClassShortName(builderDeclaration: FirDeclaration): String {
        val refinedBuilderClassName = builderClassName ?: session.lombokService.config.builderClassName

        if (hasSpecifiedBuilderClassName) {
            return refinedBuilderClassName
        }

        val builderClassNamePart = when (builderDeclaration) {
            is FirRegularClass -> builderDeclaration.name.asString()
            is FirConstructor -> builderDeclaration.nameOrSpecialName.asString()
            is FirNamedFunction -> {
                // If the builder class name is not specified explicitly, infer the name from the method's return type
                // according to Lombok rules
                when (val returnType = (builderDeclaration.returnTypeRef as? FirJavaTypeRef)?.type) {
                    is JavaPrimitiveType -> returnType.type?.typeName?.identifier ?: "Void"
                    is JavaClassifierType -> returnType.classifier?.name?.asString() ?: returnType.presentableText
                    else -> returnType?.toString() ?: "" // Infer something instead of throwing an exception for unsupported types
                }
            }
            else -> {
                builderDeclaration.toString() // Normally unreachable, but infer something instead of throwing an exception
            }
        }

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

    private val String.singularForm: String?
        get() = StringUtil.unpluralize(this)

    private fun JavaClassifierType.parameterType(index: Int): JavaType? {
        return typeArguments.getOrNull(index)
    }

    private fun JavaType.withProperNullability(allowNull: Boolean): JavaType {
        return if (allowNull) makeNullable() else makeNotNullable()
    }

    @OptIn(ExperimentalContracts::class)
    protected val FirDeclaration.isStaticDeclaration: Boolean
        get() {
            contract {
                returns(false) implies (this@isStaticDeclaration is FirNamedFunction)
            }
            return this !is FirNamedFunction || this.isStatic
        }
}

fun JavaType.makeNullable(): JavaType = withAnnotations(annotations + NullabilityJavaAnnotation.Nullable)
fun JavaType.makeNotNullable(): JavaType = withAnnotations(annotations + NullabilityJavaAnnotation.NotNull)
