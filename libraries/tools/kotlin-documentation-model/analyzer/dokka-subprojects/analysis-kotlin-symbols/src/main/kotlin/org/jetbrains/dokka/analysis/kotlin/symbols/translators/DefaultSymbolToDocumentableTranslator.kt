/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators


import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.*
import com.intellij.psi.util.PsiLiteralUtil
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.java.JavaAnalysisPlugin
import org.jetbrains.dokka.analysis.java.parsers.JavadocParser
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.getGeneratedKDocDocumentationFrom
import org.jetbrains.dokka.analysis.kotlin.symbols.services.KtPsiDocumentableSource
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.getJavaDocDocumentationFrom
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.getKDocDocumentationFrom
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.hasGeneratedKDocDocumentation
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.AnnotationTranslator.Companion.getPresentableName
import org.jetbrains.dokka.analysis.kotlin.symbols.utils.typeConstructorsBeingExceptions
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Visibility
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.sources.AsyncSourceToDocumentableTranslator
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
import org.jetbrains.kotlin.psi.*

internal class DefaultSymbolToDocumentableTranslator(context: DokkaContext) : AsyncSourceToDocumentableTranslator {
    private val kotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }
    private val javadocParser = JavadocParser(
        docCommentParsers = context.plugin<JavaAnalysisPlugin>().query { docCommentParsers },
        docCommentFinder = context.plugin<JavaAnalysisPlugin>().docCommentFinder
    )

    override suspend fun invokeSuspending(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        context: DokkaContext
    ): DModule {
        val analysisContext = kotlinAnalysis
        @Suppress("unused")
        return DokkaSymbolVisitor(
            sourceSet = sourceSet,
            moduleName = context.configuration.moduleName,
            analysisContext = analysisContext,
            logger = context.logger,
            javadocParser = if(sourceSet.analysisPlatform == Platform.jvm) javadocParser else null
        ).visitModule()
    }
}

internal fun <T : Bound> T.wrapWithVariance(variance: org.jetbrains.kotlin.types.Variance) =
    when (variance) {
        org.jetbrains.kotlin.types.Variance.INVARIANT -> Invariance(this)
        org.jetbrains.kotlin.types.Variance.IN_VARIANCE -> Contravariance(this)
        org.jetbrains.kotlin.types.Variance.OUT_VARIANCE -> Covariance(this)
    }

/**
 * Maps [KaSymbol] to Documentable model [Documentable]
 *
 * @param javadocParser can be null for non JVM platform
 */
