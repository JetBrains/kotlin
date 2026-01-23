/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
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
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaDeclarationList
import org.jetbrains.kotlin.fir.java.javaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.AbstractBuilder
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Singular
import org.jetbrains.kotlin.lombok.k2.config.LombokService
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.k2.java.*
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.lombok.utils.capitalize
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import kotlin.collections.set
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(DirectDeclarationsAccess::class)
abstract class AbstractBuilderGenerator<T : AbstractBuilder>(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private const val TO_BUILDER = "toBuilder"
    }

    protected val lombokService: LombokService
        get() = session.lombokService

    protected val builderClassesCache: FirCache<FirClassSymbol<*>, Map<Name, FirJavaClass>?, Nothing?> =
        session.firCachesFactory.createCache(::createAndInitializeBuilders)

    private val builderWithDeclarationsCache: FirCache<FirClassSymbol<*>, List<BuilderWithDeclaration<T>>?, Nothing?> =
        session.firCachesFactory.createCache(::extractBuilderWithDeclarations)

    // Lombok doesn't add a new function if a function with the same name already exists disregarding parameters
    // It means the multimap with several functions on the same name is unnecessary
    private val functionsCache: FirCache<FirClassSymbol<*>, Map<Name, FirJavaMethod>?, Nothing?> =
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

    protected abstract fun FirJavaClassBuilder.completeBuilder(classSymbol: FirClassSymbol<*>, builderSymbol: FirClassSymbol<*>)

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (!classSymbol.isSuitableJavaClass()) return emptySet()
        return functionsCache.getValue(classSymbol)?.keys.orEmpty()
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        if (!classSymbol.isSuitableJavaClass()) return emptySet()
        val classesMap = builderClassesCache.getValue(classSymbol) ?: return emptySet()
        return classesMap.keys
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val classSymbol = context?.owner ?: return emptyList()
        return functionsCache.getValue(classSymbol)?.get(callableId.callableName)?.let { listOf(it.symbol) } ?: emptyList()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        if (!owner.isSuitableJavaClass()) return null
        return builderClassesCache.getValue(owner)?.get(name)?.symbol
    }

    private fun createFunctions(classSymbol: FirClassSymbol<*>): Map<Name, FirJavaMethod>? {
        // The same class can have both builder and entity methods in case of names clashing.
        return buildMap {
            addBuilderMethods(classSymbol)

            builderWithDeclarationsCache.getValue(classSymbol)?.let { builderWithDeclarations ->
                addEntityMethods(builderWithDeclarations, classSymbol)
            }
        }.takeIf { it.isNotEmpty() }
    }

    private fun MutableMap<Name, FirJavaMethod>.addBuilderMethods(classSymbol: FirClassSymbol<*>) {
        val containingClassSymbol = classSymbol.getContainingClassSymbol() as? FirClassSymbol<*> ?: return
        val builderWithDeclarations = builderWithDeclarationsCache.getValue(containingClassSymbol) ?: return
        val className = classSymbol.classId.shortClassName.asString()

        for ((builder, declaration) in builderWithDeclarations) {
            val containingClassBuilderName = builder.getBuilderClassShortName(declaration)
            // Make sure the current class is really a builder of the containing parent
            if (className != containingClassBuilderName) continue

            // Generate builder methods only for existing Java builder classes.
            // Otherwise, those methods are generated together with class generation (`createAndInitializeBuilders`).
            val existingJavaBuilderSymbol = session.javaSymbolProvider?.getClassLikeSymbolByClassId(classSymbol.classId)
                ?: continue
            val existingJavaBuilderFunctionNames = existingJavaBuilderSymbol.getExistingFunctionNames()

            addBuilderMethods(
                builder,
                builderDeclaration = declaration,
                builderSymbol = existingJavaBuilderSymbol,
                entitySymbol = containingClassSymbol,
                existingFunctionNames = existingJavaBuilderFunctionNames,
            )
        }
    }

    private fun MutableMap<Name, FirJavaMethod>.addEntityMethods(
        builderWithDeclarations: List<BuilderWithDeclaration<T>>,
        entitySymbol: FirClassSymbol<*>
    ) {
        for ((builder, builderDeclaration) in builderWithDeclarations) {
            val entityClassId = entitySymbol.classId
            val builderClassName = builder.getBuilderClassShortName(builderDeclaration)
            val builderClassId = entityClassId.createNestedClassId(Name.identifier(builderClassName))

            val visibility = builder.visibility.toVisibility()
            val existingFunctionNames = entitySymbol.getExistingFunctionNames()

            addIfNonClashing(Name.identifier(builder.builderMethodName), existingFunctionNames) { name ->
                val isStatic = builderDeclaration.isStaticDeclaration
                val (builderTypeRef, methodSymbol, methodTypeParameters) = constructReturnBuilderTypeAndMethodSymbol(
                    entitySymbol,
                    name,
                    builderDeclaration,
                    builderClassId
                )
                entitySymbol.createJavaMethod(
                    name,
                    valueParameters = emptyList(),
                    returnTypeRef = builderTypeRef,
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
                    val (builderTypeRef, methodSymbol, methodTypeParameters) = constructReturnBuilderTypeAndMethodSymbol(
                        entitySymbol,
                        name,
                        builderDeclaration,
                        builderClassId,
                    )
                    entitySymbol.createJavaMethod(
                        name,
                        valueParameters = emptyList(),
                        returnTypeRef = builderTypeRef,
                        visibility = visibility,
                        modality = Modality.FINAL,
                        methodSymbol = methodSymbol,
                        methodTypeParameters = methodTypeParameters,
                    )
                }
            }
        }
    }

    private data class ReturnBuilderInfo(
        val builderTypeRef: FirResolvedTypeRef,
        val methodSymbol: FirNamedFunctionSymbol,
        val methodTypeParameters: Collection<FirTypeParameter>,
    )

    @OptIn(SymbolInternals::class)
    private fun constructReturnBuilderTypeAndMethodSymbol(
        entitySymbol: FirClassSymbol<*>,
        methodName: Name,
        builderDeclaration: FirDeclaration,
        builderClassId: ClassId,
    ): ReturnBuilderInfo {
        val methodSymbol = FirNamedFunctionSymbol(CallableId(entitySymbol.classId, methodName))
        val methodTypeParameters = builderDeclaration.initializeTypeParametersMapping(methodSymbol).values

        return ReturnBuilderInfo(
            builderClassId
                .constructClassLikeType((methodTypeParameters.map { it.toConeType() } + getExtraTypeArguments()).toTypedArray())
                .toFirResolvedTypeRef(),
            methodSymbol,
            methodTypeParameters,
        )
    }

    private fun FirClassSymbol<*>.getExistingFunctionNames(): Set<Name> =
        declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>().mapTo(mutableSetOf()) { it.name }

    @OptIn(SymbolInternals::class)
    private fun createAndInitializeBuilders(classSymbol: FirClassSymbol<*>): Map<Name, FirJavaClass>? {
        val entityClass = classSymbol.fir as? FirJavaClass ?: return null
        val builderWithDeclarations = builderWithDeclarationsCache.getValue(classSymbol) ?: return null
        val builderClasses = mutableMapOf<Name, FirJavaClass>()

        for ((builder, builderDeclaration) in builderWithDeclarations) {
            val builderName = Name.identifier(builder.getBuilderClassShortName(builderDeclaration))
            val builderClassId = entityClass.classId.createNestedClassId(builderName)

            val existingJavaBuilderSymbol = session.javaSymbolProvider?.getClassLikeSymbolByClassId(builderClassId)

            // Extend existing classes using `generateFunctions` instead of generating a new class
            if (existingJavaBuilderSymbol != null) continue

            // Lombok ignores generates builder classes with the same name
            if (builderClasses.containsKey(builderName)) continue

            val visibility = builder.visibility.toVisibility()
            val builderClass = classSymbol.createEmptyBuilderClass(
                session,
                builderName,
                visibility,
                builderDeclaration,
            ) { builderSymbol ->
                object : FirJavaDeclarationList {
                    override val declarations: List<FirDeclaration> by lazy(LazyThreadSafetyMode.PUBLICATION) {
                        buildList {
                            add(builderSymbol.createDefaultJavaConstructor(visibility))
                            val builderMethods = mutableMapOf<Name, FirJavaMethod>()
                            builderMethods.addBuilderMethods(
                                builder,
                                builderDeclaration,
                                builderSymbol,
                                entityClass.symbol,
                                existingFunctionNames = emptySet()
                            )
                            addAll(builderMethods.values)
                        }
                    }
                }
            }
            if (builderClass != null) {
                builderClasses[builderName] = builderClass
            }
        }

        return builderClasses
    }

    @OptIn(SymbolInternals::class)
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

    @OptIn(SymbolInternals::class)
    private fun MutableMap<Name, FirJavaMethod>.addBuilderMethods(
        builder: T,
        builderDeclaration: FirDeclaration,
        builderSymbol: FirClassSymbol<*>,
        entitySymbol: FirClassSymbol<*>,
        existingFunctionNames: Set<Name>,
    ) {
        val entityJavaClass = entitySymbol.fir as FirJavaClass

        addSpecialBuilderMethods(builder, builderSymbol, builderDeclaration, existingFunctionNames)

        val items = when (builderDeclaration) {
            is FirJavaClass -> {
                if (entityJavaClass.isRecord) {
                    entityJavaClass.primaryConstructorIfAny(session)?.valueParameterSymbols?.map { it.fir } ?: emptyList()
                } else {
                    entityJavaClass.declarations.filterIsInstance<FirJavaField>().map { it }
                }
            }
            is FirJavaConstructor -> builderDeclaration.valueParameters
            is FirJavaMethod -> builderDeclaration.valueParameters
            else -> emptyList()
        }
        for (item in items) {
            when (val singular = lombokService.getSingular(item.symbol)) {
                null -> {
                    addSetterMethod(builder, item, builderSymbol, existingFunctionNames)
                }
                else -> {
                    addMethodsForSingularFields(builder, singular, item, builderSymbol, existingFunctionNames)
                }
            }
        }
    }

    private fun MutableMap<Name, FirJavaMethod>.addSetterMethod(
        builder: AbstractBuilder,
        item: FirVariable,
        builderSymbol: FirClassSymbol<*>,
        existingFunctionNames: Set<Name>,
    ) {
        val fieldName = item.name
        val setterName = fieldName.toMethodName(builder)
        val builderType = getBuilderType(builderSymbol) ?: return
        addIfNonClashing(setterName, existingFunctionNames) {
            builderSymbol.createJavaMethod(
                name = it,
                valueParameters = listOf(ConeLombokValueParameter(fieldName, item.returnTypeRef)),
                returnTypeRef = builderType.toFirResolvedTypeRef(),
                modality = Modality.FINAL,
                visibility = builder.visibility.toVisibility()
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
        val visibility = builder.visibility.toVisibility()

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
        declarationListProvider: (FirRegularClassSymbol) -> FirJavaDeclarationList,
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
            this.visibility = visibility
            this.modality = builderModality
            this.isStatic = builderDeclaration.isStaticDeclaration
            classKind = ClassKind.CLASS

            val typeParametersMapping = builderDeclaration.initializeTypeParametersMapping(builderSymbol)
            typeParametersMapping.mapTo(typeParameters) { it.value }
            // Remap Java type parameters to the newly created type parameters to make the Java resolve work.
            javaTypeParameterStack = MutableJavaTypeParameterStack().apply {
                for (javaTypeParam in containingClass.classJavaTypeParameterStack) {
                    addParameter(javaTypeParam.key, typeParametersMapping[javaTypeParam.value]?.symbol ?: javaTypeParam.value)
                }
            }

            scopeProvider = JavaScopeProvider
            this.superTypeRefs += superTypeRefs
            val effectiveVisibility = containingClass.effectiveVisibility.lowerBound(
                visibility.toEffectiveVisibility(this@createEmptyBuilderClass, forClass = true),
                session.typeContext
            )
            isTopLevel = false
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

            completeBuilder(this@createEmptyBuilderClass, builderSymbol)

            declarationList = declarationListProvider(symbol)
        }
    }

    /**
     * Given the following generic class with `@Builder`:
     *
     * ```kt
     * @lombok.Builder
     * public class C<T> {
     *     private final T value;
     * }
     * ```
     *
     * That has the following generated builder:
     *
     * ```kt
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
     * ```kt
     * public static <T2> CBuilder<T2> builder() {
     *     return new CBuilder<T2>();
     * }
     * ```
     *
     * And new type parameters for `CBuilder<T>` with its `build` method (T -> T3);
     *
     * ```kt
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
     * @return a map used for remapping type parameters on a Java stack
     */
    @OptIn(SymbolInternals::class)
    private fun FirDeclaration.initializeTypeParametersMapping(newContainingDeclarationSymbol: FirBasedSymbol<*>): Map<FirTypeParameterSymbol, FirTypeParameter> {
        val typeParameters: List<FirTypeParameter> = when (this) {
            is FirJavaClass -> typeParameters.map { it.symbol.fir }
            is FirJavaMethod -> typeParameters
            is FirJavaConstructor -> typeParameters.map { it.symbol.fir }
            else -> emptyList() // Use the fallback just in case, although it's normally unreachable
        }
        return buildMap {
            for (typeParameter in typeParameters) {
                this[typeParameter.symbol] = buildTypeParameterCopy(typeParameter.symbol.fir) {
                    symbol = FirTypeParameterSymbol()
                    containingDeclarationSymbol = newContainingDeclarationSymbol
                }
            }
        }
    }

    private fun T.getBuilderClassShortName(builderDeclaration: FirDeclaration): String {
        if (hasSpecifiedBuilderClassName) {
            return builderClassName
        }

        val builderClassNamePart = when (builderDeclaration) {
            is FirJavaClass -> builderDeclaration.name.asString()
            is FirJavaConstructor -> builderDeclaration.nameOrSpecialName.asString()
            is FirJavaMethod -> {
                // If the builder class name is not specified explicitly, infer the name from the method's return type
                // according to Lombok rules
                when (val returnType = (builderDeclaration.returnTypeRef as? FirJavaTypeRef)?.type) {
                    is JavaPrimitiveType -> returnType.type?.typeName?.identifier ?: "Void"
                    is JavaClassifierType -> returnType.presentableText
                    else -> returnType?.toString() ?: "" // Infer something instead of throwing an exception for unsupported types
                }
            }
            else -> {
                builderDeclaration.toString() // Normally unreachable, but infer something instead of throwing an exception
            }
        }

        return builderClassName.replace("*", builderClassNamePart)
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

fun FirClassSymbol<*>.createDefaultJavaConstructor(
    visibility: Visibility,
): FirJavaConstructor {
    val outerClassSymbol = this
    return buildJavaConstructor {
        containingClassSymbol = outerClassSymbol
        moduleData = outerClassSymbol.moduleData
        isFromSource = true
        symbol = FirConstructorSymbol(classId)
        isInner = outerClassSymbol.rawStatus.isInner
        status = FirResolvedDeclarationStatusImpl(
            visibility,
            Modality.FINAL,
            visibility.toEffectiveVisibility(outerClassSymbol)
        ).apply {
            isExpect = false
            isActual = false
            isOverride = false
            isInner = this@buildJavaConstructor.isInner
        }
        isPrimary = false
        returnTypeRef = buildResolvedTypeRef {
            coneType = outerClassSymbol.defaultType()
        }
        dispatchReceiverType = if (isInner) outerClassSymbol.defaultType() else null
        typeParameters += outerClassSymbol.typeParameterSymbols.map { buildConstructedClassTypeParameterRef { symbol = it } }
    }
}
