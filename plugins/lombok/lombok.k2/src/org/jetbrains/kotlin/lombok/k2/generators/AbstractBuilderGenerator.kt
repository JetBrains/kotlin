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
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaDeclarationList
import org.jetbrains.kotlin.fir.java.javaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
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

    protected abstract fun constructBuilderType(builderClassId: ClassId): ConeClassLikeType

    protected abstract fun getBuilderType(builderSymbol: FirClassSymbol<*>): ConeKotlinType?

    protected abstract fun MutableMap<Name, FirJavaMethod>.addSpecialBuilderMethods(
        builder: T,
        classSymbol: FirClassSymbol<*>,
        builderSymbol: FirClassSymbol<*>,
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
        val containingClassName = containingClassSymbol.classId.shortClassName
        val className = classSymbol.classId.shortClassName.asString()

        for ((builder, declaration) in builderWithDeclarations) {
            val containingClassBuilderName = builder.getBuilderClassShortName(containingClassName)
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
        for ((builder, _) in builderWithDeclarations) {
            val entityClassId = entitySymbol.classId
            val builderClassName = builder.getBuilderClassShortName(entityClassId.shortClassName)
            val builderClassId = entityClassId.createNestedClassId(Name.identifier(builderClassName))

            val builderTypeRef = constructBuilderType(builderClassId).toFirResolvedTypeRef()
            val visibility = builder.visibility.toVisibility()
            val existingFunctionNames = entitySymbol.getExistingFunctionNames()

            addIfNonClashing(Name.identifier(builder.builderMethodName), existingFunctionNames) {
                entitySymbol.createJavaMethod(
                    it,
                    valueParameters = emptyList(),
                    returnTypeRef = builderTypeRef,
                    visibility = visibility,
                    modality = Modality.FINAL,
                    dispatchReceiverType = null,
                    isStatic = true
                )
            }

            if (builder.requiresToBuilder) {
                addIfNonClashing(Name.identifier(TO_BUILDER), existingFunctionNames) {
                    entitySymbol.createJavaMethod(
                        it,
                        valueParameters = emptyList(),
                        returnTypeRef = builderTypeRef,
                        visibility = visibility,
                        modality = Modality.FINAL,
                    )
                }
            }
        }
    }

    private fun FirClassSymbol<*>.getExistingFunctionNames(): Set<Name> =
        declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>().mapTo(mutableSetOf()) { it.name }

    @OptIn(SymbolInternals::class)
    private fun createAndInitializeBuilders(classSymbol: FirClassSymbol<*>): Map<Name, FirJavaClass>? {
        val entityClass = classSymbol.fir as? FirJavaClass ?: return null
        val builderWithDeclarations = builderWithDeclarationsCache.getValue(classSymbol) ?: return null
        val builderClasses = mutableMapOf<Name, FirJavaClass>()

        for ((builder, builderDeclaration) in builderWithDeclarations) {
            val builderName = Name.identifier(builder.getBuilderClassShortName(classSymbol.name))
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
                visibility
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
        return buildList {
            val annotationSymbol = annotationClassId.toSymbol(session) as FirRegularClassSymbol
            val allowedTargets = annotationSymbol.fir.getAllowedAnnotationTargets(session)

            if (allowedTargets.contains(KotlinTarget.CLASS)) {
                getBuilder(classSymbol)?.let { add(BuilderWithDeclaration(it, classSymbol.fir)) }
            }

            for (declarationSymbol in classSymbol.declarationSymbols) {
                // TODO: add support for methods KT-71893
                if (declarationSymbol is FirConstructorSymbol && allowedTargets.contains(KotlinTarget.CONSTRUCTOR)) {
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

        addSpecialBuilderMethods(builder, entitySymbol, builderSymbol, existingFunctionNames)

        val items = when (builderDeclaration) {
            is FirClassLikeDeclaration -> entityJavaClass.declarations.filterIsInstance<FirJavaField>().map { it }
            is FirConstructor -> builderDeclaration.valueParameters
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
            this.isStatic = true
            classKind = ClassKind.CLASS
            javaTypeParameterStack = containingClass.classJavaTypeParameterStack
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
                this.isInner = !isTopLevel && !this@buildJavaClass.isStatic
                isCompanion = false
                isData = false
                isInline = false
                isFun = classKind == ClassKind.INTERFACE
            }

            declarationList = declarationListProvider(symbol)

            completeBuilder(this@createEmptyBuilderClass, builderSymbol)
        }
    }

    private fun T.getBuilderClassShortName(className: Name): String =
        builderClassName.replace("*", className.asString())

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
}

fun JavaType.makeNullable(): JavaType = withAnnotations(annotations + NullabilityJavaAnnotation.Nullable)
fun JavaType.makeNotNullable(): JavaType = withAnnotations(annotations + NullabilityJavaAnnotation.NotNull)

fun FirClassSymbol<*>.createJavaMethod(
    name: Name,
    valueParameters: List<ConeLombokValueParameter>,
    returnTypeRef: FirTypeRef,
    visibility: Visibility,
    modality: Modality,
    dispatchReceiverType: ConeSimpleKotlinType? = this.defaultType(),
    isStatic: Boolean = false,
): FirJavaMethod {
    return buildJavaMethod {
        containingClassSymbol = this@createJavaMethod
        moduleData = this@createJavaMethod.moduleData
        this.returnTypeRef = returnTypeRef
        this.dispatchReceiverType = dispatchReceiverType
        this.name = name
        symbol = FirNamedFunctionSymbol(CallableId(classId, name))
        status = FirResolvedDeclarationStatusImpl(visibility, modality, visibility.toEffectiveVisibility(this@createJavaMethod)).apply {
            this.isStatic = isStatic
        }
        isFromSource = true
        for (valueParameter in valueParameters) {
            this.valueParameters += buildJavaValueParameter {
                moduleData = this@createJavaMethod.moduleData
                this.returnTypeRef = valueParameter.typeRef
                containingDeclarationSymbol = this@buildJavaMethod.symbol
                this.name = valueParameter.name
                isVararg = false
                isFromSource = true
            }
        }
    }.apply {
        if (isStatic) {
            containingClassForStaticMemberAttr = this@createJavaMethod.toLookupTag()
        }
    }
}

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

class ConeLombokValueParameter(val name: Name, val typeRef: FirTypeRef)