internal class DokkaSymbolVisitor(
    private val sourceSet: DokkaConfiguration.DokkaSourceSet,
    private val moduleName: String,
    private val analysisContext: KotlinAnalysis,
    private val logger: DokkaLogger,
    private val javadocParser: JavadocParser? = null
) {
    private val annotationTranslator = AnnotationTranslator()
    private val typeTranslator = TypeTranslator(sourceSet, annotationTranslator)

    private fun <T> T.toSourceSetDependent() = if (this != null) mapOf(sourceSet to this) else emptyMap()

    private fun <T : KaSymbol> Sequence<T>.filterSymbolsInSourceSet(moduleFiles: Set<KtFile>): Sequence<T> = filter {
        when (val file = it.psi?.containingFile) {
            is KtFile -> moduleFiles.contains(file)
            else -> false
        }
    }

    fun visitModule(): DModule {
        val sourceModule = analysisContext.getModule(sourceSet)
        val ktFiles = analysisContext.modulesWithFiles[sourceModule]?.filterIsInstance<KtFile>()?.toSet()
            ?: throw IllegalStateException("No source files for a source module ${sourceModule.name} of source set ${sourceSet.sourceSetID}")
        val processedPackages: MutableSet<FqName> = mutableSetOf()
        return analyze(sourceModule) {
            val packages = ktFiles.mapNotNull { file ->
                if (processedPackages.contains(file.packageFqName))
                    return@mapNotNull null
                processedPackages.add(file.packageFqName)
                findPackage(file.packageFqName)?.let { packageSymbol ->
                    visitPackageSymbol(packageSymbol, ktFiles)
                }
            }

            DModule(
                name = moduleName,
                packages = packages,
                documentation = emptyMap(),
                expectPresentInSet = null,
                sourceSets = setOf(sourceSet)
            )
        }
    }

    private fun KaSession.visitPackageSymbol(
        packageSymbol: KaPackageSymbol,
        moduleFiles: Set<KtFile>
    ): DPackage {
        val dri = getDRIFromPackage(packageSymbol)
        val scope = packageSymbol.packageScope
        val callables = scope.callables.filterSymbolsInSourceSet(moduleFiles).toList()
        val classifiers = scope.classifiers.filterSymbolsInSourceSet(moduleFiles).toList()

        val functions = callables.filterIsInstance<KaNamedFunctionSymbol>().map { visitFunctionSymbol(it, dri) }
        val properties = callables.filterIsInstance<KaPropertySymbol>().map { visitPropertySymbol(it, dri) }
        val classlikes =
            classifiers.filterIsInstance<KaNamedClassSymbol>()
                .map { visitClassSymbol(it, dri) }
        val typealiases = classifiers.filterIsInstance<KaTypeAliasSymbol>().map { visitTypeAliasSymbol(it, dri) }

        return DPackage(
            dri = dri,
            functions = functions,
            properties = properties,
            classlikes = classlikes,
            typealiases = typealiases,
            documentation = emptyMap(),
            sourceSets = setOf(sourceSet)
        )
    }

    private fun KaSession.visitTypeAliasSymbol(
        typeAliasSymbol: KaTypeAliasSymbol,
        parent: DRI
    ): DTypeAlias = withExceptionCatcher(typeAliasSymbol) {
        val name = typeAliasSymbol.name.asString()
        val dri = parent.withClass(name)

        val ancestryInfo = with(typeTranslator) { buildAncestryInformationFrom(typeAliasSymbol.expandedType) }

        val generics =
            typeAliasSymbol.typeParameters.mapIndexed { index, symbol -> visitVariantTypeParameter(index, symbol, dri) }

        return DTypeAlias(
            dri = dri,
            name = name,
            type = GenericTypeConstructor(
                dri = dri,
                projections = generics.map { it.variantTypeParameter }), // this property can be removed in DTypeAlias
            expectPresentInSet = null,
            underlyingType = toBoundFrom(typeAliasSymbol.expandedType).toSourceSetDependent(),
            visibility = typeAliasSymbol.getDokkaVisibility().toSourceSetDependent(),
            documentation = getDocumentation(typeAliasSymbol)?.toSourceSetDependent() ?: emptyMap(),
            sourceSets = setOf(sourceSet),
            generics = generics,
            sources = getSource(typeAliasSymbol),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(typeAliasSymbol)?.toSourceSetDependent()?.toAnnotations(),
                ancestryInfo.exceptionInSupertypesOrNull(),
            )
        )
    }

    fun KaSession.visitClassSymbol(
        namedClassSymbol: KaNamedClassSymbol,
        parent: DRI
    ): DClasslike = withExceptionCatcher(namedClassSymbol) {
        val name = namedClassSymbol.name.asString()
        val dri = parent.withClass(name)

        val isExpect = namedClassSymbol.isExpect
        val isActual = namedClassSymbol.isActual
        val documentation = getDocumentation(namedClassSymbol)?.toSourceSetDependent() ?: emptyMap()

        val (constructors, functions, properties, classlikesWithoutCompanion, typeAliases) = getDokkaScopeFrom(namedClassSymbol, dri)

        val companionObject = namedClassSymbol.companionObject?.let {
            visitClassSymbol(
                it,
                dri
            )
        } as? DObject
        val classlikes = if (companionObject == null) classlikesWithoutCompanion else classlikesWithoutCompanion + companionObject

        val generics = namedClassSymbol.typeParameters.mapIndexed { index, symbol ->
            visitVariantTypeParameter(
                index,
                symbol,
                dri
            )
        }

        val ancestryInfo =
            with(typeTranslator) { buildAncestryInformationFrom(namedClassSymbol.defaultType) }
        val supertypes =
            //(ancestryInfo.interfaces.map{ it.typeConstructor } + listOfNotNull(ancestryInfo.superclass?.typeConstructor))
            namedClassSymbol.superTypes.filterNot { it.isAnyType }
                .map { with(typeTranslator) { toTypeConstructorWithKindFrom(it) } }
                .toSourceSetDependent()
        return@withExceptionCatcher when (namedClassSymbol.classKind) {
            KaClassKind.OBJECT, KaClassKind.COMPANION_OBJECT ->
                DObject(
                    dri = dri,
                    name = name,
                    functions = functions,
                    properties = properties,
                    classlikes = classlikes,
                    typealiases = typeAliases,
                    sources = getSource(namedClassSymbol),
                    expectPresentInSet = sourceSet.takeIf { isExpect },
                    visibility = namedClassSymbol.getDokkaVisibility().toSourceSetDependent(),
                    supertypes = supertypes,
                    documentation = documentation,
                    sourceSets = setOf(sourceSet),
                    isExpectActual = (isExpect || isActual),
                    extra = PropertyContainer.withAll(
                        namedClassSymbol.additionalExtras()?.toSourceSetDependent()
                            ?.toAdditionalModifiers(),
                        getDokkaAnnotationsFrom(namedClassSymbol)?.toSourceSetDependent()?.toAnnotations(),
                        ImplementedInterfaces(ancestryInfo.allImplementedInterfaces().toSourceSetDependent()),
                        ancestryInfo.exceptionInSupertypesOrNull()
                    )
                )

            KaClassKind.CLASS -> DClass(
                dri = dri,
                name = name,
                constructors = constructors,
                functions = functions,
                properties = properties,
                classlikes = classlikes,
                typealiases = typeAliases,
                sources = getSource(namedClassSymbol),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                visibility = namedClassSymbol.getDokkaVisibility().toSourceSetDependent(),
                supertypes = supertypes,
                generics = generics,
                documentation = documentation,
                modifier = namedClassSymbol.getDokkaModality().toSourceSetDependent(),
                companion = companionObject,
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    namedClassSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(namedClassSymbol)?.toSourceSetDependent()?.toAnnotations(),
                    ImplementedInterfaces(ancestryInfo.allImplementedInterfaces().toSourceSetDependent()),
                    ancestryInfo.exceptionInSupertypesOrNull()
                )
            )

            KaClassKind.INTERFACE -> DInterface(
                dri = dri,
                name = name,
                functions = functions,
                properties = properties,
                classlikes = classlikes,
                typealiases = typeAliases,
                sources = getSource(namedClassSymbol),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                visibility = namedClassSymbol.getDokkaVisibility().toSourceSetDependent(),
                supertypes = supertypes,
                generics = generics,
                documentation = documentation,
                companion = companionObject,
                modifier = namedClassSymbol.getDokkaModality().toSourceSetDependent(),
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    namedClassSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(namedClassSymbol)?.toSourceSetDependent()?.toAnnotations(),
                    ImplementedInterfaces(ancestryInfo.allImplementedInterfaces().toSourceSetDependent()),
                    ancestryInfo.exceptionInSupertypesOrNull()
                )
            )

            KaClassKind.ANNOTATION_CLASS -> DAnnotation(
                dri = dri,
                name = name,
                documentation = documentation,
                functions = functions,
                properties = properties,
                classlikes = classlikes,
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                companion = companionObject,
                visibility = namedClassSymbol.getDokkaVisibility().toSourceSetDependent(),
                generics = generics,
                constructors = constructors,
                sources = getSource(namedClassSymbol),
                extra = PropertyContainer.withAll(
                    namedClassSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(namedClassSymbol)?.toSourceSetDependent()?.toAnnotations(),
                )
            )

            KaClassKind.ANONYMOUS_OBJECT -> throw NotImplementedError("ANONYMOUS_OBJECT does not support")
            KaClassKind.ENUM_CLASS -> {
                /**
                 * See https://github.com/Kotlin/dokka/issues/3129
                 *
                 *  e.g. the `A` enum entry in the `enum E` is
                 * ```
                 * static val A: E = object : E() {
                 *    val x: Int = 5
                 * }
                 * ```
                 * it needs to exclude all static members like `values` and `valueOf` from the enum class's scope
                 */
                val enumEntryScope = lazy {
                    getDokkaScopeFrom(namedClassSymbol, dri, includeStaticScope = false).let {
                        it.copy(
                            functions = it.functions.map { it.withNewExtras( it.extra + InheritedMember(dri.copy(callable = null).toSourceSetDependent())) },
                            properties = it.properties.map { it.withNewExtras( it.extra + InheritedMember(dri.copy(callable = null).toSourceSetDependent())) }
                        )
                    }
                }

                val entries =
                    namedClassSymbol.staticDeclaredMemberScope.callables.filterIsInstance<KaEnumEntrySymbol>().map {
                        visitEnumEntrySymbol(it, enumEntryScope.value)
                    }.toList()

                DEnum(
                    dri = dri,
                    name = name,
                    entries = entries,
                    constructors = constructors,
                    functions = functions,
                    properties = properties,
                    classlikes = classlikes,
                    typealiases = typeAliases,
                    sources = getSource(namedClassSymbol),
                    expectPresentInSet = sourceSet.takeIf { isExpect },
                    visibility = namedClassSymbol.getDokkaVisibility().toSourceSetDependent(),
                    supertypes = supertypes,
                    documentation = documentation,
                    companion = namedClassSymbol.companionObject?.let {
                        visitClassSymbol(
                            it,
                            dri
                        )
                    } as? DObject,
                    sourceSets = setOf(sourceSet),
                    isExpectActual = (isExpect || isActual),
                    extra = PropertyContainer.withAll(
                        namedClassSymbol.additionalExtras()?.toSourceSetDependent()
                            ?.toAdditionalModifiers(),
                        getDokkaAnnotationsFrom(namedClassSymbol)?.toSourceSetDependent()?.toAnnotations(),
                        ImplementedInterfaces(ancestryInfo.allImplementedInterfaces().toSourceSetDependent())
                    )
                )
            }
        }
    }

    private data class DokkaScope(
        val constructors: List<DFunction>,
        val functions: List<DFunction>,
        val properties: List<DProperty>,
        val classlikesWithoutCompanion: List<DClasslike>,
        val typeAliases: List<DTypeAlias>,
    )

    /**
     * @return a scope [DokkaScope] consisting of:
     * - primary and secondary constructors
     * - member functions, including inherited ones
     * - member properties, including inherited ones and synthetic java properties
     * - classlikes (classes and objects **except a companion**) that are explicitly declared in [namedClassOrObjectSymbol]
     *   only if [includeStaticScope] is enabled
     *
     * @param includeStaticScope a flag to add static members, e.g. `valueOf`, `values` and `entries` members for Enum.
     * See [org.jetbrains.kotlin.analysis.api.components.KaScopeProvider.staticDeclaredMemberScope] for what a static scope is.
     */
    @OptIn(KaExperimentalApi::class) // due to getSyntheticJavaPropertiesScope
    private fun KaSession.getDokkaScopeFrom(
        namedClassOrObjectSymbol: KaNamedClassSymbol,
        dri: DRI,
        includeStaticScope: Boolean = true
    ): DokkaScope {
        // getCombinedMemberScope additionally includes a static scope, see [getCombinedMemberScope]
        // e.g. getStaticMemberScope contains `valueOf`, `values` and `entries` members for Enum
        val scope = if(includeStaticScope) namedClassOrObjectSymbol.combinedMemberScope else namedClassOrObjectSymbol.memberScope
        val constructors = scope.constructors.map { visitConstructorSymbol(it) }.toList()

        val callables = scope.callables.toList()

        // Dokka K1 does not show inherited nested and inner classes,
        // so it should show only classifiers (classes and objects) explicitly declared
        val classifiers = when {
            includeStaticScope -> namedClassOrObjectSymbol.staticMemberScope.classifiers.toList()
            else -> emptyList()
        }

        val syntheticJavaProperties =
            namedClassOrObjectSymbol.defaultType.syntheticJavaPropertiesScope?.getCallableSignatures()
                ?.map { it.symbol }
                ?.filterIsInstance<KaSyntheticJavaPropertySymbol>()
                ?.toList()
                .orEmpty()

        fun List<KaJavaFieldSymbol>.filterOutSyntheticJavaPropBackingField() =
            filterNot { javaField -> syntheticJavaProperties.any { it.hasBackingField && javaField.name == it.name } }

        val javaFields = callables.filterIsInstance<KaJavaFieldSymbol>()
            .filterOutSyntheticJavaPropBackingField()

        fun List<KaNamedFunctionSymbol>.filterOutSyntheticJavaPropAccessors() = filterNot { fn ->
            if ((fn.origin == KaSymbolOrigin.JAVA_SOURCE || fn.origin == KaSymbolOrigin.JAVA_LIBRARY) && fn.callableId != null)
                syntheticJavaProperties.any { fn.callableId == it.javaGetterSymbol.callableId || fn.callableId == it.javaSetterSymbol?.callableId }
            else false
        }

        val functions = callables.filterIsInstance<KaNamedFunctionSymbol>()
            .filterOutSyntheticJavaPropAccessors().map { visitFunctionSymbol(it, dri) }


        val properties = callables.filterIsInstance<KaPropertySymbol>().map { visitPropertySymbol(it, dri) } +
                syntheticJavaProperties.map { visitPropertySymbol(it, dri) } +
                javaFields.map { visitJavaFieldSymbol(it, dri) }

        val typealiases = classifiers.filterIsInstance<KaTypeAliasSymbol>()
            .map { visitTypeAliasSymbol(it, dri) }

        fun List<KaNamedClassSymbol>.filterOutCompanion() =
                filterNot {
                    it.classKind == KaClassKind.COMPANION_OBJECT
                }

        val classlikes = classifiers.filterIsInstance<KaNamedClassSymbol>()
            .filterOutCompanion()
            .map { visitClassSymbol(it, dri) }

        return DokkaScope(
            constructors = constructors,
            functions = functions,
            properties = properties,
            classlikesWithoutCompanion = classlikes,
            typeAliases = typealiases,
        )
    }

    private fun KaSession.visitEnumEntrySymbol(
        enumEntrySymbol: KaEnumEntrySymbol, scope: DokkaScope
    ): DEnumEntry = withExceptionCatcher(enumEntrySymbol) {
        val dri = getDRIFromEnumEntry(enumEntrySymbol)
        val isExpect = false

        return DEnumEntry(
            dri = dri,
            name = enumEntrySymbol.name.asString(),
            documentation = getDocumentation(enumEntrySymbol)?.toSourceSetDependent() ?: emptyMap(),
            functions = scope.functions,
            properties = scope.properties,
            classlikes = emptyList(), // always empty, see https://github.com/Kotlin/dokka/issues/3129
            sourceSets = setOf(sourceSet),
            expectPresentInSet = sourceSet.takeIf { isExpect },
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(enumEntrySymbol)?.toSourceSetDependent()?.toAnnotations()
            )
        )
    }

    private fun KaSession.visitPropertySymbol(propertySymbol: KaPropertySymbol, parent: DRI): DProperty =
        withExceptionCatcher(propertySymbol) {
            val dri = createDRIWithOverridden(propertySymbol).origin
            val inheritedFrom = dri.getInheritedFromDRI(parent)
            val (isExpect, isActual) = when (propertySymbol) {
                is KaKotlinPropertySymbol -> propertySymbol.isExpect to propertySymbol.isActual
                is KaSyntheticJavaPropertySymbol -> false to false
            }
            val generics =
                propertySymbol.typeParameters.mapIndexed { index, symbol ->
                    visitVariantTypeParameter(
                        index,
                        symbol,
                        dri
                    )
                }

            return DProperty(
                dri = dri,
                name = propertySymbol.name.asString(),
                receiver = propertySymbol.receiverParameter?.let {
                    visitReceiverParameter(
                        it,
                        dri
                    )
                },
                contextParameters = @OptIn(KaExperimentalApi::class) propertySymbol.contextParameters
                    .mapIndexed { index, symbol -> visitContextParameter(index, symbol, dri) },
                sources = getSource(propertySymbol),
                getter = propertySymbol.getter?.let { visitPropertyAccessor(it, propertySymbol, dri, parent) },
                setter = propertySymbol.setter?.let { visitPropertyAccessor(it, propertySymbol, dri, parent) },
                visibility = propertySymbol.visibility.toDokkaVisibility().toSourceSetDependent(),
                documentation = getDocumentation(propertySymbol)?.toSourceSetDependent() ?: emptyMap(), // TODO
                modifier = propertySymbol.getDokkaModality().toSourceSetDependent(),
                type = toBoundFrom(propertySymbol.returnType),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sourceSets = setOf(sourceSet),
                generics = generics,
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    additionalExtrasOfProperty(propertySymbol)?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(propertySymbol)?.toSourceSetDependent()?.toAnnotations(),
                    propertySymbol.getDefaultValue()?.let { DefaultValue(it.toSourceSetDependent()) },
                    inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                    takeUnless { propertySymbol.isVal }?.let { IsVar },
                    takeIf { propertySymbol.isFromPrimaryConstructor &&
                            // a property can be from a constructor of a super class
                            inheritedFrom == null }?.let {

                        IsAlsoParameter(listOf(sourceSet))
                    }
                )
            )
        }

    private fun KaSession.visitJavaFieldSymbol(
        javaFieldSymbol: KaJavaFieldSymbol,
        parent: DRI
    ): DProperty =
        withExceptionCatcher(javaFieldSymbol) {
            val dri = createDRIWithOverridden(javaFieldSymbol).origin
            val inheritedFrom = dri.getInheritedFromDRI(parent)
            val isExpect = false
            val isActual = false

            return DProperty(
                dri = dri,
                name = javaFieldSymbol.name.asString(),
                receiver = javaFieldSymbol.receiverParameter?.let {
                    visitReceiverParameter(
                        it,
                        dri
                    )
                },
                sources = getSource(javaFieldSymbol),
                getter = null,
                setter = null,
                visibility = javaFieldSymbol.getDokkaVisibility().toSourceSetDependent(),
                documentation = getDocumentation(javaFieldSymbol)?.toSourceSetDependent() ?: emptyMap(), // TODO
                modifier = javaFieldSymbol.getDokkaModality().toSourceSetDependent(),
                type = toBoundFrom(javaFieldSymbol.returnType),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sourceSets = setOf(sourceSet),
                generics = emptyList(),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    javaFieldSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(javaFieldSymbol)?.toSourceSetDependent()?.toAnnotations(),
                    //javaFieldSymbol.getDefaultValue()?.let { DefaultValue(it.toSourceSetDependent()) },
                    inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                    // non-final java property should be var
                    takeUnless { javaFieldSymbol.isVal }?.let { IsVar }
                )
            )
        }

    private fun KaSession.visitPropertyAccessor(
        propertyAccessorSymbol: KaPropertyAccessorSymbol,
        propertySymbol: KaPropertySymbol,
        propertyDRI: DRI,
        propertyParentDRI: DRI
    ): DFunction = withExceptionCatcher(propertyAccessorSymbol) {
        val isGetter = propertyAccessorSymbol is KaPropertyGetterSymbol
        // it also covers @JvmName annotation
        @OptIn(KaExperimentalApi::class) // due to javaGetterName/javaSetterName
        val name = (if (isGetter) propertySymbol.javaGetterName else propertySymbol.javaSetterName)?.asString() ?: ""

        // SyntheticJavaProperty has callableId, propertyAccessorSymbol.origin = JAVA_SYNTHETIC_PROPERTY
        // For Kotlin properties callableId=null
        val dri = if (propertyAccessorSymbol.callableId != null)
            getDRIFromFunction(propertyAccessorSymbol)
        else
            propertyDRI.copy(
                callable = propertyDRI.callable?.copy(
                    name = name,
                    params = propertyAccessorSymbol.valueParameters.map { getTypeReferenceFrom(it.returnType) },
                    isProperty = false
                )
            )

        val inheritedFrom = if(propertyAccessorSymbol.origin == KaSymbolOrigin.JAVA_SYNTHETIC_PROPERTY) dri.copy(callable = null) else dri.getInheritedFromDRI(propertyParentDRI)

        @OptIn(KaExperimentalApi::class) // due to typeParameters
        val generics = propertyAccessorSymbol.typeParameters.mapIndexed { index, symbol ->
            visitVariantTypeParameter(
                index,
                symbol,
                dri
            )
        }

        return DFunction(
            dri = dri,
            name = name,
            isConstructor = false,
            parameters = propertyAccessorSymbol.valueParameters
                .mapIndexed { index, symbol -> visitValueParameter(index, symbol, dri) },
            contextParameters = emptyList(),
            receiver = propertyAccessorSymbol.receiverParameter?.let {
                visitReceiverParameter(
                    it,
                    dri
                )
            },
            expectPresentInSet = null,
            sources = getSource(propertyAccessorSymbol),
            visibility = propertyAccessorSymbol.visibility.toDokkaVisibility().toSourceSetDependent(),
            generics = generics,
            documentation = getAccessorSymbolDocumentation(propertyAccessorSymbol)?.toSourceSetDependent() ?: emptyMap(),
            modifier = propertyAccessorSymbol.getDokkaModality().toSourceSetDependent(),
            type = toBoundFrom(propertyAccessorSymbol.returnType),
            sourceSets = setOf(sourceSet),
            isExpectActual = false,
            extra = PropertyContainer.withAll(
                propertyAccessorSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                getDokkaAnnotationsFrom(propertyAccessorSymbol)?.toSourceSetDependent()?.toAnnotations()
            )
        )
    }

    private fun KaSession.visitConstructorSymbol(
        constructorSymbol: KaConstructorSymbol
    ): DFunction = withExceptionCatcher(constructorSymbol) {
        val name = constructorSymbol.containingClassId?.shortClassName?.asString()
            ?: throw IllegalStateException("Unknown containing class of constructor")
        val dri = createDRIWithOverridden(constructorSymbol).origin
        val isExpect = constructorSymbol.isExpect
        val isActual = constructorSymbol.isActual

        val generics = constructorSymbol.typeParameters.mapIndexed { index, symbol ->
            visitVariantTypeParameter(
                index,
                symbol,
                dri
            )
        }

        val documentation = getDocumentation(constructorSymbol)?.let { docNode ->
            if (constructorSymbol.isPrimary) {
                docNode.copy(children = (docNode.children.find { it is Constructor }?.root?.let { constructor ->
                    listOf(Description(constructor))
                } ?: emptyList<TagWrapper>()) + docNode.children.filterIsInstance<Param>())
            } else {
                docNode
            }
        }?.toSourceSetDependent()

        return DFunction(
            dri = dri,
            name = name,
            isConstructor = true,
            receiver = constructorSymbol.receiverParameter?.let {
                visitReceiverParameter(
                    it,
                    dri
                )
            },
            parameters = constructorSymbol.valueParameters
                .mapIndexed { index, symbol -> visitValueParameter(index, symbol, dri) },
            expectPresentInSet = sourceSet.takeIf { isExpect },
            sources = getSource(constructorSymbol),
            visibility = constructorSymbol.visibility.toDokkaVisibility().toSourceSetDependent(),
            generics = generics,
            documentation = documentation ?: emptyMap(),
            modifier = KotlinModifier.Empty.toSourceSetDependent(),
            type = toBoundFrom(constructorSymbol.returnType),
            sourceSets = setOf(sourceSet),
            isExpectActual = (isExpect || isActual),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(constructorSymbol)?.toSourceSetDependent()?.toAnnotations(),
                takeIf { constructorSymbol.isPrimary }?.let { PrimaryConstructorExtra }
            )
        )
    }

    private fun KaSession.visitFunctionSymbol(functionSymbol: KaNamedFunctionSymbol, parent: DRI): DFunction =
        withExceptionCatcher(functionSymbol) {
            val dri = createDRIWithOverridden(functionSymbol).origin
            val inheritedFrom = dri.getInheritedFromDRI(parent)
            val isExpect = functionSymbol.isExpect
            val isActual = functionSymbol.isActual

            val generics =
                functionSymbol.typeParameters.mapIndexed { index, symbol ->
                    visitVariantTypeParameter(
                        index,
                        symbol,
                        dri
                    )
                }

            return DFunction(
                dri = dri,
                name = functionSymbol.name.asString(),
                isConstructor = false,
                receiver = functionSymbol.receiverParameter?.let {
                    visitReceiverParameter(
                        it,
                        dri
                    )
                },
                parameters = functionSymbol.valueParameters
                    .mapIndexed { index, symbol -> visitValueParameter(index, symbol, dri) },
                contextParameters = @OptIn(KaExperimentalApi::class) functionSymbol.contextParameters
                    .mapIndexed { index, symbol -> visitContextParameter(index, symbol, dri) },
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sources = getSource(functionSymbol),
                visibility = functionSymbol.getDokkaVisibility().toSourceSetDependent(),
                generics = generics,
                documentation = getDocumentation(functionSymbol)?.toSourceSetDependent() ?: emptyMap(),
                modifier = functionSymbol.getDokkaModality().toSourceSetDependent(),
                type = toBoundFrom(functionSymbol.returnType),
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                    additionalExtrasOfFunction(functionSymbol)?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(functionSymbol)
                        ?.toSourceSetDependent()?.toAnnotations(),
                    ObviousMember.takeIf { isObvious(functionSymbol, inheritedFrom) },
                )
            )
        }

    private fun KaSession.visitValueParameter(
        index: Int, valueParameterSymbol: KaValueParameterSymbol, dri: DRI
    ) = DParameter(
        dri = dri.copy(target = PointingToCallableParameters(index)),
        name = valueParameterSymbol.name.asString(),
        type = toBoundFrom(valueParameterSymbol.returnType),
        expectPresentInSet = null,
        documentation = getDocumentation(valueParameterSymbol)?.toSourceSetDependent() ?: emptyMap(),
        sourceSets = setOf(sourceSet),
        extra = PropertyContainer.withAll(
            valueParameterSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
            getDokkaAnnotationsFrom(valueParameterSymbol)?.toSourceSetDependent()?.toAnnotations(),
            getDefaultValue(valueParameterSymbol, index)?.let { DefaultValue(it.toSourceSetDependent()) }
        )
    )

    @OptIn(KaExperimentalApi::class)
    private fun KaSession.visitContextParameter(
        index: Int, contextParameterSymbol: KaContextParameterSymbol, dri: DRI
    ): DParameter {
        return DParameter(
            dri = dri.copy(target = @OptIn(ExperimentalDokkaApi::class) PointingToContextParameters(index)),
            name = if(contextParameterSymbol.name == UNDERSCORE_FOR_UNUSED_VAR) "_" else contextParameterSymbol.name.asString(),
            type = toBoundFrom(contextParameterSymbol.returnType),
            expectPresentInSet = null,
            documentation = getDocumentation(contextParameterSymbol)?.toSourceSetDependent() ?: emptyMap(),
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(
               getDokkaAnnotationsFrom(contextParameterSymbol)?.toSourceSetDependent()?.toAnnotations(),
            )
        )
    }

    private fun KaSession.visitReceiverParameter(
        receiverParameterSymbol: KaReceiverParameterSymbol, dri: DRI
    ) = DParameter(
        dri = dri.copy(target = PointingToDeclaration),
        name = null,
        type = toBoundFrom(receiverParameterSymbol.returnType),
        expectPresentInSet = null,
        documentation = getDocumentation(receiverParameterSymbol)?.toSourceSetDependent() ?: emptyMap(),
        sourceSets = setOf(sourceSet),
        extra = PropertyContainer.withAll(
            getDokkaAnnotationsFrom(receiverParameterSymbol)?.toSourceSetDependent()?.toAnnotations()
        )
    )

    /**
     * TODO https://youtrack.jetbrains.com/issue/KT-61254/Analysis-API-Add-a-default-value-for-KtValueParameterSymbol
     * Retrieves the default value of a value parameter, if available from sources.
     * It may be `null` if the owner function comes from a non-source file.
     */
    private fun KaSession.getDefaultValue(symbol: KaValueParameterSymbol, parameterIndex: Int): Expression? {
        fun KaValueParameterSymbol.getExplicitDefaultValue(): Expression? =
            if (origin == KaSymbolOrigin.SOURCE) (psi as? KtParameter)?.defaultValue?.toDefaultValueExpression() else null
        fun KaDeclarationSymbol.findMatchingParameterWithDefaultValue(): Expression? =
            (this as? KaFunctionSymbol)?.valueParameters?.getOrNull(parameterIndex)?.getExplicitDefaultValue()

        val result = symbol.getExplicitDefaultValue()
        return if (result != null)
            result
        else { // in the case of fake declarations
            val ownerFunction = symbol.containingDeclaration as? KaNamedFunctionSymbol ?: return null

            //overriding function
            if (ownerFunction.isOverride)
                ownerFunction.allOverriddenSymbols.firstNotNullOfOrNull { it.findMatchingParameterWithDefaultValue() }
            else null
        }
    }

    @OptIn(KaExperimentalApi::class) // due to `KaPropertySymbol.initializer`
    private fun KaPropertySymbol.getDefaultValue(): Expression? =
        (initializer?.initializerPsi as? KtConstantExpression)?.toDefaultValueExpression() // TODO consider [KaConstantInitializerValue], but should we keep an original format, e.g. 0xff or 0b101?

    private fun KtExpression.toDefaultValueExpression(): Expression? = when (node?.elementType) {
        KtNodeTypes.INTEGER_CONSTANT -> PsiLiteralUtil.parseLong(node?.text)?.let { IntegerConstant(it) }
        KtNodeTypes.FLOAT_CONSTANT -> if (node?.text?.lowercase()?.endsWith('f') == true)
            PsiLiteralUtil.parseFloat(node?.text)?.let { FloatConstant(it) }
        else PsiLiteralUtil.parseDouble(node?.text)?.let { DoubleConstant(it) }

        KtNodeTypes.BOOLEAN_CONSTANT -> BooleanConstant(node?.text == "true")
        KtNodeTypes.STRING_TEMPLATE -> StringConstant(node.findChildByType(KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY)?.text.orEmpty())
        else -> node?.text?.let { ComplexExpression(it) }
    }

    private fun KaSession.visitVariantTypeParameter(
        index: Int,
        typeParameterSymbol: KaTypeParameterSymbol,
        dri: DRI
    ): DTypeParameter {
        val upperBoundsOrNullableAny =
            typeParameterSymbol.upperBounds.takeIf { it.isNotEmpty() } ?: listOf(this.builtinTypes.nullableAny)
        return DTypeParameter(
            variantTypeParameter = TypeParameter(
                dri = dri.copy(target = PointingToGenericParameters(index)),
                name = typeParameterSymbol.name.asString(),
                presentableName = typeParameterSymbol.getPresentableName()
            ).wrapWithVariance(typeParameterSymbol.variance),
            documentation = getDocumentation(typeParameterSymbol)?.toSourceSetDependent() ?: emptyMap(),
            expectPresentInSet = null,
            bounds = upperBoundsOrNullableAny.map { toBoundFrom(it) },
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(typeParameterSymbol)?.toSourceSetDependent()?.toAnnotations()
            )
        )
    }
    // ----------- Utils ----------------------------------------------------------------------------

    private fun KaSession.getDokkaAnnotationsFrom(annotated: KaAnnotated): List<Annotations.Annotation>? =
        with(annotationTranslator) { getAllAnnotationsFrom(annotated) }.takeUnless { it.isEmpty() }

    private fun KaSession.toBoundFrom(type: KaType) =
        with(typeTranslator) { toBoundFrom(type) }

    /**
     * `createDRI` returns the DRI of the exact element and potential DRI of an element that is overriding it
     * (It can be also FAKE_OVERRIDE which is in fact just inheritance of the symbol)
     *
     * Looking at what PSIs do, they give the DRI of the element within the classnames where it is actually
     * declared and inheritedFrom as the same DRI but truncated callable part.
     * Therefore, we set callable to null and take the DRI only if it is indeed coming from different class.
     */
    private fun DRI.getInheritedFromDRI(dri: DRI): DRI? {
        return this.copy(callable = null)
            .takeIf { dri.classNames != this.classNames || dri.packageName != this.packageName }
    }

    data class DRIWithOverridden(val origin: DRI, val overridden: DRI? = null)

    private fun KaSession.createDRIWithOverridden(
        callableSymbol: KaCallableSymbol,
        wasOverriddenBy: DRI? = null
    ): DRIWithOverridden {
        /**
         * `open class A { fun x() = 0 }` is given
         *
         * There are two cases:
         * 1. The callable `B.x` [DRI] should lead to itself - `B.x`.
         *    Dokka will create a separate page for the declaration.
         *    This case should be actual for cases when a callable is explicitly declared in a class.
         *    E.g. for override - `class B : A() { override fun x() = 1 }`
         * 2. The `B.x` [DRI] should be lead to the parent `A.x`
         *    For the case `class B : A() {}` the compiler returns the `A.x` symbol itself.
         *    But in some cases, it creates and returns the `B.x` symbol:
         *      - fake overrides
         *        K2 distinguishes two kinds of fake overrides: [KaSymbolOrigin.INTERSECTION_OVERRIDE] and [KaSymbolOrigin.SUBSTITUTION_OVERRIDE]
         *      - synthetic members, e.g. `hashCode`/`equals` for data classes
         *      - delegating members
         */
        val isDeclaration = callableSymbol.origin == KaSymbolOrigin.SOURCE || callableSymbol.origin == KaSymbolOrigin.LIBRARY
        if (isDeclaration) {
            return DRIWithOverridden(getDRIFromSymbol(callableSymbol), wasOverriddenBy)

        } else { // fake, synthetic, delegating
            val firstOverriddenSymbolOrNull = callableSymbol.directlyOverriddenSymbols.firstOrNull()
            return if (firstOverriddenSymbolOrNull == null) {
                DRIWithOverridden(getDRIFromSymbol(callableSymbol), wasOverriddenBy)
            } else {
                createDRIWithOverridden(firstOverriddenSymbolOrNull)
            }
        }
    }

    private fun KaSession.getAccessorSymbolDocumentation(symbol: KaPropertyAccessorSymbol): DocumentationNode? {
        val documentation = getDocumentation(symbol) ?: return null
        return documentation.removePropertyTag()
    }

    private fun KaSession.getDocumentation(symbol: KaSymbol) =
        if (symbol.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED)
            // a primary (implicit default) constructor  can be generated, so we need KDoc from @constructor tag
            getGeneratedKDocDocumentationFrom(symbol) ?: if(symbol is KaConstructorSymbol) getKDocDocumentationFrom(symbol, logger, sourceSet) else null
        else
            getKDocDocumentationFrom(symbol, logger, sourceSet) ?: javadocParser?.let { getJavaDocDocumentationFrom(symbol, it) }

    /**
     * Unwrap the documentation for property accessors from the [Property] wrapper if its present.
     * Otherwise, documentation will not be rendered
     */
    private fun DocumentationNode.removePropertyTag(): DocumentationNode =
        DocumentationNode(children.map { it.removePropertyTag() })

    private fun TagWrapper.removePropertyTag(): TagWrapper = when (this) {
        is Property -> Description(root)
        else -> this
    }

    private fun KaSession.isObvious(functionSymbol: KaFunctionSymbol, inheritedFrom: DRI?): Boolean {
        return functionSymbol.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED && !hasGeneratedKDocDocumentation(functionSymbol) ||
                inheritedFrom?.isObvious() == true
    }

    private fun DRI.isObvious(): Boolean = when (packageName) {
        "kotlin" -> classNames == "Any" || classNames == "Enum"
        "java.lang" -> classNames == "Object" || classNames == "Enum"
        else -> false
    }

    private fun KaSession.getSource(symbol: KaSymbol): SourceSetDependent<DocumentableSource> {
        val psi = when(symbol) {
            // implicit/default accessors have no psi
            is KaPropertyAccessorSymbol -> symbol.containingSymbol?.psi
            else -> symbol.psi
        }
        return KtPsiDocumentableSource(psi).toSourceSetDependent()
    }

    private fun AncestryNode.exceptionInSupertypesOrNull(): ExceptionInSupertypes? =
        typeConstructorsBeingExceptions().takeIf { it.isNotEmpty() }
            ?.let { ExceptionInSupertypes(it.toSourceSetDependent()) }


    // ----------- Translators of modifiers ----------------------------------------------------------------------------
    /**
     * Dokka has its own conditions for the keyword `override`
     */
    private fun KaSession.isDokkaOverride(symbol: KaCallableSymbol): Boolean {
        fun KaCallableSymbol.isCompilerOverride() =
            if (this is KaPropertySymbol) isOverride else if (this is KaNamedFunctionSymbol) isOverride else error("Should be property or function but was '${this::class}'")
        fun KaSymbol.isFake() = origin == KaSymbolOrigin.SUBSTITUTION_OVERRIDE || origin == KaSymbolOrigin.INTERSECTION_OVERRIDE


        /**
         * In the following example, class A has two fake functions: one with the keyword `override`, and the other without.
         * In the compiler, both are treated as overrides (`isOverride=true`).
         *
         * ```kt
         * class A<T>  : C<T>()
         *
         *  open class C<T> : D<T> {
         *     override fun f(a: T) = 1
         *     fun f2(a: T) = 1
         * }
         *
         * //...
         * ```
         */
        return if(symbol.isFake()) {
            symbol.allOverriddenSymbols.firstOrNull { !it.isFake() }?.isCompilerOverride() ?: false
        } else {
            symbol.isCompilerOverride()
        }
    }

    private fun KaDeclarationSymbol.getDokkaModality(): KotlinModifier {
        val isInterface = this is KaClassSymbol && classKind == KaClassKind.INTERFACE
        return if (isInterface) {
            // only two modalities are possible for interfaces:
            //  - `SEALED` - when it's declared as `sealed interface`
            //  - `ABSTRACT` - when it's declared as `interface` or `abstract interface` (`abstract` is redundant but possible here)
            when (modality) {
                KaSymbolModality.SEALED -> KotlinModifier.Sealed
                else -> KotlinModifier.Empty
            }
        } else {
            when (modality) {
                KaSymbolModality.FINAL -> KotlinModifier.Final
                KaSymbolModality.SEALED -> KotlinModifier.Sealed
                KaSymbolModality.OPEN -> KotlinModifier.Open
                KaSymbolModality.ABSTRACT -> KotlinModifier.Abstract
            }
        }
    }
    private fun KaDeclarationSymbol.getDokkaVisibility() = visibility.toDokkaVisibility()
    private fun KaValueParameterSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.NoInline.takeIf { isNoinline },
        ExtraModifiers.KotlinOnlyModifiers.CrossInline.takeIf { isCrossinline },
        ExtraModifiers.KotlinOnlyModifiers.VarArg.takeIf { isVararg }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KaPropertyAccessorSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Inline.takeIf { isInline },
//ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        ExtraModifiers.KotlinOnlyModifiers.Override.takeIf { isOverride }
    ).toSet().takeUnless { it.isEmpty() }


    private fun KaSession.additionalExtrasOfProperty(symbol: KaPropertySymbol) = with(symbol) {
        listOfNotNull(
            ExtraModifiers.KotlinOnlyModifiers.Const.takeIf { (this as? KaKotlinPropertySymbol)?.isConst == true },
            ExtraModifiers.KotlinOnlyModifiers.LateInit.takeIf { (this as? KaKotlinPropertySymbol)?.isLateInit == true },
            //ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isJvmStaticInObjectOrClassOrInterface() },
            ExtraModifiers.KotlinOnlyModifiers.External.takeIf { isExternal },
            //ExtraModifiers.KotlinOnlyModifiers.Static.takeIf { isStatic },
            ExtraModifiers.KotlinOnlyModifiers.Override.takeIf {
                isDokkaOverride(this)
            }
        ).toSet().takeUnless { it.isEmpty() }
    }

    private fun KaJavaFieldSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isStatic }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KaSession.additionalExtrasOfFunction(symbol: KaNamedFunctionSymbol) = with(symbol) {
        listOfNotNull(
            ExtraModifiers.KotlinOnlyModifiers.Infix.takeIf { isInfix },
            ExtraModifiers.KotlinOnlyModifiers.Inline.takeIf { isInline },
            ExtraModifiers.KotlinOnlyModifiers.Suspend.takeIf { isSuspend },
            ExtraModifiers.KotlinOnlyModifiers.Operator.takeIf { isOperator },
//ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isJvmStaticInObjectOrClassOrInterface() },
            ExtraModifiers.KotlinOnlyModifiers.TailRec.takeIf { isTailRec },
            ExtraModifiers.KotlinOnlyModifiers.External.takeIf { isExternal },
            ExtraModifiers.KotlinOnlyModifiers.Override.takeIf {
                isDokkaOverride(this)
            }
        ).toSet().takeUnless { it.isEmpty() }
    }

    private fun KaNamedClassSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Inline.takeIf { (this.psi as? KtClass)?.isInline() == true },
        ExtraModifiers.KotlinOnlyModifiers.Value.takeIf { (this.psi as? KtClass)?.isValue() == true },
        ExtraModifiers.KotlinOnlyModifiers.External.takeIf { isExternal },
        ExtraModifiers.KotlinOnlyModifiers.Inner.takeIf { isInner },
        ExtraModifiers.KotlinOnlyModifiers.Data.takeIf { isData },
        ExtraModifiers.KotlinOnlyModifiers.Fun.takeIf { isFun },
    ).toSet().takeUnless { it.isEmpty() }

    private fun KaSymbolVisibility.toDokkaVisibility(): Visibility = when (this) {
        KaSymbolVisibility.PUBLIC -> KotlinVisibility.Public
        KaSymbolVisibility.PROTECTED -> KotlinVisibility.Protected
        KaSymbolVisibility.INTERNAL -> KotlinVisibility.Internal
        KaSymbolVisibility.PRIVATE -> KotlinVisibility.Private
        KaSymbolVisibility.PACKAGE_PROTECTED -> KotlinVisibility.Protected
        KaSymbolVisibility.PACKAGE_PRIVATE -> JavaVisibility.Default
        KaSymbolVisibility.UNKNOWN, KaSymbolVisibility.LOCAL -> KotlinVisibility.Public
    }
}
