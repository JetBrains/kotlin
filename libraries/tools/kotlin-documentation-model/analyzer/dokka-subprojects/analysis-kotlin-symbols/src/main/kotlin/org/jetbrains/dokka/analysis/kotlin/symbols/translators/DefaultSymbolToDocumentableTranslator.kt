/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators


import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.*
import com.intellij.psi.util.PsiLiteralUtil
import org.jetbrains.dokka.DokkaConfiguration
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
import org.jetbrains.dokka.links.Callable
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
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier

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
 * Maps [KtSymbol] to Documentable model [Documentable]
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
    private var annotationTranslator = AnnotationTranslator()
    private var typeTranslator = TypeTranslator(sourceSet, annotationTranslator)

    private fun <T> T.toSourceSetDependent() = if (this != null) mapOf(sourceSet to this) else emptyMap()

    // KT-54846 will replace
    private val KtDeclarationSymbol.isActual
        get() = (psi as? KtModifierListOwner)?.hasActualModifier() == true
    private val KtDeclarationSymbol.isExpect
        get() = (psi as? KtModifierListOwner)?.hasExpectModifier() == true

    private fun <T : KtSymbol> List<T>.filterSymbolsInSourceSet(moduleFiles: Set<KtFile>): List<T> = filter {
        when (val file = it.psi?.containingFile) {
            is KtFile -> moduleFiles.contains(file)
            else -> false
        }
    }

    fun visitModule(): DModule {
        val sourceModule = analysisContext.getModule(sourceSet)
        val ktFiles = analysisContext.modulesWithFiles[sourceModule]?.filterIsInstance<KtFile>()?.toSet()
            ?: throw IllegalStateException("No source files for a source module ${sourceModule.moduleName} of source set ${sourceSet.sourceSetID}")
        val processedPackages: MutableSet<FqName> = mutableSetOf()
        return analyze(sourceModule) {
            val packages = ktFiles.mapNotNull { file ->
                if (processedPackages.contains(file.packageFqName))
                    return@mapNotNull null
                processedPackages.add(file.packageFqName)
                getPackageSymbolIfPackageExists(file.packageFqName)?.let { packageSymbol ->
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

    private fun KtAnalysisSession.visitPackageSymbol(
        packageSymbol: KtPackageSymbol,
        moduleFiles: Set<KtFile>
    ): DPackage {
        val dri = getDRIFromPackage(packageSymbol)
        val scope = packageSymbol.getPackageScope()
        val callables = scope.getCallableSymbols().toList().filterSymbolsInSourceSet(moduleFiles)
        val classifiers = scope.getClassifierSymbols().toList().filterSymbolsInSourceSet(moduleFiles)

        val functions = callables.filterIsInstance<KtFunctionSymbol>().map { visitFunctionSymbol(it, dri) }
        val properties = callables.filterIsInstance<KtPropertySymbol>().map { visitPropertySymbol(it, dri) }
        val classlikes =
            classifiers.filterIsInstance<KtNamedClassOrObjectSymbol>()
                .map { visitNamedClassOrObjectSymbol(it, dri) }
        val typealiases = classifiers.filterIsInstance<KtTypeAliasSymbol>().map { visitTypeAliasSymbol(it, dri) }

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

    private fun KtAnalysisSession.visitTypeAliasSymbol(
        typeAliasSymbol: KtTypeAliasSymbol,
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
            sources = typeAliasSymbol.getSource(),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(typeAliasSymbol)?.toSourceSetDependent()?.toAnnotations(),
                ancestryInfo.exceptionInSupertypesOrNull(),
            )
        )
    }

    fun KtAnalysisSession.visitNamedClassOrObjectSymbol(
        namedClassOrObjectSymbol: KtNamedClassOrObjectSymbol,
        parent: DRI
    ): DClasslike = withExceptionCatcher(namedClassOrObjectSymbol) {
        val name = namedClassOrObjectSymbol.name.asString()
        val dri = parent.withClass(name)

        val isExpect = namedClassOrObjectSymbol.isExpect
        val isActual = namedClassOrObjectSymbol.isActual
        val documentation = getDocumentation(namedClassOrObjectSymbol)?.toSourceSetDependent() ?: emptyMap()

        val (constructors, functions, properties, classlikesWithoutCompanion) = getDokkaScopeFrom(namedClassOrObjectSymbol, dri)

        val companionObject = namedClassOrObjectSymbol.companionObject?.let {
            visitNamedClassOrObjectSymbol(
                it,
                dri
            )
        } as? DObject
        val classlikes = if (companionObject == null) classlikesWithoutCompanion else classlikesWithoutCompanion + companionObject

        val generics = namedClassOrObjectSymbol.typeParameters.mapIndexed { index, symbol ->
            visitVariantTypeParameter(
                index,
                symbol,
                dri
            )
        }

        val ancestryInfo =
            with(typeTranslator) { buildAncestryInformationFrom(namedClassOrObjectSymbol.buildSelfClassType()) }
        val supertypes =
            //(ancestryInfo.interfaces.map{ it.typeConstructor } + listOfNotNull(ancestryInfo.superclass?.typeConstructor))
            namedClassOrObjectSymbol.superTypes.filterNot { it.isAny }
                .map { with(typeTranslator) { toTypeConstructorWithKindFrom(it) } }
                .toSourceSetDependent()
        return@withExceptionCatcher when (namedClassOrObjectSymbol.classKind) {
            KtClassKind.OBJECT, KtClassKind.COMPANION_OBJECT ->
                DObject(
                    dri = dri,
                    name = name,
                    functions = functions,
                    properties = properties,
                    classlikes = classlikes,
                    sources = namedClassOrObjectSymbol.getSource(),
                    expectPresentInSet = sourceSet.takeIf { isExpect },
                    visibility = namedClassOrObjectSymbol.getDokkaVisibility().toSourceSetDependent(),
                    supertypes = supertypes,
                    documentation = documentation,
                    sourceSets = setOf(sourceSet),
                    isExpectActual = (isExpect || isActual),
                    extra = PropertyContainer.withAll(
                        namedClassOrObjectSymbol.additionalExtras()?.toSourceSetDependent()
                            ?.toAdditionalModifiers(),
                        getDokkaAnnotationsFrom(namedClassOrObjectSymbol)?.toSourceSetDependent()?.toAnnotations(),
                        ImplementedInterfaces(ancestryInfo.allImplementedInterfaces().toSourceSetDependent()),
                        ancestryInfo.exceptionInSupertypesOrNull()
                    )
                )

            KtClassKind.CLASS -> DClass(
                dri = dri,
                name = name,
                constructors = constructors,
                functions = functions,
                properties = properties,
                classlikes = classlikes,
                sources = namedClassOrObjectSymbol.getSource(),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                visibility = namedClassOrObjectSymbol.getDokkaVisibility().toSourceSetDependent(),
                supertypes = supertypes,
                generics = generics,
                documentation = documentation,
                modifier = namedClassOrObjectSymbol.getDokkaModality().toSourceSetDependent(),
                companion = companionObject,
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    namedClassOrObjectSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(namedClassOrObjectSymbol)?.toSourceSetDependent()?.toAnnotations(),
                    ImplementedInterfaces(ancestryInfo.allImplementedInterfaces().toSourceSetDependent()),
                    ancestryInfo.exceptionInSupertypesOrNull()
                )
            )

            KtClassKind.INTERFACE -> DInterface(
                dri = dri,
                name = name,
                functions = functions,
                properties = properties,
                classlikes = classlikes,
                sources = namedClassOrObjectSymbol.getSource(), //
                expectPresentInSet = sourceSet.takeIf { isExpect },
                visibility = namedClassOrObjectSymbol.getDokkaVisibility().toSourceSetDependent(),
                supertypes = supertypes,
                generics = generics,
                documentation = documentation,
                companion = companionObject,
                modifier = namedClassOrObjectSymbol.getDokkaModality().toSourceSetDependent(),
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    namedClassOrObjectSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(namedClassOrObjectSymbol)?.toSourceSetDependent()?.toAnnotations(),
                    ImplementedInterfaces(ancestryInfo.allImplementedInterfaces().toSourceSetDependent()),
                    ancestryInfo.exceptionInSupertypesOrNull()
                )
            )

            KtClassKind.ANNOTATION_CLASS -> DAnnotation(
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
                visibility = namedClassOrObjectSymbol.getDokkaVisibility().toSourceSetDependent(),
                generics = generics,
                constructors = constructors,
                sources = namedClassOrObjectSymbol.getSource(),
                extra = PropertyContainer.withAll(
                    namedClassOrObjectSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(namedClassOrObjectSymbol)?.toSourceSetDependent()?.toAnnotations(),
                )
            )

            KtClassKind.ANONYMOUS_OBJECT -> throw NotImplementedError("ANONYMOUS_OBJECT does not support")
            KtClassKind.ENUM_CLASS -> {
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
                    getDokkaScopeFrom(namedClassOrObjectSymbol, dri, includeStaticScope = false).let {
                        it.copy(
                            functions = it.functions.map { it.withNewExtras( it.extra + InheritedMember(dri.copy(callable = null).toSourceSetDependent())) },
                            properties = it.properties.map { it.withNewExtras( it.extra + InheritedMember(dri.copy(callable = null).toSourceSetDependent())) }
                        )
                    }
                }

                val entries =
                    namedClassOrObjectSymbol.getEnumEntries().map {
                        visitEnumEntrySymbol(it, enumEntryScope.value)
                    }

                DEnum(
                    dri = dri,
                    name = name,
                    entries = entries,
                    constructors = constructors,
                    functions = functions,
                    properties = properties,
                    classlikes = classlikes,
                    sources = namedClassOrObjectSymbol.getSource(),
                    expectPresentInSet = sourceSet.takeIf { isExpect },
                    visibility = namedClassOrObjectSymbol.getDokkaVisibility().toSourceSetDependent(),
                    supertypes = supertypes,
                    documentation = documentation,
                    companion = namedClassOrObjectSymbol.companionObject?.let {
                        visitNamedClassOrObjectSymbol(
                            it,
                            dri
                        )
                    } as? DObject,
                    sourceSets = setOf(sourceSet),
                    isExpectActual = (isExpect || isActual),
                    extra = PropertyContainer.withAll(
                        namedClassOrObjectSymbol.additionalExtras()?.toSourceSetDependent()
                            ?.toAdditionalModifiers(),
                        getDokkaAnnotationsFrom(namedClassOrObjectSymbol)?.toSourceSetDependent()?.toAnnotations(),
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
        val classlikesWithoutCompanion: List<DClasslike>
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
     * See [org.jetbrains.kotlin.analysis.api.components.KtScopeProvider.getStaticDeclaredMemberScope] for what a static scope is.
     */
    private fun KtAnalysisSession.getDokkaScopeFrom(
        namedClassOrObjectSymbol: KtNamedClassOrObjectSymbol,
        dri: DRI,
        includeStaticScope: Boolean = true
    ): DokkaScope {
        // getCombinedMemberScope additionally includes a static scope, see [getCombinedMemberScope]
        // e.g. getStaticMemberScope contains `valueOf`, `values` and `entries` members for Enum
        val scope = if(includeStaticScope) namedClassOrObjectSymbol.getCombinedMemberScope() else namedClassOrObjectSymbol.getMemberScope()
        val constructors = scope.getConstructors().map { visitConstructorSymbol(it) }.toList()

        val callables = scope.getCallableSymbols().toList()

        // Dokka K1 does not show inherited nested and inner classes,
        // so it should show only classifiers (classes and objects) explicitly declared
        val classifiers = if(includeStaticScope) namedClassOrObjectSymbol.getStaticMemberScope().getClassifierSymbols() else emptySequence()

        val syntheticJavaProperties =
            namedClassOrObjectSymbol.buildSelfClassType().getSyntheticJavaPropertiesScope()?.getCallableSignatures()
                ?.map { it.symbol }
                ?.filterIsInstance<KtSyntheticJavaPropertySymbol>() ?: emptySequence()

        fun List<KtJavaFieldSymbol>.filterOutSyntheticJavaPropBackingField() =
            filterNot { javaField -> syntheticJavaProperties.any { it.hasBackingField && javaField.name == it.name } }

        val javaFields = callables.filterIsInstance<KtJavaFieldSymbol>()
            .filterOutSyntheticJavaPropBackingField()

        fun List<KtFunctionSymbol>.filterOutSyntheticJavaPropAccessors() = filterNot { fn ->
            if (fn.origin == KtSymbolOrigin.JAVA && fn.callableIdIfNonLocal != null)
                syntheticJavaProperties.any { fn.callableIdIfNonLocal == it.javaGetterSymbol.callableIdIfNonLocal || fn.callableIdIfNonLocal == it.javaSetterSymbol?.callableIdIfNonLocal }
            else false
        }

        val functions = callables.filterIsInstance<KtFunctionSymbol>()
            .filterOutSyntheticJavaPropAccessors().map { visitFunctionSymbol(it, dri) }


        val properties = callables.filterIsInstance<KtPropertySymbol>().map { visitPropertySymbol(it, dri) } +
                syntheticJavaProperties.map { visitPropertySymbol(it, dri) } +
                javaFields.map { visitJavaFieldSymbol(it, dri) }


        fun Sequence<KtNamedClassOrObjectSymbol>.filterOutCompanion() =
                filterNot {
                    it.classKind == KtClassKind.COMPANION_OBJECT
                }

        val classlikes = classifiers.filterIsInstance<KtNamedClassOrObjectSymbol>()
            .filterOutCompanion() // also, this is a hack to filter out companion for enum
            .map { visitNamedClassOrObjectSymbol(it, dri) }

        return DokkaScope(
            constructors = constructors,
            functions = functions,
            properties = properties,
            classlikesWithoutCompanion = classlikes.toList()
        )
    }

    private fun KtAnalysisSession.visitEnumEntrySymbol(
        enumEntrySymbol: KtEnumEntrySymbol, scope: DokkaScope
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

    private fun KtAnalysisSession.visitPropertySymbol(propertySymbol: KtPropertySymbol, parent: DRI): DProperty =
        withExceptionCatcher(propertySymbol) {
            val dri = createDRIWithOverridden(propertySymbol).origin
            val inheritedFrom = dri.getInheritedFromDRI(parent)
            val isExpect = propertySymbol.isExpect
            val isActual = propertySymbol.isActual
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
                sources = propertySymbol.getSource(),
                getter = propertySymbol.getter?.let { visitPropertyAccessor(it, propertySymbol, dri) },
                setter = propertySymbol.setter?.let { visitPropertyAccessor(it, propertySymbol, dri) },
                visibility = propertySymbol.visibility.toDokkaVisibility().toSourceSetDependent(),
                documentation = getDocumentation(propertySymbol)?.toSourceSetDependent() ?: emptyMap(), // TODO
                modifier = propertySymbol.getDokkaModality().toSourceSetDependent(),
                type = toBoundFrom(propertySymbol.returnType),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sourceSets = setOf(sourceSet),
                generics = generics,
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    propertySymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(propertySymbol)?.toSourceSetDependent()?.toAnnotations(),
                    propertySymbol.getDefaultValue()?.let { DefaultValue(it.toSourceSetDependent()) },
                    inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                    takeUnless { propertySymbol.isVal }?.let { IsVar },
                    takeIf { propertySymbol.psi is KtParameter }?.let { IsAlsoParameter(listOf(sourceSet)) }
                )
            )
        }

    private fun KtAnalysisSession.visitJavaFieldSymbol(
        javaFieldSymbol: KtJavaFieldSymbol,
        parent: DRI
    ): DProperty =
        withExceptionCatcher(javaFieldSymbol) {
            val dri = createDRIWithOverridden(javaFieldSymbol).origin
            val inheritedFrom = dri.getInheritedFromDRI(parent)
            val isExpect = false
            val isActual = false
            val generics =
                javaFieldSymbol.typeParameters.mapIndexed { index, symbol ->
                    visitVariantTypeParameter(
                        index,
                        symbol,
                        dri
                    )
                }

            return DProperty(
                dri = dri,
                name = javaFieldSymbol.name.asString(),
                receiver = javaFieldSymbol.receiverParameter?.let {
                    visitReceiverParameter(
                        it,
                        dri
                    )
                },
                sources = javaFieldSymbol.getSource(),
                getter = null,
                setter = null,
                visibility = javaFieldSymbol.getDokkaVisibility().toSourceSetDependent(),
                documentation = getDocumentation(javaFieldSymbol)?.toSourceSetDependent() ?: emptyMap(), // TODO
                modifier = javaFieldSymbol.getDokkaModality().toSourceSetDependent(),
                type = toBoundFrom(javaFieldSymbol.returnType),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sourceSets = setOf(sourceSet),
                generics = generics,
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

    private fun KtAnalysisSession.visitPropertyAccessor(
        propertyAccessorSymbol: KtPropertyAccessorSymbol,
        propertySymbol: KtPropertySymbol,
        propertyDRI: DRI
    ): DFunction = withExceptionCatcher(propertyAccessorSymbol) {
        val isGetter = propertyAccessorSymbol is KtPropertyGetterSymbol
        // it also covers @JvmName annotation
        val name = (if (isGetter) propertySymbol.javaGetterName else propertySymbol.javaSetterName)?.asString() ?: ""

        // SyntheticJavaProperty has callableIdIfNonLocal, propertyAccessorSymbol.origin = JAVA_SYNTHETIC_PROPERTY
        // For Kotlin properties callableIdIfNonLocal=null
        val dri = if (propertyAccessorSymbol.callableIdIfNonLocal != null)
            getDRIFromFunctionLike(propertyAccessorSymbol)
        else
            propertyDRI.copy(
                callable = Callable(name, null, propertyAccessorSymbol.valueParameters.map { getTypeReferenceFrom(it.returnType) })
            )
        // for SyntheticJavaProperty
        val inheritedFrom = if(propertyAccessorSymbol.origin == KtSymbolOrigin.JAVA_SYNTHETIC_PROPERTY) dri.copy(callable = null) else null

        val isExpect = propertyAccessorSymbol.isExpect
        val isActual = propertyAccessorSymbol.isActual

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
            receiver = propertyAccessorSymbol.receiverParameter?.let {
                visitReceiverParameter(
                    it,
                    dri
                )
            },
            parameters = propertyAccessorSymbol.valueParameters.mapIndexed { index, symbol ->
                visitValueParameter(index, symbol, dri)
            },
            expectPresentInSet = sourceSet.takeIf { isExpect },
            sources = propertyAccessorSymbol.getSource(),
            visibility = propertyAccessorSymbol.visibility.toDokkaVisibility().toSourceSetDependent(),
            generics = generics,
            documentation = getDocumentation(propertyAccessorSymbol)?.toSourceSetDependent() ?: emptyMap(),
            modifier = propertyAccessorSymbol.getDokkaModality().toSourceSetDependent(),
            type = toBoundFrom(propertyAccessorSymbol.returnType),
            sourceSets = setOf(sourceSet),
            isExpectActual = (isExpect || isActual),
            extra = PropertyContainer.withAll(
                propertyAccessorSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                getDokkaAnnotationsFrom(propertyAccessorSymbol)?.toSourceSetDependent()?.toAnnotations()
            )
        )
    }

    private fun KtAnalysisSession.visitConstructorSymbol(
        constructorSymbol: KtConstructorSymbol
    ): DFunction = withExceptionCatcher(constructorSymbol) {
        val name = constructorSymbol.containingClassIdIfNonLocal?.shortClassName?.asString()
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
            parameters = constructorSymbol.valueParameters.mapIndexed { index, symbol ->
                visitValueParameter(index, symbol, dri)
            },
            expectPresentInSet = sourceSet.takeIf { isExpect },
            sources = constructorSymbol.getSource(),
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

    private fun KtAnalysisSession.visitFunctionSymbol(functionSymbol: KtFunctionSymbol, parent: DRI): DFunction =
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
                parameters = functionSymbol.valueParameters.mapIndexed { index, symbol ->
                    visitValueParameter(index, symbol, dri)
                },
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sources = functionSymbol.getSource(),
                visibility = functionSymbol.getDokkaVisibility().toSourceSetDependent(),
                generics = generics,
                documentation = getDocumentation(functionSymbol)?.toSourceSetDependent() ?: emptyMap(),
                modifier = functionSymbol.getDokkaModality().toSourceSetDependent(),
                type = toBoundFrom(functionSymbol.returnType),
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                    functionSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(functionSymbol)
                        ?.toSourceSetDependent()?.toAnnotations(),
                    ObviousMember.takeIf { isObvious(functionSymbol, inheritedFrom) },
                )
            )
        }

    private fun KtAnalysisSession.visitValueParameter(
        index: Int, valueParameterSymbol: KtValueParameterSymbol, dri: DRI
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
            valueParameterSymbol.getDefaultValue()?.let { DefaultValue(it.toSourceSetDependent()) }
        )
    )

    private fun KtAnalysisSession.visitReceiverParameter(
        receiverParameterSymbol: KtReceiverParameterSymbol, dri: DRI
    ) = DParameter(
        dri = dri.copy(target = PointingToDeclaration),
        name = null,
        type = toBoundFrom(receiverParameterSymbol.type),
        expectPresentInSet = null,
        documentation = getDocumentation(receiverParameterSymbol)?.toSourceSetDependent() ?: emptyMap(),
        sourceSets = setOf(sourceSet),
        extra = PropertyContainer.withAll(
            getDokkaAnnotationsFrom(receiverParameterSymbol)?.toSourceSetDependent()?.toAnnotations()
        )
    )

    private fun KtValueParameterSymbol.getDefaultValue(): Expression? =
        if (origin == KtSymbolOrigin.SOURCE) (psi as? KtParameter)?.defaultValue?.toDefaultValueExpression()
        else null

    private fun KtPropertySymbol.getDefaultValue(): Expression? =
        (initializer?.initializerPsi as? KtConstantExpression)?.toDefaultValueExpression() // TODO consider [KtConstantInitializerValue], but should we keep an original format, e.g. 0xff or 0b101?

    private fun KtExpression.toDefaultValueExpression(): Expression? = when (node?.elementType) {
        KtNodeTypes.INTEGER_CONSTANT -> PsiLiteralUtil.parseLong(node?.text)?.let { IntegerConstant(it) }
        KtNodeTypes.FLOAT_CONSTANT -> if (node?.text?.toLowerCase()?.endsWith('f') == true)
            PsiLiteralUtil.parseFloat(node?.text)?.let { FloatConstant(it) }
        else PsiLiteralUtil.parseDouble(node?.text)?.let { DoubleConstant(it) }

        KtNodeTypes.BOOLEAN_CONSTANT -> BooleanConstant(node?.text == "true")
        KtNodeTypes.STRING_TEMPLATE -> StringConstant(node.findChildByType(KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY)?.text.orEmpty())
        else -> node?.text?.let { ComplexExpression(it) }
    }

    private fun KtAnalysisSession.visitVariantTypeParameter(
        index: Int,
        typeParameterSymbol: KtTypeParameterSymbol,
        dri: DRI
    ): DTypeParameter {
        val upperBoundsOrNullableAny =
            typeParameterSymbol.upperBounds.takeIf { it.isNotEmpty() } ?: listOf(this.builtinTypes.NULLABLE_ANY)
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

    private fun KtAnalysisSession.getDokkaAnnotationsFrom(annotated: KtAnnotated): List<Annotations.Annotation>? =
        with(annotationTranslator) { getAllAnnotationsFrom(annotated) }.takeUnless { it.isEmpty() }

    private fun KtAnalysisSession.toBoundFrom(type: KtType) =
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

    private fun KtAnalysisSession.createDRIWithOverridden(
        callableSymbol: KtCallableSymbol,
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
         *        K2 distinguishes two kinds of fake overrides: [KtSymbolOrigin.INTERSECTION_OVERRIDE] and [KtSymbolOrigin.SUBSTITUTION_OVERRIDE]
         *      - synthetic members, e.g. `hashCode`/`equals` for data classes
         *      - delegating members
         */
        val isDeclaration = callableSymbol.origin == KtSymbolOrigin.SOURCE || callableSymbol.origin == KtSymbolOrigin.LIBRARY
        if (isDeclaration) {
            return DRIWithOverridden(getDRIFromSymbol(callableSymbol), wasOverriddenBy)

        } else { // fake, synthetic, delegating
            val overriddenSymbols = callableSymbol.getAllOverriddenSymbols()
            return if (overriddenSymbols.isEmpty()) {
                DRIWithOverridden(getDRIFromSymbol(callableSymbol), wasOverriddenBy)
            } else {
                createDRIWithOverridden(overriddenSymbols.first())
            }
        }
    }

    private fun KtAnalysisSession.getDocumentation(symbol: KtSymbol) =
        if (symbol.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED)
            // a primary (implicit default) constructor  can be generated, so we need KDoc from @constructor tag
            getGeneratedKDocDocumentationFrom(symbol) ?: if(symbol is KtConstructorSymbol) getKDocDocumentationFrom(symbol, logger) else null
        else
            getKDocDocumentationFrom(symbol, logger) ?: javadocParser?.let { getJavaDocDocumentationFrom(symbol, it) }

    private fun KtAnalysisSession.isObvious(functionSymbol: KtFunctionSymbol, inheritedFrom: DRI?): Boolean {
        return functionSymbol.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED && !hasGeneratedKDocDocumentation(functionSymbol) ||
                inheritedFrom?.isObvious() == true
    }

    private fun DRI.isObvious(): Boolean = when (packageName) {
        "kotlin" -> classNames == "Any" || classNames == "Enum"
        "java.lang" -> classNames == "Object" || classNames == "Enum"
        else -> false
    }

    private fun KtSymbol.getSource() = KtPsiDocumentableSource(psi).toSourceSetDependent()

    private fun AncestryNode.exceptionInSupertypesOrNull(): ExceptionInSupertypes? =
        typeConstructorsBeingExceptions().takeIf { it.isNotEmpty() }
            ?.let { ExceptionInSupertypes(it.toSourceSetDependent()) }


    // ----------- Translators of modifiers ----------------------------------------------------------------------------
    private fun KtSymbolWithModality.getDokkaModality(): KotlinModifier {
        val isInterface = this is KtClassOrObjectSymbol && classKind == KtClassKind.INTERFACE
        return if (isInterface) {
            // only two modalities are possible for interfaces:
            //  - `SEALED` - when it's declared as `sealed interface`
            //  - `ABSTRACT` - when it's declared as `interface` or `abstract interface` (`abstract` is redundant but possible here)
            when (modality) {
                Modality.SEALED -> KotlinModifier.Sealed
                else -> KotlinModifier.Empty
            }
        } else {
            when (modality) {
                Modality.FINAL -> KotlinModifier.Final
                Modality.SEALED -> KotlinModifier.Sealed
                Modality.OPEN -> KotlinModifier.Open
                Modality.ABSTRACT -> KotlinModifier.Abstract
            }
        }
    }
    private fun KtSymbolWithVisibility.getDokkaVisibility() = visibility.toDokkaVisibility()
    private fun KtValueParameterSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.NoInline.takeIf { isNoinline },
        ExtraModifiers.KotlinOnlyModifiers.CrossInline.takeIf { isCrossinline },
        ExtraModifiers.KotlinOnlyModifiers.VarArg.takeIf { isVararg }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KtPropertyAccessorSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Inline.takeIf { isInline },
//ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        ExtraModifiers.KotlinOnlyModifiers.Override.takeIf { isOverride }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KtPropertySymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Const.takeIf { (this as? KtKotlinPropertySymbol)?.isConst == true },
        ExtraModifiers.KotlinOnlyModifiers.LateInit.takeIf { (this as? KtKotlinPropertySymbol)?.isLateInit == true },
        //ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        //ExtraModifiers.KotlinOnlyModifiers.External.takeIf { isExternal },
        //ExtraModifiers.KotlinOnlyModifiers.Static.takeIf { isStatic },
        ExtraModifiers.KotlinOnlyModifiers.Override.takeIf { isOverride }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KtJavaFieldSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isStatic }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KtFunctionSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Infix.takeIf { isInfix },
        ExtraModifiers.KotlinOnlyModifiers.Inline.takeIf { isInline },
        ExtraModifiers.KotlinOnlyModifiers.Suspend.takeIf { isSuspend },
        ExtraModifiers.KotlinOnlyModifiers.Operator.takeIf { isOperator },
//ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        ExtraModifiers.KotlinOnlyModifiers.TailRec.takeIf { (psi as? KtNamedFunction)?.hasModifier(KtTokens.TAILREC_KEYWORD) == true },
        ExtraModifiers.KotlinOnlyModifiers.External.takeIf { isExternal },
        ExtraModifiers.KotlinOnlyModifiers.Override.takeIf { isOverride }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KtNamedClassOrObjectSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Inline.takeIf { (this.psi as? KtClass)?.isInline() == true },
        ExtraModifiers.KotlinOnlyModifiers.Value.takeIf { (this.psi as? KtClass)?.isValue() == true },
        ExtraModifiers.KotlinOnlyModifiers.External.takeIf { isExternal },
        ExtraModifiers.KotlinOnlyModifiers.Inner.takeIf { isInner },
        ExtraModifiers.KotlinOnlyModifiers.Data.takeIf { isData },
        ExtraModifiers.KotlinOnlyModifiers.Fun.takeIf { isFun },
    ).toSet().takeUnless { it.isEmpty() }

    private fun org.jetbrains.kotlin.descriptors.Visibility.toDokkaVisibility(): Visibility = when (this) {
        Visibilities.Public -> KotlinVisibility.Public
        Visibilities.Protected -> KotlinVisibility.Protected
        Visibilities.Internal -> KotlinVisibility.Internal
        Visibilities.Private, Visibilities.PrivateToThis -> KotlinVisibility.Private
        JavaVisibilities.ProtectedAndPackage -> KotlinVisibility.Protected
        JavaVisibilities.ProtectedStaticVisibility -> KotlinVisibility.Protected
        JavaVisibilities.PackageVisibility -> JavaVisibility.Default
        else -> KotlinVisibility.Public
    }
}






