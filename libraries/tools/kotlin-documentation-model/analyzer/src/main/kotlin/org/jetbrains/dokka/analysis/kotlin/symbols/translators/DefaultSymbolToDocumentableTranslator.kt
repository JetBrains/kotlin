/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators


import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.analysis.java.util.PsiHelper
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.*
import com.intellij.psi.util.PsiLiteralUtil
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.java.BreakingAbstractionKotlinLightMethodChecker
import org.jetbrains.dokka.analysis.java.JavaAnalysisPlugin
import org.jetbrains.dokka.analysis.java.SyntheticElementDocumentationProvider
import org.jetbrains.dokka.analysis.java.parsers.JavadocParser
import org.jetbrains.dokka.analysis.java.util.PsiDocumentableSource
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.getGenerateJavaDocDocumentationFrom
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.getGeneratedKDocDocumentationFrom
import org.jetbrains.dokka.analysis.kotlin.symbols.services.KtPsiDocumentableSource
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.getJavaDocDocumentationFrom
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.getKDocDocumentationFrom
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.hasGeneratedKDocDocumentation
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.AnnotationTranslator.Companion.getPresentableName
import org.jetbrains.dokka.analysis.kotlin.symbols.utils.filterOutAny
import org.jetbrains.dokka.analysis.kotlin.symbols.utils.typeConstructorsBeingExceptions
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
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.kotlin.documentation.analyzer.ids.*
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.javaInterop.javaMethodName
import org.jetbrains.kotlin.analysis.api.scopes.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
import org.jetbrains.kotlin.psi.*

