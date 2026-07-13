/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
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
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameterCopy
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.MutableJavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaConstructor
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeTypeMismatch
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.collectAllFunctions
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
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
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

abstract class AbstractBuilderGenerator<T : AbstractBuilder>(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private const val TO_BUILDER = "toBuilder"
    }

    protected val lombokService: LombokService
        get() = session.lombokService

    protected val builderClassesCache: FirCache<FirClassSymbol<*>, Map<Name, FirJavaClass>?, NestedClassGenerationContext> =
        session.firCachesFactory.createCache(::createAndInitializeBuilders)

    private val builderWithDeclarationsCache: FirCache<FirClassSymbol<*>, List<BuilderWithDeclaration<T>>?, Nothing?> =
        session.firCachesFactory.createCache(::extractBuilderWithDeclarations)

    // Lombok doesn't add a new function if a function with the same name already exists disregarding parameters
    // It means the multimap with several functions on the same name is unnecessary
    private val functionsCache: FirCache<FirClassSymbol<*>, Map<Name, FirJavaMethod>?, MemberGenerationContext?> =
        session.firCachesFactory.createCache(::createFunctions)

    protected abstract val builderModality: Modality

    protected abstract val annotationClassId: ClassId

    protected abstract fun getBuilder(symbol: FirBasedSymbol<*>): T?

    protected abstract fun getExtraTypeArguments(): List<ConeTypeProjection>

    protected abstract fun getBuilderType(builderSymbol: FirClassSymbol<*>): ConeKotlinType?

    protected abstract fun MutableMap<Name, FirJavaMethod>.addSpecialBuilderMethods(
        builder: T,
        builderSymbol: FirClassSymbol<*>,
        builderDeclaration: FirDeclaration,
        existingFunctionNames: Set<Name>,
    )

    protected abstract fun FirJavaClassBuilder.completeBuilder(
        classSymbol: FirClassSymbol<*>,
        builderSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    )

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (!classSymbol.isSuitableJavaClass()) return emptySet()
        return functionsCache.getValue(classSymbol, context)?.keys.orEmpty()
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        if (!classSymbol.isSuitableJavaClass()) return emptySet()
        val classesMap = builderClassesCache.getValue(classSymbol, context) ?: return emptySet()
        return classesMap.keys
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val classSymbol = context?.owner ?: return emptyList()
        return functionsCache.getValue(classSymbol, context)?.get(callableId.callableName)?.let { listOf(it.symbol) } ?: emptyList()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        if (!owner.isSuitableJavaClass()) return null
        return builderClassesCache.getValue(owner, context)?.get(name)?.symbol
    }

    private fun createFunctions(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext?): Map<Name, FirJavaMethod>? {
        // The same class can have both builder and entity methods in case of names clashing.
        return buildMap {
            addBuilderMethods(classSymbol, context)

            builderWithDeclarationsCache.getValue(classSymbol)?.let { builderWithDeclarations ->
                addEntityMethods(builderWithDeclarations, classSymbol, context)
            }
        }.takeIf { it.isNotEmpty() }
    }

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun MutableMap<Name, FirJavaMethod>.addBuilderMethods(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext?) {
        val containingClassSymbol = classSymbol.getContainingClassSymbol() as? FirClassSymbol<*> ?: return
        val builderWithDeclarations = builderWithDeclarationsCache.getValue(containingClassSymbol) ?: return
        val className = classSymbol.classId.shortClassName.asString()
        val builderFir = classSymbol.fir as? FirJavaClass
        val entityJavaClass = containingClassSymbol.fir as FirJavaClass

        val nestedClassifierScope =
            containingClassSymbol.fir.scopeProvider.getNestedClassifierScope(containingClassSymbol.fir, session, ScopeSession())
        var builderSymbolAlreadyExists = false // TODO: distinguish explicit/generated builders via origin, it's blocked by KT-79778
        nestedClassifierScope?.processClassifiersByName(classSymbol.name) {
            builderSymbolAlreadyExists = true
        }

        val existingFunctionNames = context.getExistingFunctionNames()

        for ((builder, declaration) in builderWithDeclarations) {
            val containingClassBuilderName = builder.getBuilderClassShortName(declaration)
            // Make sure the current class is really a builder of the containing parent
            if (className != containingClassBuilderName) continue

            if (builderSymbolAlreadyExists && builderFir != null) {
                // For already existing explicit builders, initialize and populate type parameters to link generated functions with them.
                // Unfortunately, we can't do it on nested classes generation step because scope are being traversed recursively (that would lead to StackOverflow)
                // For Lombok-generated builders, createEmptyBuilderClass already sets up the correct mapping
                val typeParametersMapping =
                    declaration.initializeTypeParametersMapping(newContainingDeclarationSymbol = classSymbol, existingDeclaration = true)
                for ([key, value] in typeParametersMapping) {
                    builderFir.classJavaTypeParameterStack.addParameter(key, value.symbol)
                }
            }

            addSpecialBuilderMethods(
                builder,
                builderSymbol = classSymbol,
                builderDeclaration = declaration,
                existingFunctionNames = existingFunctionNames
            )

            val items = when (declaration) {
                is FirJavaClass -> {
                    if (entityJavaClass.isRecord) {
                        entityJavaClass.primaryConstructorIfAny(session)?.valueParameterSymbols?.map { it.fir } ?: emptyList()
                    } else {
                        entityJavaClass.declarations.filterIsInstance<FirJavaField>().map { it }
                    }
                }
                is FirJavaConstructor -> declaration.valueParameters
                is FirJavaMethod -> declaration.valueParameters
                else -> emptyList()
            }
            for (item in items) {
                when (val singular = lombokService.getSingular(item.symbol)) {
                    null -> {
                        addSetterMethod(builder, item, builderSymbol = classSymbol, existingFunctionNames = existingFunctionNames)
                    }
                    else -> {
                        addMethodsForSingularFields(
                            builder,
                            singular,
                            item,
                            builderSymbol = classSymbol,
                            existingFunctionNames = existingFunctionNames
                        )
                    }
                }
            }
        }
    }

    private fun MutableMap<Name, FirJavaMethod>.addEntityMethods(
        builderWithDeclarations: List<BuilderWithDeclaration<T>>,
        entitySymbol: FirClassSymbol<*>,
        context: MemberGenerationContext?,
    ) {
        for ((val builder, val builderDeclaration = declaration) in builderWithDeclarations) {
            val visibility = builder.visibility ?: continue
            val entityClassId = entitySymbol.classId
            val builderClassName = Name.identifier(builder.getBuilderClassShortName(builderDeclaration))
            val builderClassId = entityClassId.createNestedClassId(builderClassName)

            val existingFunctionNames = context.getExistingFunctionNames()

            var existingBuilder: FirClassSymbol<*>? = null
            context?.declaredScope?.processClassifiersByName(builderClassName) {
                if (existingBuilder == null && it is FirClassSymbol<*>) {
                    existingBuilder = it
                }
            }

            fun createBuilderTypeRef(typeParameterSymbols: List<FirTypeParameterSymbol>): FirResolvedTypeRef {
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
                }.toFirResolvedTypeRef()
            }

            addIfNonClashing(Name.identifier(builder.builderMethodName), existingFunctionNames) { name ->
                val isStatic = builderDeclaration.isStaticDeclaration

                val methodSymbol = FirNamedFunctionSymbol(CallableId(entitySymbol.classId, name))
                val methodTypeParameters = builderDeclaration.initializeTypeParametersMapping(methodSymbol).values

                entitySymbol.createJavaMethod(
                    name,
                    valueParameters = emptyList(),
                    returnTypeRef = createBuilderTypeRef(methodTypeParameters.map { it.symbol }),
                    visibility = visibility,
                    modality = Modality.FINAL,
                    dispatchReceiverType = if (isStatic) null else builderDeclaration.dispatchReceiverType,
                    isStatic = isStatic,
                    methodSymbol = methodSymbol,
                    methodTypeParameters = methodTypeParameters,
                )
            }

            if (builder.requiresToBuilder) {
                addIfNonClashing(Name.identifier(TO_BUILDER), existingFunctionNames) { name ->
                    // toBuilder() is always an instance method, so the class type parameters are
                    // already provided by the dispatch receiver. The method must not introduce its
                    // own independent type parameters — otherwise call-site inference would fail.
                    entitySymbol.createJavaMethod(
                        name,
                        valueParameters = emptyList(),
                        returnTypeRef = createBuilderTypeRef(entitySymbol.typeParameterSymbols),
                        visibility = visibility,
                        modality = Modality.FINAL,
                        methodSymbol = FirNamedFunctionSymbol(CallableId(entitySymbol.classId, name)),
                    )
                }
            }
        }
    }

    private fun MemberGenerationContext?.getExistingFunctionNames(): Set<Name> = buildSet {
        this@getExistingFunctionNames?.declaredScope?.collectAllFunctions()?.mapTo(this) { it.name }
    }

    private fun createAndInitializeBuilders(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Map<Name, FirJavaClass>? {
        val builderWithDeclarations = builderWithDeclarationsCache.getValue(classSymbol) ?: return null

        @OptIn(SymbolInternals::class)
        val existingClassifiers =
            classSymbol.fir.scopeProvider.getNestedClassifierScope(classSymbol.fir, session, ScopeSession())?.getClassifierNames()
                ?: emptySet()

        return buildMap {
            for ((val builder, val builderDeclaration = declaration) in builderWithDeclarations) {
                val visibility = builder.visibility ?: continue

                val builderName = Name.identifier(builder.getBuilderClassShortName(builderDeclaration))

                // Don't generate classes if they already exist
                if (containsKey(builderName) || existingClassifiers.contains(builderName)) continue

                val builderClass = classSymbol.createEmptyBuilderClass(
                    session,
                    builderName,
                    visibility,
                    builderDeclaration,
                    context,
                )
                if (builderClass != null) {
                    this[builderName] = builderClass
                }
            }
        }.takeIf { it.isNotEmpty() }
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

    private fun MutableMap<Name, FirJavaMethod>.addSetterMethod(
        builder: AbstractBuilder,
        item: FirVariable,
        builderSymbol: FirClassSymbol<*>,
        existingFunctionNames: Set<Name>,
    ) {
        val fieldName = item.name
        val setterName = fieldName.toMethodName(builder)
        val builderType = getBuilderType(builderSymbol) ?: return
        if (builder.visibility == null) return

        addIfNonClashing(setterName, existingFunctionNames) {
            builderSymbol.createJavaMethod(
                name = it,
                valueParameters = listOf(ConeLombokValueParameter(fieldName, item.returnTypeRef)),
                returnTypeRef = builderType.toFirResolvedTypeRef(),
                modality = Modality.FINAL,
                visibility = builder.visibility
            )
        }
    }

    private fun MutableMap<Name, FirJavaMethod>.addMethodsForSingularFields(
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
                modality = Modality.FINAL,
                visibility = visibility
            )
        }

        addIfNonClashing(item.name.toMethodName(builder), existingFunctionNames) {
            builderSymbol.createJavaMethod(
                name = it,
                valueParameters = listOf(ConeLombokValueParameter(item.name, addMultipleParameterType)),
                returnTypeRef = builderType,
                modality = Modality.FINAL,
                visibility = visibility
            )
        }

        addIfNonClashing(Name.identifier("clear${item.name.identifier.capitalize()}"), existingFunctionNames) {
            builderSymbol.createJavaMethod(
                name = it,
                valueParameters = listOf(),
                returnTypeRef = builderType,
                modality = Modality.FINAL,
                visibility = visibility
            )
        }
    }

    /* Lombok doesn't add a generated method if a class already has a method with the same name.
       The number and types of parameters don't matter, see https://projectlombok.org/features/Builder#overview
       "Each listed generated element will be silently skipped if that element already exists (disregarding parameter counts and looking only at names)"
     */
    protected inline fun MutableMap<Name, FirJavaMethod>.addIfNonClashing(
        functionName: Name,
        existingFunctionNames: Set<Name>,
        createJavaMethod: (name: Name) -> FirJavaMethod
    ) {
        if (functionName !in existingFunctionNames) {
            getOrPut(functionName) { createJavaMethod(functionName) }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun FirClassSymbol<*>.createEmptyBuilderClass(
        session: FirSession,
        name: Name,
        visibility: Visibility,
        builderDeclaration: FirDeclaration,
        context: NestedClassGenerationContext,
    ): FirJavaClass? {
        val containingClass = this.fir as? FirJavaClass ?: return null
        val classId = containingClass.classId.createNestedClassId(name)
        val builderSymbol = FirRegularClassSymbol(classId)
        return buildJavaClass {
            containingClassSymbol = containingClass.symbol
            moduleData = containingClass.moduleData
            symbol = builderSymbol
            this.name = name
            isFromSource = true
            classKind = ClassKind.CLASS

            val typeParametersMapping = builderDeclaration.initializeTypeParametersMapping(builderSymbol)
            typeParametersMapping.mapTo(typeParameters) { it.value }
            // Remap Java type parameters from the containing declaration to the newly created type parameters to make the Java resolve work.
            // Don't care about outer type parameters because builder classes are always static (nested).
            javaTypeParameterStack = MutableJavaTypeParameterStack().apply {
                for ([key, value] in typeParametersMapping) {
                    addParameter(key, value.symbol)
                }
            }

            scopeProvider = JavaScopeProvider
            this.superTypeRefs += superTypeRefs
            val effectiveVisibility = containingClass.effectiveVisibility.lowerBound(
                visibility.toEffectiveVisibility(this@createEmptyBuilderClass, forClass = true),
                session.typeContext
            )
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

            completeBuilder(this@createEmptyBuilderClass, builderSymbol, context)
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
    private fun FirDeclaration.initializeTypeParametersMapping(
        newContainingDeclarationSymbol: FirBasedSymbol<*>,
        existingDeclaration: Boolean = false,
    ): Map<JavaTypeParameter, FirTypeParameter> {
        val typeParameters: List<FirTypeParameter> = when (this) {
            is FirJavaClass -> typeParameters.map { it.symbol.fir }
            is FirJavaMethod -> typeParameters
            is FirJavaConstructor -> typeParameters.map { it.symbol.fir }
            else -> emptyList() // Use the fallback just in case, although it's normally unreachable
        }
        return buildMap {
            for ([index, typeParameter] in typeParameters.withIndex()) {
                // Normally it's always `FirJavaTypeParameter` but check just in case to avoid potential exceptions.
                if (typeParameter !is FirJavaTypeParameter) continue

                this[typeParameter.javaTypeParameter] = runIf(existingDeclaration) {
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
            is FirJavaClass -> builderDeclaration.name.asString()
            is FirJavaConstructor -> builderDeclaration.nameOrSpecialName.asString()
            is FirJavaMethod -> {
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
                returns(false) implies (this@isStaticDeclaration is FirJavaMethod)
            }
            return this !is FirJavaMethod || this.isStatic
        }
}

fun JavaType.makeNullable(): JavaType = withAnnotations(annotations + NullabilityJavaAnnotation.Nullable)
fun JavaType.makeNotNullable(): JavaType = withAnnotations(annotations + NullabilityJavaAnnotation.NotNull)