internal class DefaultSymbolToDocumentableTranslator(context: DokkaContext) : AsyncSourceToDocumentableTranslator {
    private val kotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }
    private val javadocParser = JavadocParser(
        docCommentParsers = context.plugin<JavaAnalysisPlugin>().query { docCommentParsers },
        docCommentFinder = context.plugin<JavaAnalysisPlugin>().docCommentFinder
    )
    private val lightMethodChecker = context.plugin<JavaAnalysisPlugin>().querySingle { kotlinLightMethodChecker }

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
            javadocParser = if(sourceSet.analysisPlatform == Platform.jvm) javadocParser else null,
            lightMethodChecker = lightMethodChecker
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
    private val javadocParser: JavadocParser? = null,
    lightMethodChecker: BreakingAbstractionKotlinLightMethodChecker,
) {
    private val psiHelper = PsiHelper(logger, lightMethodChecker)
    private val annotationTranslator = AnnotationTranslator(logger)
    private val typeTranslator = TypeTranslator(sourceSet, annotationTranslator, logger)

    private val syntheticJavaDocProvider = javadocParser?.let {
        SyntheticElementDocumentationProvider(it, analysisContext.getModule(sourceSet).project)
    }

    private fun <T> T.toSourceSetDependent() = if (this != null) mapOf(sourceSet to this) else emptyMap()

    private fun <T : KaSymbol> Sequence<T>.filterSymbolsInSourceSet(moduleKtFiles: Set<KtFile>, moduleJavaFiles: Set<PsiJavaFile>): Sequence<T> = filter {
        when (val file = it.psi?.containingFile) {
            is KtFile -> moduleKtFiles.contains(file) && file.containingDirectory?.name != "snippet-files"
            is PsiJavaFile -> moduleJavaFiles.contains(file) && file.containingDirectory.name != "snippet-files"
            else -> false
        }
    }

    fun visitModule(): DModule {
        val sourceModule = analysisContext.getModule(sourceSet)
        val sourceFiles = analysisContext.modulesWithFiles[sourceModule] ?: throw IllegalStateException("No source files for a source module ${sourceModule.name} of source set ${sourceSet.sourceSetID}")

        val ancestorKtFiles = sourceModule.transitiveDependsOnDependencies
            .flatMapTo(mutableSetOf()) { ancestorModule ->
                (analysisContext.modulesWithFiles[ancestorModule] ?: emptySet()).filterIsInstance<KtFile>()
            }
        val ktFiles = sourceFiles.filterIsInstance<KtFile>().toSet()
        val javaFiles = sourceFiles.filterIsInstance<PsiJavaFile>().toSet()

        val processedPackages: MutableSet<FqName> = mutableSetOf()
        return analyze(sourceModule) {
            fun <T> Set<T>.collectPackages(getPackageFqName: (T) -> FqName): List<DPackage> =
                this.mapNotNull { item ->
                    val packageFqName = getPackageFqName(item)
                    if (processedPackages.contains(packageFqName)) {
                        return@mapNotNull null
                    }
                    processedPackages.add(packageFqName)
                    findPackage(packageFqName)?.let { packageSymbol ->
                        visitPackageSymbol(packageSymbol, ktFiles, ancestorKtFiles, javaFiles)
                    }
                }

            val packages = (ktFiles + ancestorKtFiles).collectPackages { it.packageFqName } + javaFiles.collectPackages { FqName(it.packageName) }

            DModule(
                name = moduleName,
                packages = packages,
                documentation = emptyMap(),
                expectPresentInSet = null,
                sourceSets = setOf(sourceSet)
            )
        }
    }

    context(_: KaSession)
    private fun visitPackageSymbol(
        packageSymbol: KaPackageSymbol,
        moduleKtFiles: Set<KtFile>,
        ancestorKtFiles: Set<KtFile>,
        moduleJavaFiles: Set<PsiJavaFile>
    ): DPackage {
        val declarationId = generateDeclarationId(packageSymbol)
        val scope = packageSymbol.packageScope
        val callables = scope.callables.filterSymbolsInSourceSet(moduleKtFiles, moduleJavaFiles).toList()
        val classifiers = scope.classifiers.filterSymbolsInSourceSet(moduleKtFiles + ancestorKtFiles, moduleJavaFiles).toList()

        val functions = callables.filterIsInstance<KaNamedFunctionSymbol>().map { visitFunctionSymbol(it, declarationId, isJavaContext = it.isJavaSource()) }
        val properties = callables.filterIsInstance<KaPropertySymbol>().map { visitPropertySymbol(it, declarationId, isJavaContext = it.isJavaSource()) }
        val classlikes =
            classifiers.filterIsInstance<KaNamedClassSymbol>()
                .map { visitClassSymbol(it) }
        val typealiases = classifiers.filterIsInstance<KaTypeAliasSymbol>()
            .filter { it.psi?.containingFile !in ancestorKtFiles }
            .map { visitTypeAliasSymbol(it) }

        val packageInfo = moduleJavaFiles.singleOrNull { it.name == "package-info.java" }
        val documentation = packageInfo?.let {
            javadocParser?.parseDocumentation(it, sourceSet).toSourceSetDependent()
        }.orEmpty()

        val packageAnnotations = packageInfo?.packageStatement?.annotationList?.annotations

        return DPackage(
            declarationId = declarationId,
            functions = functions,
            properties = properties,
            classlikes = classlikes,
            typealiases = typealiases,
            documentation = documentation,
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(
                psiHelper.convertAnnotations(packageAnnotations?.toList().orEmpty()).toSourceSetDependent().toAnnotations()
            )
        )
    }

    context(_: KaSession)
    private fun visitTypeAliasSymbol(
        typeAliasSymbol: KaTypeAliasSymbol
    ): DTypeAlias = withExceptionCatcher(typeAliasSymbol) {
        val name = typeAliasSymbol.name.asString()
        val declarationId = generateDeclarationId(typeAliasSymbol)

        val ancestryInfo = with(typeTranslator) { buildAncestryInformationFrom(typeAliasSymbol.expandedType, Location(typeAliasSymbol)) }

        val generics =
            typeAliasSymbol.typeParameters.mapIndexed { index, symbol -> visitVariantTypeParameter(index, symbol, declarationId) }

        return DTypeAlias(
            declarationId = declarationId,
            name = name,
            type = GenericTypeConstructor(
                declarationId = typeAliasSymbol.classId?.let { KdElementId(it.escape()) }
                    ?: throw IllegalStateException("Non-local type alias has no ClassId: ${typeAliasSymbol.name.asString()}"),
                projections = generics.map { it.variantTypeParameter }), // this property can be removed in DTypeAlias
            expectPresentInSet = null,
            underlyingType = toBoundFrom(typeAliasSymbol.expandedType, typeAliasSymbol).toSourceSetDependent(),
            visibility = getDokkaVisibility(typeAliasSymbol, typeAliasSymbol.isJavaSource()).toSourceSetDependent(),
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

    context(_: KaSession)
    fun visitClassSymbol(
        namedClassSymbol: KaNamedClassSymbol
    ): DClasslike = withExceptionCatcher(namedClassSymbol) {
        val name = namedClassSymbol.name.asString()
        val declarationId = generateDeclarationId(namedClassSymbol)

        val isExpect = namedClassSymbol.isExpect
        val isActual = namedClassSymbol.isActual
        val documentation = getDocumentation(namedClassSymbol)?.toSourceSetDependent() ?: emptyMap()

        val (constructors, functions, properties, classlikesWithoutCompanion, typeAliases) = getDokkaScopeFrom(namedClassSymbol, declarationId, isJavaContext = namedClassSymbol.isJavaSource())

        val companionObject = namedClassSymbol.companionObject?.let {
            visitClassSymbol(
                it
            )
        } as? DObject
        val classlikes = if (companionObject == null) classlikesWithoutCompanion else classlikesWithoutCompanion + companionObject

        val generics = namedClassSymbol.typeParameters.mapIndexed { index, symbol ->
            visitVariantTypeParameter(
                index,
                symbol,
                declarationId
            )
        }

        val ancestryInfo =
            with(typeTranslator) { buildAncestryInformationFrom(namedClassSymbol.defaultType, Location(namedClassSymbol)) }
        val supertypes =
            namedClassSymbol.superTypes.asSequence().filterOutAny()
                .map { with(typeTranslator) { toTypeConstructorWithKindFrom(it, Location(namedClassSymbol)) } }
                .toList()
                .toSourceSetDependent()
        return@withExceptionCatcher when (namedClassSymbol.classKind) {
            KaClassKind.OBJECT, KaClassKind.COMPANION_OBJECT ->
                DObject(
                    declarationId = declarationId,
                    name = name,
                    functions = functions,
                    properties = properties,
                    classlikes = classlikes,
                    typealiases = typeAliases,
                    sources = getSource(namedClassSymbol),
                    expectPresentInSet = sourceSet.takeIf { isExpect },
                    visibility = getDokkaVisibility(namedClassSymbol, namedClassSymbol.isJavaSource()).toSourceSetDependent(),
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
                declarationId = declarationId,
                name = name,
                constructors = constructors,
                functions = functions,
                properties = properties,
                classlikes = classlikes,
                typealiases = typeAliases,
                sources = getSource(namedClassSymbol),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                visibility = getDokkaVisibility(namedClassSymbol, namedClassSymbol.isJavaSource()).toSourceSetDependent(),
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
                declarationId = declarationId,
                name = name,
                functions = functions,
                properties = properties,
                classlikes = classlikes,
                typealiases = typeAliases,
                sources = getSource(namedClassSymbol),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                visibility = getDokkaVisibility(namedClassSymbol, namedClassSymbol.isJavaSource()).toSourceSetDependent(),
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
                declarationId = declarationId,
                name = name,
                documentation = documentation,
                functions = functions,
                properties = properties,
                classlikes = classlikes,
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                companion = companionObject,
                visibility = getDokkaVisibility(namedClassSymbol, namedClassSymbol.isJavaSource()).toSourceSetDependent(),
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
                    getDokkaScopeFrom(namedClassSymbol, declarationId, includeStaticScope = false, isJavaContext = namedClassSymbol.isJavaSource()).let {
                        it.copy(
                            functions = it.functions.map { it.withNewExtras( it.extra + InheritedMember(declarationId.toSourceSetDependent())) },
                            properties = it.properties.map { it.withNewExtras( it.extra + InheritedMember(declarationId.toSourceSetDependent())) }
                        )
                    }
                }

                val entries =
                    namedClassSymbol.staticDeclaredMemberScope.callables.filterIsInstance<KaEnumEntrySymbol>().map {
                        visitEnumEntrySymbol(it, enumEntryScope.value)
                    }.toList()

                DEnum(
                    declarationId = declarationId,
                    name = name,
                    entries = entries,
                    constructors = constructors,
                    functions = functions,
                    properties = properties,
                    classlikes = classlikes,
                    typealiases = typeAliases,
                    sources = getSource(namedClassSymbol),
                    expectPresentInSet = sourceSet.takeIf { isExpect },
                    visibility = getDokkaVisibility(namedClassSymbol, namedClassSymbol.isJavaSource()).toSourceSetDependent(),
                    supertypes = supertypes,
                    documentation = documentation,
                    companion = namedClassSymbol.companionObject?.let {
                        visitClassSymbol(
                            it
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
    context(_: KaSession)
    private fun getDokkaScopeFrom(
        namedClassOrObjectSymbol: KaNamedClassSymbol,
        ownerDeclarationId: KdElementId,
        includeStaticScope: Boolean = true,
        isJavaContext: Boolean
    ): DokkaScope {
        // getCombinedMemberScope additionally includes a static scope, see [getCombinedMemberScope]
        // e.g. getStaticMemberScope contains `valueOf`, `values` and `entries` members for Enum
        val scope = if(includeStaticScope) namedClassOrObjectSymbol.combinedMemberScope else namedClassOrObjectSymbol.memberScope
        val constructors = scope.constructors.map { visitConstructorSymbol(it, isJavaContext) }.toList()

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
            if ((fn.origin == KaSymbolOrigin.JAVA_SOURCE || fn.origin == KaSymbolOrigin.JAVA_LIBRARY) && fn.psi != null)
                syntheticJavaProperties.any { fn.psi == it.javaGetterSymbol.psi || fn.psi == it.javaSetterSymbol?.psi }
            else false
        }

        val functions = callables.filterIsInstance<KaNamedFunctionSymbol>()
            .filterOutSyntheticJavaPropAccessors().map { visitFunctionSymbol(it, ownerDeclarationId, isJavaContext) }


        val properties = callables.filterIsInstance<KaPropertySymbol>().map { visitPropertySymbol(it, ownerDeclarationId, isJavaContext) } +
                syntheticJavaProperties.map { visitPropertySymbol(it, ownerDeclarationId, isJavaContext) } +
                javaFields.map { visitJavaFieldSymbol(it, ownerDeclarationId, isJavaContext) }

        val typealiases = classifiers.filterIsInstance<KaTypeAliasSymbol>()
            .map { visitTypeAliasSymbol(it) }

        fun List<KaNamedClassSymbol>.filterOutCompanion() =
                filterNot {
                    it.classKind == KaClassKind.COMPANION_OBJECT
                }

        val classlikes = classifiers.filterIsInstance<KaNamedClassSymbol>()
            .filterOutCompanion()
            .map { visitClassSymbol(it) }

        return DokkaScope(
            constructors = constructors,
            functions = functions,
            properties = properties,
            classlikesWithoutCompanion = classlikes,
            typeAliases = typealiases,
        )
    }

    context(_: KaSession)
    private fun visitEnumEntrySymbol(
        enumEntrySymbol: KaEnumEntrySymbol, scope: DokkaScope
    ): DEnumEntry = withExceptionCatcher(enumEntrySymbol) {
        val declarationId = generateDeclarationId(enumEntrySymbol)
        val isExpect = false

        return DEnumEntry(
            declarationId = declarationId,
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

    context(_: KaSession)
    private fun visitPropertySymbol(propertySymbol: KaPropertySymbol, parentDeclarationId: KdElementId, isJavaContext: Boolean): DProperty =
        withExceptionCatcher(propertySymbol) {
            val declarationId = declarationIdWithOverridden(propertySymbol)
            val inheritedFrom = inheritedFromDeclarationId(propertySymbol, parentDeclarationId)
            val (isExpect, isActual) = when (propertySymbol) {
                is KaKotlinPropertySymbol -> propertySymbol.isExpect to propertySymbol.isActual
                is KaSyntheticJavaPropertySymbol -> false to false
            }
            val generics =
                propertySymbol.typeParameters.mapIndexed { index, symbol ->
                    visitVariantTypeParameter(
                        index,
                        symbol,
                        declarationId
                    )
                }

            // java inheriting kotlin with @JvmField
            val isJvmField = isJavaContext && propertySymbol.backingFieldSymbol?.annotations?.any {
                it.classId == JvmStandardClassIds.Annotations.JvmField
            } == true

            return DProperty(
                declarationId = declarationId,
                name = propertySymbol.name.asString(),
                receiver = propertySymbol.receiverParameter?.let {
                    visitReceiverParameter(
                        it,
                        declarationId
                    )
                },
                contextParameters = @OptIn(KaExperimentalApi::class) propertySymbol.contextParameters
                    .mapIndexed { index, symbol -> visitContextParameter(index, symbol, declarationId) },
                sources = getSource(propertySymbol),
                getter = if (isJvmField) null else propertySymbol.getter?.let { visitPropertyAccessor(it, propertySymbol, declarationId, inheritedFrom, isJavaContext) },
                setter = if (isJvmField) null else propertySymbol.setter?.let { visitPropertyAccessor(it, propertySymbol, declarationId, inheritedFrom, isJavaContext) },
                visibility = getDokkaVisibility(propertySymbol, isJavaContext).toSourceSetDependent(),
                documentation = getDocumentation(propertySymbol)?.toSourceSetDependent() ?: emptyMap(), // TODO
                modifier = propertySymbol.getDokkaModality().toSourceSetDependent(),
                type = toBoundFrom(propertySymbol.returnType, propertySymbol),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sourceSets = setOf(sourceSet),
                generics = generics,
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    additionalExtrasOfProperty(propertySymbol)?.toSourceSetDependent()?.toAdditionalModifiers(),
                    overriddenMembers(propertySymbol),
                    getDokkaAnnotationsFrom(propertySymbol)?.toSourceSetDependent()?.toAnnotations(),
                    propertySymbol.getDefaultValue()?.let { DefaultValue(it.toSourceSetDependent()) },
                    inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                    takeUnless { propertySymbol.isVal }?.let { IsVar },
                    takeIf { (propertySymbol as? KaKotlinPropertySymbol)?.primaryConstructorParameter != null &&
                            // a property can be from a constructor of a super class
                            inheritedFrom == null }?.let {

                        IsAlsoParameter(listOf(sourceSet))
                    },
                    @OptIn(ExperimentalDokkaApi::class) IsCompanion.takeIf { @OptIn(KaExperimentalApi::class) propertySymbol.isCompanion },
                )
            )
        }

    context(_: KaSession)
    private fun visitJavaFieldSymbol(
        javaFieldSymbol: KaJavaFieldSymbol,
        parentDeclarationId: KdElementId,
        isJavaContext: Boolean
    ): DProperty =
        withExceptionCatcher(javaFieldSymbol) {
            val declarationId = declarationIdWithOverridden(javaFieldSymbol)
            val inheritedFrom = inheritedFromDeclarationId(javaFieldSymbol, parentDeclarationId)
            val isExpect = false
            val isActual = false

            return DProperty(
                declarationId = declarationId,
                name = javaFieldSymbol.name.asString(),
                receiver = javaFieldSymbol.receiverParameter?.let {
                    visitReceiverParameter(
                        it,
                        declarationId
                    )
                },
                sources = getSource(javaFieldSymbol),
                getter = null,
                setter = null,
                visibility = getDokkaVisibility(javaFieldSymbol, isJavaContext).toSourceSetDependent(),
                documentation = getDocumentation(javaFieldSymbol)?.toSourceSetDependent() ?: emptyMap(), // TODO
                modifier = javaFieldSymbol.getDokkaModality().toSourceSetDependent(),
                type = toBoundFrom(javaFieldSymbol.returnType, javaFieldSymbol),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sourceSets = setOf(sourceSet),
                generics = emptyList(),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    javaFieldSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(javaFieldSymbol)?.toSourceSetDependent()?.toAnnotations(),
                    getJavaConstantExpression(javaFieldSymbol)?.let { DefaultValue(it.toSourceSetDependent()) },
                    inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                    // non-final java property should be var
                    takeUnless { javaFieldSymbol.isVal }?.let { IsVar },
                    @OptIn(ExperimentalDokkaApi::class) IsCompanion.takeIf { @OptIn(KaExperimentalApi::class) javaFieldSymbol.isCompanion },
                )
            )
        }

    context(_: KaSession)
    private fun visitPropertyAccessor(
        propertyAccessorSymbol: KaPropertyAccessorSymbol,
        propertySymbol: KaPropertySymbol,
        propertyDeclarationId: KdElementId,
        propertyInheritedFrom: KdElementId?,
        isJavaContext: Boolean
    ): DFunction = withExceptionCatcher(propertyAccessorSymbol) {
        val isGetter = propertyAccessorSymbol is KaPropertyGetterSymbol
        // it also covers @JvmName annotation
        @OptIn(KaExperimentalApi::class)
        // `javaMethodName` is null when there is no addressable JVM method (e.g. accessors of `const` or `@JvmField` properties),
        val name = propertyAccessorSymbol.javaMethodName
            ?: propertySymbol.name.asString()
                .let { if (isGetter) JvmAbi.getterName(it) else JvmAbi.setterName(it) }

        // Property accessors are not declarations and have no ID of their own (see `generateDeclarationId`):
        // both accessors are identified by their property's ID.
        val declarationId = propertyDeclarationId

        val inheritedFrom = if(propertyAccessorSymbol.origin == KaSymbolOrigin.JAVA_SYNTHETIC_PROPERTY) ownerDeclarationIdOf(propertyDeclarationId) else propertyInheritedFrom

        @OptIn(KaExperimentalApi::class) // due to typeParameters
        val generics = propertyAccessorSymbol.typeParameters.mapIndexed { index, symbol ->
            visitVariantTypeParameter(
                index,
                symbol,
                declarationId
            )
        }

        return DFunction(
            declarationId = declarationId,
            name = name,
            isConstructor = false,
            parameters = propertyAccessorSymbol.valueParameters
                .mapIndexed { index, symbol -> visitValueParameter(index, symbol, declarationId) },
            contextParameters = emptyList(),
            receiver = propertyAccessorSymbol.receiverParameter?.let {
                visitReceiverParameter(
                    it,
                    declarationId
                )
            },
            expectPresentInSet = null,
            sources = getSource(propertyAccessorSymbol),
            visibility = getDokkaVisibility(propertyAccessorSymbol, isJavaContext).toSourceSetDependent(),
            generics = generics,
            documentation = getAccessorSymbolDocumentation(propertyAccessorSymbol)?.toSourceSetDependent() ?: emptyMap(),
            modifier = propertyAccessorSymbol.getDokkaModality().toSourceSetDependent(),
            type = toBoundFrom(propertyAccessorSymbol.returnType, propertyAccessorSymbol),
            sourceSets = setOf(sourceSet),
            isExpectActual = false,
            extra = PropertyContainer.withAll(
                propertyAccessorSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                getDokkaAnnotationsFrom(propertyAccessorSymbol)?.toSourceSetDependent()?.toAnnotations()
            )
        )
    }

    context(_: KaSession)
    private fun visitConstructorSymbol(
        constructorSymbol: KaConstructorSymbol,
        isJavaContext: Boolean
    ): DFunction = withExceptionCatcher(constructorSymbol) {
        val name = constructorSymbol.containingClassId?.shortClassName?.asString()
            ?: throw IllegalStateException("Unknown containing class of constructor")
        val declarationId = generateDeclarationId(constructorSymbol)
        val isExpect = constructorSymbol.isExpect
        val isActual = constructorSymbol.isActual

        val generics = constructorSymbol.typeParameters.mapIndexed { index, symbol ->
            visitVariantTypeParameter(
                index,
                symbol,
                declarationId
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
            declarationId = declarationId,
            name = name,
            isConstructor = true,
            receiver = constructorSymbol.receiverParameter?.let {
                visitReceiverParameter(
                    it,
                    declarationId
                )
            },
            parameters = constructorSymbol.valueParameters
                .mapIndexed { index, symbol -> visitValueParameter(index, symbol, declarationId) },
            expectPresentInSet = sourceSet.takeIf { isExpect },
            sources = getSource(constructorSymbol),
            visibility = getDokkaVisibility(constructorSymbol, isJavaContext).toSourceSetDependent(),
            generics = generics,
            documentation = documentation ?: emptyMap(),
            modifier = KotlinModifier.Empty.toSourceSetDependent(),
            type = toBoundFrom(constructorSymbol.returnType, constructorSymbol),
            sourceSets = setOf(sourceSet),
            isExpectActual = (isExpect || isActual),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(constructorSymbol)?.toSourceSetDependent()?.toAnnotations(),
                getCheckedExceptions(constructorSymbol),
                takeIf { constructorSymbol.isPrimary }?.let { PrimaryConstructorExtra }
            )
        )
    }

    context(_: KaSession)
    private fun visitFunctionSymbol(functionSymbol: KaNamedFunctionSymbol, parentDeclarationId: KdElementId, isJavaContext: Boolean): DFunction =
        withExceptionCatcher(functionSymbol) {
            val declarationId = declarationIdWithOverridden(functionSymbol)
            val inheritedFrom = inheritedFromDeclarationId(functionSymbol, parentDeclarationId)
            val isExpect = functionSymbol.isExpect
            val isActual = functionSymbol.isActual
            val generics =
                functionSymbol.typeParameters.mapIndexed { index, symbol ->
                    visitVariantTypeParameter(
                        index,
                        symbol,
                        declarationId
                    )
                }

            return DFunction(
                declarationId = declarationId,
                name = functionSymbol.name.asString(),
                isConstructor = false,
                receiver = functionSymbol.receiverParameter?.let {
                    visitReceiverParameter(
                        it,
                        declarationId
                    )
                },
                parameters = functionSymbol.valueParameters
                    .mapIndexed { index, symbol -> visitValueParameter(index, symbol, declarationId) },
                contextParameters = @OptIn(KaExperimentalApi::class) functionSymbol.contextParameters
                    .mapIndexed { index, symbol -> visitContextParameter(index, symbol, declarationId) },
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sources = getSource(functionSymbol),
                visibility = getDokkaVisibility(functionSymbol, isJavaContext).toSourceSetDependent(),
                generics = generics,
                documentation = getDocumentation(functionSymbol)?.toSourceSetDependent() ?: emptyMap(),
                modifier = functionSymbol.getDokkaModality().toSourceSetDependent(),
                type = toBoundFrom(functionSymbol.returnType, functionSymbol),
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                    overriddenMembers(functionSymbol),
                    additionalExtrasOfFunction(functionSymbol)?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(functionSymbol)
                        ?.toSourceSetDependent()?.toAnnotations(),
                    ObviousMember.takeIf { isObvious(functionSymbol, inheritedFrom) },
                    getCheckedExceptions(functionSymbol),
                    @OptIn(ExperimentalDokkaApi::class) IsCompanion.takeIf { @OptIn(KaExperimentalApi::class) functionSymbol.isCompanion },
                )
            )
        }

    private fun getJavaConstantExpression(javaFieldSymbol: KaJavaFieldSymbol): Expression? {
        val psiField = javaFieldSymbol.psi as? PsiField ?: return null
        return psiHelper.getConstantExpression(psiField)
    }

    context(_: KaSession)
    private fun getCheckedExceptions(functionSymbol: KaFunctionSymbol): CheckedExceptions? {
        val psiMethod = functionSymbol.psi as? PsiMethod ?: return null
        val exceptionIds = psiHelper.getCheckedExceptionDeclarationIds(psiMethod)
        return exceptionIds.takeIf { it.isNotEmpty() }?.let { CheckedExceptions(it.toSourceSetDependent()) }
    }

    context(_: KaSession)
    private fun visitValueParameter(
        index: Int, valueParameterSymbol: KaValueParameterSymbol, ownerDeclarationId: KdElementId
    ) = DParameter(
        declarationId = valueParameterDeclarationId(ownerDeclarationId, index),
        name = valueParameterSymbol.name.asString(),
        type = toBoundFrom(valueParameterSymbol.returnType, valueParameterSymbol),
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
    context(_: KaSession)
    private fun visitContextParameter(
        index: Int, contextParameterSymbol: KaContextParameterSymbol, ownerDeclarationId: KdElementId
    ): DParameter {
        return DParameter(
            declarationId = contextParameterDeclarationId(ownerDeclarationId, index),
            name = if(contextParameterSymbol.name == UNDERSCORE_FOR_UNUSED_VAR) "_" else contextParameterSymbol.name.asString(),
            type = toBoundFrom(contextParameterSymbol.returnType, contextParameterSymbol),
            expectPresentInSet = null,
            documentation = getDocumentation(contextParameterSymbol)?.toSourceSetDependent() ?: emptyMap(),
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(
               getDokkaAnnotationsFrom(contextParameterSymbol)?.toSourceSetDependent()?.toAnnotations(),
            )
        )
    }

    context(_: KaSession)
    private fun visitReceiverParameter(
        receiverParameterSymbol: KaReceiverParameterSymbol, ownerDeclarationId: KdElementId
    ) = DParameter(
        declarationId = receiverDeclarationId(ownerDeclarationId),
        name = null,
        type = toBoundFrom(receiverParameterSymbol.returnType, receiverParameterSymbol),
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
    context(_: KaSession)
    private fun getDefaultValue(symbol: KaValueParameterSymbol, parameterIndex: Int): Expression? {
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
        (initializer?.initializerPsi as? KtConstantExpression)?.toDefaultValueExpression() // TODO #4185

    private fun KtExpression.toDefaultValueExpression(): Expression? = when (node?.elementType) {
        KtNodeTypes.INTEGER_CONSTANT -> PsiLiteralUtil.parseLong(node?.text)?.let { IntegerConstant(it) }
        KtNodeTypes.FLOAT_CONSTANT -> if (node?.text?.lowercase()?.endsWith('f') == true)
            PsiLiteralUtil.parseFloat(node?.text)?.let { FloatConstant(it) }
        else PsiLiteralUtil.parseDouble(node?.text)?.let { DoubleConstant(it) }

        KtNodeTypes.BOOLEAN_CONSTANT -> BooleanConstant(node?.text == "true")
        KtNodeTypes.STRING_TEMPLATE -> StringConstant(node.findChildByType(KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY)?.text.orEmpty())
        else -> node?.text?.let { ComplexExpression(it) }
    }

    context(_: KaSession)
    private fun visitVariantTypeParameter(
        index: Int,
        typeParameterSymbol: KaTypeParameterSymbol,
        ownerDeclarationId: KdElementId
    ): DTypeParameter {
        val upperBoundsOrNullableAny =
            typeParameterSymbol.upperBounds.takeIf { it.isNotEmpty() } ?: listOf(builtinTypes.nullableAny)
        return DTypeParameter(
            declarationId = typeParameterDeclarationId(ownerDeclarationId, index),
            variantTypeParameter = TypeParameter(
                containerDeclarationId = ownerDeclarationId,
                name = typeParameterSymbol.name.asString(),
                presentableName = typeParameterSymbol.getPresentableName()
            ).wrapWithVariance(typeParameterSymbol.variance),
            documentation = getDocumentation(typeParameterSymbol)?.toSourceSetDependent() ?: emptyMap(),
            expectPresentInSet = null,
            bounds = upperBoundsOrNullableAny.map { toBoundFrom(it, typeParameterSymbol) },
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(
                typeParameterSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                getDokkaAnnotationsFrom(typeParameterSymbol)?.toSourceSetDependent()?.toAnnotations()
            )
        )
    }
    // ----------- Utils ----------------------------------------------------------------------------

    context(_: KaSession)
    private fun getDokkaAnnotationsFrom(annotated: KaAnnotated): List<Annotations.Annotation>? {
        // TODO: this is only relevant for mapped annotations: java.Deprecated is converted to kotlin.Deprecated by AA (in FIR)
        // For Java symbols, extract annotations via PSI to preserve Java annotation class IDs
        // (e.g., java.lang.Deprecated instead of kotlin.Deprecated)
        if (annotated is KaSymbol && annotated.isJavaSource()) {
            val psiElement = annotated.psi as? com.intellij.psi.PsiModifierListOwner
            if (psiElement != null) {
                return psiHelper.convertAnnotations(psiElement.annotations.toList()).takeUnless { it.isEmpty() }
            }
        }
        return with(annotationTranslator) { getAllAnnotationsFrom(annotated) }.takeUnless { it.isEmpty() }
    }

    @OptIn(KaExperimentalApi::class)
    context(_: KaSession)
    private fun toBoundFrom(type: KaType, containingSymbol: KaSymbol): Bound {
        return with(typeTranslator) { toBoundFrom(type, Location(containingSymbol)) }
    }

    /**
     * The classlike that actually
     * declares the member (the owner part of its ID, after fake-override normalization), or `null` if it is
     * declared in [parentDeclarationId] itself.
     */
    context(_: KaSession)
    private fun inheritedFromDeclarationId(symbol: KaCallableSymbol, parentDeclarationId: KdElementId): KdElementId? =
        ownerDeclarationIdOf(declarationIdWithOverridden(symbol)).takeIf { it != parentDeclarationId }

    /**
     * The declaration ID of the member a callable symbol resolves to: fake overrides (substitution/intersection),
     * synthetic and delegating members inherit the ID of the declaration they are a copy of, so inherited
     * copies share the declaring member's ID (see [generateDeclarationIdWithOverridden]).
     */
    context(_: KaSession)
    private fun declarationIdWithOverridden(callableSymbol: KaCallableSymbol): KdElementId =
        generateDeclarationIdWithOverridden(callableSymbol)

    context(_: KaSession)
    private fun overriddenMembers(symbol: KaCallableSymbol): OverriddenMembers {
        // Inherited copies share the declaring member's ID and therefore its override relationships.
        val declaration = if (symbol.origin == KaSymbolOrigin.SUBSTITUTION_OVERRIDE ||
            symbol.origin == KaSymbolOrigin.INTERSECTION_OVERRIDE
        ) {
            symbol.allOverriddenSymbols.firstOrNull {
                it.origin != KaSymbolOrigin.SUBSTITUTION_OVERRIDE && it.origin != KaSymbolOrigin.INTERSECTION_OVERRIDE
            } ?: symbol
        } else symbol
        val members = declaration.directlyOverriddenSymbols.mapNotNull { overridden ->
            when (overridden) {
                is KaNamedFunctionSymbol, is KaPropertySymbol -> declarationIdWithOverridden(overridden)
                else -> null
            }
        }.distinct().toList()
        return OverriddenMembers(members.toSourceSetDependent())
    }


    context(_: KaSession)
    private fun getAccessorSymbolDocumentation(symbol: KaPropertyAccessorSymbol): DocumentationNode? {
        val documentation = getDocumentation(symbol) ?: return null
        return documentation.removePropertyTag()
    }

    context(_: KaSession)
    private fun getDocumentation(symbol: KaSymbol) = when(symbol.origin) {
        KaSymbolOrigin.SOURCE_MEMBER_GENERATED -> {
            if (symbol.containingSymbol?.isJavaSource() == true)
                syntheticJavaDocProvider?.let { getGenerateJavaDocDocumentationFrom(symbol, it, sourceSet) }
            else
            // a primary (implicit default) constructor  can be generated, so we need KDoc from @constructor tag
                getGeneratedKDocDocumentationFrom(symbol)
                    ?: if (symbol is KaConstructorSymbol) getKDocDocumentationFrom(symbol, logger, sourceSet) else null
        }
        KaSymbolOrigin.JAVA_SOURCE, KaSymbolOrigin.JAVA_LIBRARY -> javadocParser?.let { getJavaDocDocumentationFrom(symbol, it, sourceSet) }
        else -> getKDocDocumentationFrom(symbol, logger, sourceSet) ?: javadocParser?.let { getJavaDocDocumentationFrom(symbol, it, sourceSet) }
    }

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

    private fun KaSymbol.isJavaSource() = origin == KaSymbolOrigin.JAVA_SOURCE || origin == KaSymbolOrigin.JAVA_LIBRARY

    context(_: KaSession)
    private fun isObvious(functionSymbol: KaFunctionSymbol, inheritedFrom: KdElementId?): Boolean {
        return functionSymbol.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED && !hasGeneratedKDocDocumentation(functionSymbol) ||
                inheritedFrom?.isObvious() == true
    }

    /** Whether the declaring classlike (by declaration ID) is `kotlin.Any`/`kotlin.Enum` or their Java mappings. */
    private fun KdElementId.isObvious(): Boolean =
        this == KdElementId("kotlin/Any") || this == KdElementId("kotlin/Enum") ||
                this == KdElementId("java.lang/Object") || this == KdElementId("java.lang/Enum")

    context(_: KaSession)
    private fun getSource(symbol: KaSymbol): SourceSetDependent<DocumentableSource> {
        val psi = when(symbol) {
            // implicit/default accessors have no psi
            is KaPropertyAccessorSymbol -> symbol.containingSymbol?.psi
            else -> symbol.psi
        }

        return if (symbol.isJavaSource())
            (psi as? PsiNamedElement)?.let { PsiDocumentableSource(it) }?.toSourceSetDependent() ?: emptyMap()
        else KtPsiDocumentableSource(psi).toSourceSetDependent()
    }

    private fun AncestryNode.exceptionInSupertypesOrNull(): ExceptionInSupertypes? =
        typeConstructorsBeingExceptions().takeIf { it.isNotEmpty() }
            ?.let { ExceptionInSupertypes(it.toSourceSetDependent()) }


    // ----------- Translators of modifiers ----------------------------------------------------------------------------
    /**
     * Dokka has its own conditions for the keyword `override`
     */
    context(_: KaSession)
    private fun isDokkaOverride(symbol: KaCallableSymbol): Boolean {
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

    private fun KaDeclarationSymbol.getDokkaModality(): Modifier {
        val isInterface = this is KaClassSymbol && classKind == KaClassKind.INTERFACE

        return if (isJavaSource()) {
            if (isInterface) {
                // Java interface can't have modality modifiers except for "sealed", which is not supported yet in Dokka
                JavaModifier.Empty
            } else when (modality) {
                KaSymbolModality.ABSTRACT -> JavaModifier.Abstract
                KaSymbolModality.FINAL -> JavaModifier.Final
                else -> JavaModifier.Empty
            }
        } else {
            if (isInterface) {
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
    }

    /**
     * Determines visibility for a symbol based on the documented class context.
     * [isJavaContext] should reflect whether the **documented class** is a Java source,
     * not the declaring class. This ensures Java members inherited into Kotlin classes
     * use Kotlin visibility (matching how Kotlin developers see them).
     */
    private fun getDokkaVisibility(symbol: KaDeclarationSymbol, isJavaContext: Boolean): Visibility =
        symbol.visibility.toDokkaVisibility(isJavaSource = isJavaContext)
    private fun KaValueParameterSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.NoInline.takeIf { isNoinline },
        ExtraModifiers.KotlinOnlyModifiers.CrossInline.takeIf { isCrossinline },
        ExtraModifiers.KotlinOnlyModifiers.VarArg.takeIf { isVararg }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KaTypeParameterSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Reified.takeIf { isReified }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KaPropertyAccessorSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Inline.takeIf { isInline },
//ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        ExtraModifiers.KotlinOnlyModifiers.Override.takeIf { isOverride }
    ).toSet().takeUnless { it.isEmpty() }


    context(_: KaSession)
    private fun additionalExtrasOfProperty(symbol: KaPropertySymbol) = with(symbol) {
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

    context(_: KaSession)
    private fun additionalExtrasOfFunction(symbol: KaNamedFunctionSymbol) = with(symbol) {
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
            },
            ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isStatic },
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

    private fun KaSymbolVisibility.toDokkaVisibility(isJavaSource: Boolean = false): Visibility = when (this) {
        KaSymbolVisibility.PUBLIC -> if (isJavaSource) JavaVisibility.Public else KotlinVisibility.Public
        KaSymbolVisibility.PROTECTED -> if (isJavaSource) JavaVisibility.Protected else KotlinVisibility.Protected
        KaSymbolVisibility.INTERNAL -> KotlinVisibility.Internal
        KaSymbolVisibility.PRIVATE -> if (isJavaSource) JavaVisibility.Private else KotlinVisibility.Private
        KaSymbolVisibility.PACKAGE_PROTECTED -> if (isJavaSource) JavaVisibility.Protected else KotlinVisibility.Protected
        KaSymbolVisibility.PACKAGE_PRIVATE -> JavaVisibility.Default
        KaSymbolVisibility.UNKNOWN, KaSymbolVisibility.LOCAL -> if (isJavaSource) JavaVisibility.Public else KotlinVisibility.Public
    }
}
