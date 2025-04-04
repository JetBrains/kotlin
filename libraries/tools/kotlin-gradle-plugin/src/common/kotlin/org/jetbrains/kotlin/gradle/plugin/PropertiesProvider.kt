/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerArgumentsLogLevel
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.dsl.NativeCacheOrchestration
import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.internal.properties.PropertiesBuildService
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler.Companion.IGNORE_TCSM_OVERFLOW
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.Companion.jsCompilerProperty
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_CLASSLOADER_CACHE_TIMEOUT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_COMPILER_KEEP_INCREMENTAL_COMPILATION_CACHES_IN_MEMORY
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_COMPILER_USE_PRECISE_COMPILATION_RESULTS_BACKUP
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_CREATE_ARCHIVE_TASKS_FOR_CUSTOM_COMPILATIONS
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_CREATE_DEFAULT_MULTIPLATFORM_PUBLICATIONS
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_EXPERIMENTAL_TRY_NEXT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_INCREMENTAL_FIR
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_JVM_ADD_CLASSES_VARIANT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_INTERNAL_DIAGNOSTICS_IGNORE_WARNING_MODE
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_INTERNAL_DIAGNOSTICS_SHOW_STACKTRACE
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_INTERNAL_DIAGNOSTICS_USE_PARSABLE_FORMATTING
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_JS_KARMA_BROWSERS
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_JS_STDLIB_DOM_API_INCLUDED
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_JS_YARN
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_KMP_PUBLICATION_STRATEGY
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ALLOW_LEGACY_DEPENDENCIES
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_ANDROID_STYLE_NO_WARN
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_COMPUTE_TRANSFORMED_LIBRARY_CHECKSUM
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_INTRANSITIVE_METADATA_CONFIGURATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_OPTIMISTIC_NUMBER_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_PLATFORM_INTEGER_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_RESOURCES_PUBLICATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_FILTER_RESOURCES_BY_EXTENSION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_IMPORT_ENABLE_KGP_DEPENDENCY_RESOLUTION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_IMPORT_ENABLE_SLOW_SOURCES_JAR_RESOLVER
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_KMP_RESOLUTION_STRATEGY
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_KMP_STRICT_RESOLVE_IDE_DEPENDENCIES
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_SUPPRESS_EXPERIMENTAL_ARTIFACTS_DSL_WARNING
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_PARSE_INLINED_LOCAL_CLASSES
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_PUBLISH_JVM_ENVIRONMENT_ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_RUN_COMPILER_VIA_BUILD_TOOLS_API
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_STDLIB_DEFAULT_DEPENDENCY
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_STDLIB_JDK_VARIANTS_VERSION_ALIGNMENT
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerBuild
import org.jetbrains.kotlin.gradle.plugin.internal.isProjectIsolationEnabled
import org.jetbrains.kotlin.gradle.plugin.mpp.KmpIsolatedProjectsSupport
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinIrJsGeneratedTSValidationStrategy
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrOutputGranularity
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.gradle.utils.localProperties
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import org.jetbrains.kotlin.util.prefixIfNot
import java.io.File
import kotlin.time.Duration.Companion.minutes

internal class PropertiesProvider private constructor(private val project: Project) {
    val buildReportSingleFile: String?
        get() = property(PropertyNames.KOTLIN_BUILD_REPORT_SINGLE_FILE).orNull

    val buildReportOutputs: List<String>
        get() = property("kotlin.build.report.output").orNull?.split(",") ?: emptyList()

    val buildReportLabel: String?
        get() = property("kotlin.build.report.label").orNull

    val buildReportFileOutputDir: String?
        get() = property(PropertyNames.KOTLIN_BUILD_REPORT_FILE_DIR).orNull

    val buildReportHttpUrl: String?
        get() = property(PropertyNames.KOTLIN_BUILD_REPORT_HTTP_URL).orNull

    val buildReportHttpUser: String?
        get() = property("kotlin.build.report.http.user").orNull

    val buildReportHttpPassword: String?
        get() = property("kotlin.build.report.http.password").orNull

    val buildReportHttpVerboseEnvironment: Boolean
        get() = property("kotlin.build.report.http.verbose_environment").orNull?.toBoolean() ?: false

    val buildReportHttpIncludeGitBranchName: Boolean
        get() = property("kotlin.build.report.http.include_git_branch.name").orNull?.toBoolean() ?: false

    val buildReportHttpUseExecutor: Boolean
        get() = property("$KOTLIN_INTERNAL_NAMESPACE.build.report.http.use.executor").orNull?.toBoolean() ?: true

    val buildReportIncludeCompilerArguments: Boolean
        get() = booleanProperty("kotlin.build.report.include_compiler_arguments") ?: true

    val buildReportBuildScanCustomValuesLimit: Int
        get() = property("kotlin.build.report.build_scan.custom_values_limit").orNull?.toInt() ?: 1000

    val buildReportBuildScanMetrics: String?
        get() = property("kotlin.build.report.build_scan.metrics").orNull

    val buildReportMetrics: Boolean
        get() = booleanProperty("kotlin.build.report.metrics") ?: false

    val buildReportJsonDir: String?
        get() = property(PropertyNames.KOTLIN_BUILD_REPORT_JSON_DIR).orNull

    val buildReportVerbose: Boolean
        get() = booleanProperty("kotlin.build.report.verbose") ?: false

    val incrementalJvm: Boolean?
        get() = booleanProperty("kotlin.incremental")

    /**
     * Enables new experimental "IncrementalFirJvmCompilerRunner" for incremental compilation.
     */
    val incrementalJvmFir: Provider<Boolean>
        get() = booleanProvider(KOTLIN_INCREMENTAL_FIR).orElse(false)

    val incrementalJs: Boolean?
        get() = booleanProperty("kotlin.incremental.js")

    val incrementalJsKlib: Boolean?
        get() = booleanProperty("kotlin.incremental.js.klib")

    val incrementalJsIr: Boolean
        get() = booleanProperty("kotlin.incremental.js.ir") ?: true

    val incrementalWasm: Boolean
        get() = booleanProperty("kotlin.incremental.wasm") ?: false

    val incrementalNative: Boolean?
        get() = booleanProperty(PropertyNames.KOTLIN_NATIVE_INCREMENTAL_COMPILATION)

    val jsIrOutputGranularity: KotlinJsIrOutputGranularity
        get() = property("kotlin.js.ir.output.granularity").orNull?.let { KotlinJsIrOutputGranularity.byArgument(it) }
            ?: KotlinJsIrOutputGranularity.PER_MODULE

    val jsIrGeneratedTypeScriptValidationDevStrategy: KotlinIrJsGeneratedTSValidationStrategy
        get() = property("kotlin.js.ir.development.typescript.validation.strategy")
            .orNull?.let {
                KotlinIrJsGeneratedTSValidationStrategy.byArgument(
                    it
                )
            } ?: KotlinIrJsGeneratedTSValidationStrategy.IGNORE

    val jsIrGeneratedTypeScriptValidationProdStrategy: KotlinIrJsGeneratedTSValidationStrategy
        get() = property("kotlin.js.ir.production.typescript.validation.strategy")
            .orNull?.let {
                KotlinIrJsGeneratedTSValidationStrategy.byArgument(
                    it
                )
            } ?: KotlinIrJsGeneratedTSValidationStrategy.IGNORE

    val incrementalMultiplatform: Boolean?
        get() = booleanProperty("kotlin.incremental.multiplatform")

    val usePreciseJavaTracking: Boolean?
        get() = booleanProperty("kotlin.incremental.usePreciseJavaTracking")

    /**
     * Enable exposing secondary 'classes' variant for JVM compilations.
     */
    val addSecondaryClassesVariant: Boolean
        get() = booleanProperty(KOTLIN_JVM_ADD_CLASSES_VARIANT) ?: false

    val keepMppDependenciesIntactInPoms: Boolean?
        get() = booleanProperty("kotlin.mpp.keepMppDependenciesIntactInPoms")

    val ignorePluginLoadedInMultipleProjects: Boolean?
        get() = booleanProperty("kotlin.pluginLoadedInMultipleProjects.ignore")

    val keepAndroidBuildTypeAttribute: Boolean
        get() = booleanProperty("kotlin.android.buildTypeAttribute.keep") ?: false

    val kmpPublicationStrategy: KmpPublicationStrategy
        get() = this.get(KOTLIN_KMP_PUBLICATION_STRATEGY)?.let {
            KmpPublicationStrategy.fromProperty(it)
        } ?: KmpPublicationStrategy.StandardKMPPublication

    val kmpResolutionStrategy: KmpResolutionStrategy
        get() = this.get(KOTLIN_KMP_RESOLUTION_STRATEGY)?.let {
            KmpResolutionStrategy.fromProperty(it)
        } ?: KmpResolutionStrategy.StandardKMPResolution

    // Throw in IDE resolvers instead of just printing them
    val strictResolveIdeDependencies: Boolean
        get() = booleanProperty(KOTLIN_KMP_STRICT_RESOLVE_IDE_DEPENDENCIES) ?: false

    // This property disables -${checksum} output path suffix in the GMT transformed output klib to make them testable
    val computeTransformedLibraryChecksum: Boolean
        get() = booleanProperty(KOTLIN_MPP_COMPUTE_TRANSFORMED_LIBRARY_CHECKSUM) ?: true

    val enableKotlinToolingMetadataArtifact: Boolean
        get() = booleanProperty("kotlin.mpp.enableKotlinToolingMetadataArtifact") ?: true

    val mppEnableOptimisticNumberCommonization: Boolean
        get() = booleanProperty(KOTLIN_MPP_ENABLE_OPTIMISTIC_NUMBER_COMMONIZATION) ?: true

    val mppEnablePlatformIntegerCommonization: Boolean
        get() = booleanProperty(KOTLIN_MPP_ENABLE_PLATFORM_INTEGER_COMMONIZATION) ?: false

    val mppApplyDefaultHierarchyTemplate: Boolean
        get() = this.booleanProperty(KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE) ?: true

    val mppResourcesPublication: Boolean
        get() = this.booleanProperty(KOTLIN_MPP_ENABLE_RESOURCES_PUBLICATION) ?: true

    // This property exists for cases like KT-68095
    val mppFilterResourcesByExtension: Provider<Boolean>
        get() = project.providers
            .provider<Boolean> {
                booleanProperty(KOTLIN_MPP_FILTER_RESOURCES_BY_EXTENSION)
            }
            .orElse(false)

    val ignoreDisabledNativeTargets: Boolean?
        get() = booleanProperty(KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS)

    val ignoreAbsentAndroidMultiplatformTarget: Boolean
        get() = booleanProperty("kotlin.mpp.absentAndroidTarget.nowarn") ?: false

    val mppAndroidSourceSetLayoutVersion: Int?
        get() = this.property(KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION).orNull?.toIntOrNull()

    val ignoreMppAndroidSourceSetLayoutV2AndroidStyleDirs: Boolean
        get() = booleanProperty(KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_ANDROID_STYLE_NO_WARN) ?: false

    val ignoreDisabledCInteropCommonization: Boolean
        get() = booleanProperty("$KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION.nowarn") ?: false

    /** @see org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.IncorrectCompileOnlyDependencyWarning */
    @Deprecated(
        "Replaced with diagnostic IncorrectCompileOnlyDependencyWarning. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR
    )
    val ignoreIncorrectNativeDependencies: Boolean?
        get() = booleanProperty("kotlin.native.ignoreIncorrectDependencies")

    val publishJvmEnvironmentAttribute: Boolean
        get() = booleanProperty(KOTLIN_PUBLISH_JVM_ENVIRONMENT_ATTRIBUTE) ?: true

    /**
     * Enables individual test task reporting for aggregated test tasks.
     *
     * By default individual test tasks will not fail build if this task will be executed,
     * also individual html and xml reports will replaced by one consolidated html report.
     */
    val individualTaskReports: Boolean
        get() = booleanProperty("kotlin.tests.individualTaskReports") ?: false

    /**
     * Allow a user to choose distribution type. The following distribution types are available:
     *  - light - Doesn't include platform libraries and generates them at the user side. For 1.3 corresponds to the restricted distribution.
     *  - prebuilt - Includes all platform libraries.
     */
    val nativeDistributionType: String?
        get() = property("kotlin.native.distribution.type").orNull

    /**
     * Allows overriding Kotlin/Native base download url.
     *
     * When Kotlin/native will try to download native compiler, it will append compiler version and os type to this url.
     */
    val nativeBaseDownloadUrl: String
        get() = property("kotlin.native.distribution.baseDownloadUrl").orNull ?: NativeCompilerDownloader.BASE_DOWNLOAD_URL

    /**
     * Forces reinstalling a K/N distribution.
     *
     * The current distribution directory will be removed along with generated platform libraries and precompiled dependencies.
     * After that a fresh distribution with the same version will be installed. Platform libraries and precompiled dependencies will
     * be built in a regular way.
     *
     * Ignored if kotlin.native.home is specified.
     */
    val nativeReinstall: Boolean
        get() = booleanProperty("kotlin.native.reinstall") ?: false


    /**
     * Allows a user to specify free compiler arguments for K/N linker.
     */
    val nativeLinkArgs: List<String>
        get() = property("kotlin.native.linkArgs").orNull.orEmpty().split(' ').filterNot { it.isBlank() }

    /**
     * Allows a user to set project-wide options that will be passed to the K/N compiler via -Xbinary flag.
     * E.g. setting kotlin.native.binary.memoryModel=experimental results in passing -Xbinary=memoryModel=experimental to the compiler.
     * @return a map: property name without `kotlin.native.binary.` prefix -> property value
     */
    val nativeBinaryOptions: Map<String, String>
        get() = propertiesWithPrefix(KOTLIN_NATIVE_BINARY_OPTION_PREFIX).mapKeys { (key, _) ->
            key.removePrefix(KOTLIN_NATIVE_BINARY_OPTION_PREFIX)
        }

    /**
     * Allows a user to specify additional arguments of a JVM executing KLIB commonizer.
     */
    val commonizerJvmArgs: List<String>
        get() = propertyWithDeprecatedVariant("kotlin.mpp.commonizerJvmArgs", "kotlin.commonizer.jvmArgs")
            ?.split("\\s+".toRegex())
            .orEmpty()

    /**
     * Enables experimental commonization of user defined c-interop libraries.
     */
    val enableCInteropCommonization: Boolean?
        get() = booleanProperty(KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION)

    private val enableCInteropCommonizationSetByExternalPluginKey = "kotlin.internal.mpp.enableCInteropCommonization.setByExternalPlugin"

    internal var enableCInteropCommonizationSetByExternalPlugin: Boolean?
        get() {
            if (!project.extensions.extraProperties.has(enableCInteropCommonizationSetByExternalPluginKey)) return null
            return booleanProperty(enableCInteropCommonizationSetByExternalPluginKey)
        }
        set(value) {
            project.extensions.extraProperties.set(enableCInteropCommonizationSetByExternalPluginKey, "$value")
        }

    val commonizerLogLevel: String?
        get() = property("kotlin.mpp.commonizerLogLevel").orNull

    val enableNativeDistributionCommonizationCache: Boolean
        get() = booleanProperty("kotlin.mpp.enableNativeDistributionCommonizationCache") ?: true

    val enableIntransitiveMetadataConfiguration: Boolean
        get() = booleanProperty(KOTLIN_MPP_ENABLE_INTRANSITIVE_METADATA_CONFIGURATION) ?: false

    val enableKgpDependencyResolution: Boolean?
        get() = booleanProperty(KOTLIN_MPP_IMPORT_ENABLE_KGP_DEPENDENCY_RESOLUTION)

    val enableSlowIdeSourcesJarResolver: Boolean
        get() = booleanProperty(KOTLIN_MPP_IMPORT_ENABLE_SLOW_SOURCES_JAR_RESOLVER) ?: true

    /**
     * Dependencies caching orchestration machinery.
     */
    val nativeCacheOrchestration: NativeCacheOrchestration?
        get() = property(PropertyNames.KOTLIN_NATIVE_CACHE_ORCHESTRATION).orNull?.let { NativeCacheOrchestration.byCompilerArgument(it) }

    /**
     * Native backend threads.
     */
    val nativeParallelThreads: Int?
        get() = this.property(PropertyNames.KOTLIN_NATIVE_PARALLEL_THREADS).orNull?.toInt()

    val errorJsGenerateExternals: Boolean?
        get() = booleanProperty("kotlin.js.generate.externals")

    /**
     * Use Kotlin/JS backend compiler publishing attribute
     */
    val publishJsCompilerAttribute: Boolean
        get() = this.booleanProperty("$jsCompilerProperty.publish.attribute") ?: true

    val stdlibDefaultDependency: Boolean
        get() = booleanProperty(KOTLIN_STDLIB_DEFAULT_DEPENDENCY) ?: true

    val stdlibJdkVariantsVersionAlignment: Boolean
        get() = booleanProperty(KOTLIN_STDLIB_JDK_VARIANTS_VERSION_ALIGNMENT) ?: true

    val stdlibDomApiIncluded: Boolean
        get() = booleanProperty(KOTLIN_JS_STDLIB_DOM_API_INCLUDED) ?: true

    val yarn: Boolean
        get() = booleanProperty(KOTLIN_JS_YARN) ?: true

    val kotlinTestInferJvmVariant: Boolean
        get() = booleanProperty("kotlin.test.infer.jvm.variant") ?: true

    val kotlinOptionsSuppressFreeArgsModificationWarning: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_OPTIONS_SUPPRESS_FREEARGS_MODIFICATION_WARNING) ?: false

    val jvmTargetValidationMode: JvmTargetValidationMode
        get() = enumProperty(
            "kotlin.jvm.target.validation.mode",
            if (GradleVersion.current().baseVersion >= GradleVersion.version("8.0")) JvmTargetValidationMode.ERROR else JvmTargetValidationMode.WARNING
        )

    val kotlinDaemonJvmArgs: String?
        get() = property("kotlin.daemon.jvmargs").orNull

    val kotlinCompilerExecutionStrategy: KotlinCompilerExecutionStrategy
        get() = KotlinCompilerExecutionStrategy.fromProperty(
            this.property("kotlin.compiler.execution.strategy").orNull?.toLowerCaseAsciiOnly()
        )

    val kotlinDaemonUseFallbackStrategy: Boolean
        get() = booleanProperty("kotlin.daemon.useFallbackStrategy") ?: true

    val createDefaultMultiplatformPublications: Boolean
        get() = booleanProperty(KOTLIN_CREATE_DEFAULT_MULTIPLATFORM_PUBLICATIONS) ?: true

    val createArchiveTasksForCustomCompilations: Boolean
        get() = booleanProperty(KOTLIN_CREATE_ARCHIVE_TASKS_FOR_CUSTOM_COMPILATIONS) ?: false

    val runKotlinCompilerViaBuildToolsApi: Provider<Boolean>
        get() = booleanProvider(KOTLIN_RUN_COMPILER_VIA_BUILD_TOOLS_API).orElse(false)

    val allowLegacyMppDependencies: Boolean
        get() = booleanProperty(KOTLIN_MPP_ALLOW_LEGACY_DEPENDENCIES) ?: false

    val kotlinExperimentalTryNext: Provider<Boolean> = project.providers
        .provider<Boolean> {
            booleanProperty(KOTLIN_EXPERIMENTAL_TRY_NEXT)
        }
        .orElse(false)

    /**
     * Outputs diagnostics in a more verbose format with synthetic delimiters, helping to parse
     * diagnostics from unstructured input (e.g. build log).
     * This mode is meant to be at least somewhat human-readable (as it's used in our tests),
     * but is not meant to be pretty (users shouldn't see it)
     */
    val internalDiagnosticsUseParsableFormat: Boolean
        get() = booleanProperty(KOTLIN_INTERNAL_DIAGNOSTICS_USE_PARSABLE_FORMATTING) ?: false

    /**
     * *Overrides* the default option for rendering a stacktrace from which a diagnostic has been reported.
     * Because it's an override, 'null' is meaningful here and signifies "prefer the choice made without this property"
     */
    val internalDiagnosticsShowStacktrace: Boolean?
        get() = booleanProperty(KOTLIN_INTERNAL_DIAGNOSTICS_SHOW_STACKTRACE)

    /**
     * *Overrides* the default option for changing diagnostics severity when WarningMode is set to Fail
     */
    val internalDiagnosticsIgnoreWarningMode: Boolean?
        get() = booleanProperty(KOTLIN_INTERNAL_DIAGNOSTICS_IGNORE_WARNING_MODE)

    val suppressedGradlePluginWarnings: List<String>
        get() = property(PropertyNames.KOTLIN_SUPPRESS_GRADLE_PLUGIN_WARNINGS).orNull?.split(",").orEmpty()

    val suppressedGradlePluginErrors: List<String>
        get() = property(PropertyNames.KOTLIN_SUPPRESS_GRADLE_PLUGIN_ERRORS).orNull?.split(",").orEmpty()

    val suppressExperimentalArtifactsDslWarning: Boolean
        get() = booleanProperty(KOTLIN_NATIVE_SUPPRESS_EXPERIMENTAL_ARTIFACTS_DSL_WARNING) ?: false

    val cocoapodsExecutablePath: RegularFile?
        get() = property(PropertyNames.KOTLIN_APPLE_COCOAPODS_EXECUTABLE).orNull?.let { RegularFile { File(it) } }

    val appleAllowEmbedAndSignWithCocoapods: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_APPLE_ALLOW_EMBED_AND_SIGN_WITH_COCOAPODS) ?: false

    val appleIgnoreXcodeVersionCompatibility: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_APPLE_XCODE_COMPATIBILITY_NOWARN) ?: false

    val appleCreateSymbolicLinkToFrameworkInBuiltProductsDir: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_APPLE_CREATE_SYMBOLIC_LINK_TO_FRAMEWORK_IN_BUILT_PRODUCTS_DIR) ?: true

    val appleCopyDsymDuringArchiving: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_APPLE_COPY_DSYM_DURING_ARCHIVING) ?: true

    val swiftExportIgnoreExperimental: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_SWIFT_EXPORT_EXPERIMENTAL_NOWARN) == true

    /**
     * Application Binary Interface (ABI) validation:
     * Disable compilation support for some targets in functional tests.
     */
    val abiValidationBannedTargets: String?
        get() = property(PropertyNames.ABI_VALIDATION_BANNED_TARGETS).orNull

    /**
     * Application Binary Interface (ABI) validation:
     * Disable extension and task creation.
     */
    val abiValidationDisabled: Boolean
        get() = booleanProperty(PropertyNames.ABI_VALIDATION_DISABLED) ?: false


    /**
     * Allows suppressing the diagnostic [KotlinToolingDiagnostics.BuildToolsApiVersionInconsistency].
     * Required only for Kotlin repo bootstrapping.
     */
    val suppressBuildToolsApiVersionConsistencyChecks: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_SUPPRESS_BUILD_TOOLS_API_VERSION_CONSISTENCY_CHECKS) ?: false

    private val propertiesBuildService = PropertiesBuildService.registerIfAbsent(project).get()

    internal fun property(propertyName: String): Provider<String> = propertiesBuildService.property(propertyName, project)

    private fun <T : Any> propertyWithDeprecatedValues(
        propertyName: String,
        deprecatedValues: Set<T>,
        valueTransformer: (String?) -> T?,
        reportDiagnostic: () -> Unit,
    ): Provider<T> {
        val propValue = property(propertyName)
        return propValue.map { value ->
            val transformedValue = valueTransformer(value)
            if (transformedValue in deprecatedValues) {
                reportDiagnostic()
            }
            // Gradle has problems with nullability annotations: https://github.com/gradle/gradle/issues/24767
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            transformedValue
        }
    }

    internal fun booleanPropertyWithDeprecatedValues(
        propertyName: String,
        deprecatedValues: Set<Boolean>,
        reportDiagnostic: () -> Unit,
    ): Provider<Boolean> = propertyWithDeprecatedValues(propertyName, deprecatedValues, { it?.toBoolean() }, reportDiagnostic)

    internal fun get(propertyName: String): String? = propertiesBuildService.get(propertyName, project)

    internal fun getProvider(propertyName: String): Provider<String> = propertiesBuildService.property(propertyName, project)

    /**
     * The directory where Kotlin global caches, logs, or project persistent data are stored.
     *
     * If the property is not set, the plugin will use `<user_home>/.kotlin` as default.
     */
    val kotlinUserHomeDir: String?
        get() = get(PropertyNames.KOTLIN_USER_HOME_DIR)

    /**
     * The directory where Kotlin stores project-specific persistent caches.
     *
     * If the property is not set, the plugin will use `<project_dir>/.kotlin` as default.
     */
    val kotlinProjectPersistentDir: String?
        get() = get(PropertyNames.KOTLIN_PROJECT_PERSISTENT_DIR)

    /**
     * Disable writing into `<project_dir>/.gradle` directory.
     */
    val kotlinProjectPersistentDirGradleDisableWrite: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_PROJECT_PERSISTENT_DIR_GRADLE_DISABLE_WRITE) ?: false

    val kotlinCompilerArgumentsLogLevel: Provider<KotlinCompilerArgumentsLogLevel>
        get() = getProvider(PropertyNames.KOTLIN_COMPILER_ARGUMENTS_LOG_LEVEL)
            .map { KotlinCompilerArgumentsLogLevel.fromPropertyValue(it) }
            .orElse(KotlinCompilerArgumentsLogLevel.DEFAULT)

    /**
     * Without unsafe optimization: in k2, if common source is dirty, module will be rebuilt.
     * With unsafe optimization: regular IC logic is used. Common sources might see declarations from platform sources. See KT-62686
     */
    val enableUnsafeOptimizationsForMultiplatform: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_UNSAFE_MULTIPLATFORM_INCREMENTAL_COMPILATION) ?: false

    /**
     * Context: assume that incremental compilation of a.kt makes b.kt dirty (for example, because some function needs to be re-inlined)
     *
     * Without monotonous expansion: a.kt might be compiled on one step, and b.kt on the next step
     * With monotonous expansion: if a.kt is compiled on one step, both a.kt and b.kt are compiled on the next step
     */
    val enableMonotonousIncrementalCompileSetExpansion: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_MONOTONOUS_COMPILE_SET_EXPANSION) ?: true

    val disableKlibsCrossCompilation: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_NATIVE_DISABLE_KLIBS_CROSSCOMPILATION) ?: false

    val kotlinKmpProjectIsolationEnabled: Boolean
        get() {
            val mode = enumProperty<KmpIsolatedProjectsSupport>(
                PropertyNames.KOTLIN_KMP_ISOLATED_PROJECT_SUPPORT,
                KmpIsolatedProjectsSupport.ENABLE
            )

            return when (mode) {
                KmpIsolatedProjectsSupport.ENABLE -> true
                KmpIsolatedProjectsSupport.DISABLE -> false
                KmpIsolatedProjectsSupport.AUTO -> project.isProjectIsolationEnabled
            }
        }

    /**
     * Enable workaround for KT-64115, where both main compilation exploded klib and the same compressed klib
     * could end up in the test compilation leading to the compiler warning.
     *
     * If we are using non-packed Klibs, there's no point in this workaround.
     */
    val enableKlibKt64115Workaround: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_KLIBS_KT64115_WORKAROUND_ENABLED) ?: !useNonPackedKlibs

    val enableFusMetricsCollection: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_COLLECT_FUS_METRICS_ENABLED) ?: true

    val archivesTaskOutputAsFriendModule: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_ARCHIVES_TASK_OUTPUT_AS_FRIEND_ENABLED) ?: true

    val useNonPackedKlibs: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_USE_NON_PACKED_KLIBS) ?: true

    private val defaultClassLoaderCacheTimeout = 120.minutes.inWholeSeconds

    val classLoaderCacheTimeoutInSeconds: Provider<Long>
        get() = property(KOTLIN_CLASSLOADER_CACHE_TIMEOUT)
            .map { it.toLongOrNull() ?: defaultClassLoaderCacheTimeout }
            .orElse(defaultClassLoaderCacheTimeout)

    val preciseCompilationResultsBackup: Provider<Boolean> =
        booleanPropertyWithDeprecatedValues(KOTLIN_COMPILER_USE_PRECISE_COMPILATION_RESULTS_BACKUP, setOf(false)) {
            project.reportDiagnosticOncePerBuild(
                KotlinToolingDiagnostics.DeprecatedLegacyCompilationOutputsBackup()
            )
        }.orElse(true)

    /**
     * This property should be enabled together with [preciseCompilationResultsBackup]
     */
    val keepIncrementalCompilationCachesInMemory: Provider<Boolean> =
        booleanPropertyWithDeprecatedValues(KOTLIN_COMPILER_KEEP_INCREMENTAL_COMPILATION_CACHES_IN_MEMORY, setOf(false)) {
            project.reportDiagnosticOncePerBuild(
                KotlinToolingDiagnostics.DeprecatedLegacyCompilationOutputsBackup()
            )
        }.orElse(true)

    /**
     * Ignore overflow in [org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler]
     */
    val ignoreTcsmOverflow: Provider<Boolean> = booleanProvider(IGNORE_TCSM_OVERFLOW).orElse(false)

    /**
     * Affects classpath snapshot transformation.
     *
     * If enabled, we'd process the inlined local classes and use their contents
     * to refine the abi hash of the containing inline functions.
     */
    val parseInlinedLocalClasses: Provider<Boolean> = booleanProvider(KOTLIN_PARSE_INLINED_LOCAL_CLASSES).orElse(false)

    /**
     * Retrieves a comma-separated list of browsers to use when running karma tests for [target]
     * @see KOTLIN_JS_KARMA_BROWSERS
     */
    fun jsKarmaBrowsers(target: KotlinTarget? = null): String? =
        target?.name?.prefixIfNot("$KOTLIN_JS_KARMA_BROWSERS.")?.let { property(it).orNull }
            ?: property(KOTLIN_JS_KARMA_BROWSERS).orNull

    private fun propertyWithDeprecatedVariant(propName: String, deprecatedPropName: String): String? {
        val deprecatedProperty = get(deprecatedPropName)
        if (deprecatedProperty != null) {
            project.reportDiagnosticOncePerBuild(KotlinToolingDiagnostics.DeprecatedPropertyWithReplacement(deprecatedPropName, propName))
        }
        return get(propName) ?: deprecatedProperty
    }

    private fun booleanProperty(propName: String): Boolean? =
        get(propName)?.toBoolean()

    private fun booleanProvider(propName: String): Provider<Boolean> =
        getProvider(propName).map { it.toBoolean() }

    private inline fun <reified T : Enum<T>> enumProperty(
        propName: String,
        defaultValue: T,
    ): T = get(propName)?.let { enumValueOf<T>(it.toUpperCaseAsciiOnly()) } ?: defaultValue

    private val localProperties: Map<String, String> by lazy { project.localProperties.get() }

    private fun propertiesWithPrefix(prefix: String): Map<String, String> {
        val result: MutableMap<String, String> = mutableMapOf()
        project.extensions.extraProperties.properties.forEach { (name, value) ->
            if (name.startsWith(prefix) && value is String) {
                result[name] = value
            }
        }
        localProperties.forEach { (name, value) ->
            if (name.startsWith(prefix)) {
                // Project properties have higher priority.
                result.putIfAbsent(name, value)
            }
        }
        return result
    }

    object PropertyNames {
        private val allPropertiesStorage: MutableSet<String> = mutableSetOf()
        private fun property(name: String) = name.also { allPropertiesStorage += it }

        fun allProperties(): Set<String> = allPropertiesStorage
        fun allInternalProperties(): Set<String> = allProperties().filterTo(mutableSetOf()) { it.startsWith(KOTLIN_INTERNAL_NAMESPACE) }

        val KOTLIN_STDLIB_DEFAULT_DEPENDENCY = property("kotlin.stdlib.default.dependency")
        val KOTLIN_STDLIB_JDK_VARIANTS_VERSION_ALIGNMENT = property("kotlin.stdlib.jdk.variants.version.alignment")
        val KOTLIN_JS_STDLIB_DOM_API_INCLUDED = property("kotlin.js.stdlib.dom.api.included")
        val KOTLIN_JS_YARN = property("kotlin.js.yarn")
        val KOTLIN_MPP_COMPUTE_TRANSFORMED_LIBRARY_CHECKSUM = property("${KOTLIN_INTERNAL_NAMESPACE}.mpp.computeTransformedLibraryChecksum")
        val KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA = property("kotlin.mpp.enableGranularSourceSetsMetadata")
        val KOTLIN_MPP_ENABLE_COMPATIBILITY_METADATA_VARIANT = property("kotlin.mpp.enableCompatibilityMetadataVariant")
        val KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION = property("kotlin.mpp.enableCInteropCommonization")
        val KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT = property("kotlin.mpp.hierarchicalStructureSupport")
        val KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION = property("kotlin.mpp.androidSourceSetLayoutVersion")
        val KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_ANDROID_STYLE_NO_WARN =
            property("kotlin.mpp.androidSourceSetLayoutV2AndroidStyleDirs.nowarn")
        val KOTLIN_MPP_IMPORT_ENABLE_KGP_DEPENDENCY_RESOLUTION = property("kotlin.mpp.import.enableKgpDependencyResolution")
        val KOTLIN_MPP_IMPORT_ENABLE_SLOW_SOURCES_JAR_RESOLVER = property("kotlin.mpp.import.enableSlowSourcesJarResolver")
        val KOTLIN_MPP_ENABLE_INTRANSITIVE_METADATA_CONFIGURATION = property("kotlin.mpp.enableIntransitiveMetadataConfiguration")
        val KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE = property("kotlin.mpp.applyDefaultHierarchyTemplate")
        val KOTLIN_MPP_ENABLE_RESOURCES_PUBLICATION = property("kotlin.mpp.enableResourcesPublication")
        val KOTLIN_MPP_RESOURCES_RESOLUTION_STRATEGY = property("kotlin.mpp.resourcesResolutionStrategy")
        val KOTLIN_MPP_FILTER_RESOURCES_BY_EXTENSION = property("kotlin.mpp.filterResourcesByExtension")
        val KOTLIN_NATIVE_DEPENDENCY_PROPAGATION = property("kotlin.native.enableDependencyPropagation")
        val KOTLIN_NATIVE_CACHE_ORCHESTRATION = property("kotlin.native.cacheOrchestration")
        val KOTLIN_NATIVE_PARALLEL_THREADS = property("kotlin.native.parallelThreads")
        val KOTLIN_NATIVE_INCREMENTAL_COMPILATION = property("kotlin.incremental.native")
        val KOTLIN_MPP_ENABLE_OPTIMISTIC_NUMBER_COMMONIZATION = property("kotlin.mpp.enableOptimisticNumberCommonization")
        val KOTLIN_MPP_ENABLE_PLATFORM_INTEGER_COMMONIZATION = property("kotlin.mpp.enablePlatformIntegerCommonization")
        val KOTLIN_JS_KARMA_BROWSERS = property("kotlin.js.browser.karma.browsers")
        val KOTLIN_BUILD_REPORT_SINGLE_FILE = property("kotlin.build.report.single_file")
        val KOTLIN_BUILD_REPORT_HTTP_URL = property("kotlin.build.report.http.url")
        val KOTLIN_BUILD_REPORT_JSON_DIR = property("kotlin.build.report.json.directory")
        val KOTLIN_BUILD_REPORT_FILE_DIR = property("kotlin.build.report.file.output_dir")
        val KOTLIN_OPTIONS_SUPPRESS_FREEARGS_MODIFICATION_WARNING = property("kotlin.options.suppressFreeCompilerArgsModificationWarning")
        val KOTLIN_JVM_ADD_CLASSES_VARIANT = property("kotlin.jvm.addClassesVariant")
        val KOTLIN_COMPILER_USE_PRECISE_COMPILATION_RESULTS_BACKUP = property("kotlin.compiler.preciseCompilationResultsBackup")
        val KOTLIN_COMPILER_KEEP_INCREMENTAL_COMPILATION_CACHES_IN_MEMORY =
            property("kotlin.compiler.keepIncrementalCompilationCachesInMemory")
        val KOTLIN_RUN_COMPILER_VIA_BUILD_TOOLS_API = property("kotlin.compiler.runViaBuildToolsApi")
        val KOTLIN_MPP_ALLOW_LEGACY_DEPENDENCIES = property("kotlin.mpp.allow.legacy.dependencies")
        val KOTLIN_PUBLISH_JVM_ENVIRONMENT_ATTRIBUTE = property("kotlin.publishJvmEnvironmentAttribute")
        val KOTLIN_EXPERIMENTAL_TRY_NEXT = property("kotlin.experimental.tryNext")
        val KOTLIN_SUPPRESS_GRADLE_PLUGIN_WARNINGS = property(KOTLIN_SUPPRESS_GRADLE_PLUGIN_WARNINGS_PROPERTY)
        val KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS = property("kotlin.native.ignoreDisabledTargets")
        val KOTLIN_NATIVE_SUPPRESS_EXPERIMENTAL_ARTIFACTS_DSL_WARNING = property("kotlin.native.suppressExperimentalArtifactsDslWarning")
        val KOTLIN_SUPPRESS_BUILD_TOOLS_API_VERSION_CONSISTENCY_CHECKS =
            property("kotlin.internal.suppress.buildToolsApiVersionConsistencyChecks")
        val KOTLIN_USER_HOME_DIR = property("kotlin.user.home")
        val KOTLIN_PROJECT_PERSISTENT_DIR = property("kotlin.project.persistent.dir")
        val KOTLIN_PROJECT_PERSISTENT_DIR_GRADLE_DISABLE_WRITE = property("kotlin.project.persistent.dir.gradle.disableWrite")
        val KOTLIN_APPLE_CREATE_SYMBOLIC_LINK_TO_FRAMEWORK_IN_BUILT_PRODUCTS_DIR = property("kotlin.apple.createSymbolicLinkToFrameworkInBuiltProductsDir")
        val KOTLIN_APPLE_COPY_DSYM_DURING_ARCHIVING = property("kotlin.apple.copyDsymDuringArchiving")
        val KOTLIN_APPLE_XCODE_COMPATIBILITY_NOWARN = property("kotlin.apple.xcodeCompatibility.nowarn")
        val KOTLIN_APPLE_COCOAPODS_EXECUTABLE = property("kotlin.apple.cocoapods.bin")
        val KOTLIN_APPLE_ALLOW_EMBED_AND_SIGN_WITH_COCOAPODS = property("kotlin.apple.deprecated.allowUsingEmbedAndSignWithCocoaPodsDependencies")
        val KOTLIN_SWIFT_EXPORT_EXPERIMENTAL_NOWARN = property("kotlin.swift-export.experimental.nowarn")
        val KOTLIN_NATIVE_DISABLE_KLIBS_CROSSCOMPILATION = property("kotlin.native.disableKlibsCrossCompilation")
        val KOTLIN_ARCHIVES_TASK_OUTPUT_AS_FRIEND_ENABLED = property("kotlin.build.archivesTaskOutputAsFriendModule")
        val KOTLIN_KMP_PUBLICATION_STRATEGY = property("${KOTLIN_INTERNAL_NAMESPACE}.kmp.kmpPublicationStrategy")
        val KOTLIN_KMP_RESOLUTION_STRATEGY = property("${KOTLIN_INTERNAL_NAMESPACE}.kmp.kmpResolutionStrategy")
        val KOTLIN_KMP_STRICT_RESOLVE_IDE_DEPENDENCIES = property("${KOTLIN_INTERNAL_NAMESPACE}.kmp.strictResolveIdeDependencies")
        val KOTLIN_KMP_ISOLATED_PROJECT_SUPPORT = property("kotlin.kmp.isolated-projects.support")
        val KOTLIN_INCREMENTAL_FIR = property("kotlin.incremental.jvm.fir")

        /**
         * Internal properties: builds get big non-suppressible warning when such properties are used
         * See [org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.InternalGradlePropertiesUsageChecker]
         **/
        val KOTLIN_MPP_HIERARCHICAL_STRUCTURE_BY_DEFAULT = property("$KOTLIN_INTERNAL_NAMESPACE.mpp.hierarchicalStructureByDefault")
        val KOTLIN_CREATE_DEFAULT_MULTIPLATFORM_PUBLICATIONS =
            property("$KOTLIN_INTERNAL_NAMESPACE.mpp.createDefaultMultiplatformPublications")
        val KOTLIN_INTERNAL_DIAGNOSTICS_USE_PARSABLE_FORMATTING = property("$KOTLIN_INTERNAL_NAMESPACE.diagnostics.useParsableFormatting")
        val KOTLIN_INTERNAL_DIAGNOSTICS_SHOW_STACKTRACE = property("$KOTLIN_INTERNAL_NAMESPACE.diagnostics.showStacktrace")
        val KOTLIN_INTERNAL_DIAGNOSTICS_IGNORE_WARNING_MODE = property("$KOTLIN_INTERNAL_NAMESPACE.diagnostics.ignoreWarningMode")
        val KOTLIN_SUPPRESS_GRADLE_PLUGIN_ERRORS = property("$KOTLIN_INTERNAL_NAMESPACE.suppressGradlePluginErrors")
        val KOTLIN_CREATE_ARCHIVE_TASKS_FOR_CUSTOM_COMPILATIONS =
            property("$KOTLIN_INTERNAL_NAMESPACE.mpp.createArchiveTasksForCustomCompilations")
        val KOTLIN_COMPILER_ARGUMENTS_LOG_LEVEL = property("$KOTLIN_INTERNAL_NAMESPACE.compiler.arguments.log.level")
        val KOTLIN_UNSAFE_MULTIPLATFORM_INCREMENTAL_COMPILATION =
            property("$KOTLIN_INTERNAL_NAMESPACE.incremental.enableUnsafeOptimizationsForMultiplatform")
        val KOTLIN_MONOTONOUS_COMPILE_SET_EXPANSION = property("$KOTLIN_INTERNAL_NAMESPACE.incremental.enableMonotonousCompileSetExpansion")
        val KOTLIN_KLIBS_KT64115_WORKAROUND_ENABLED = property("$KOTLIN_INTERNAL_NAMESPACE.klibs.enableWorkaroundForKT64115")
        val KOTLIN_COLLECT_FUS_METRICS_ENABLED = property("$KOTLIN_INTERNAL_NAMESPACE.collectFUSMetrics")
        val KOTLIN_USE_NON_PACKED_KLIBS = property("$KOTLIN_INTERNAL_NAMESPACE.klibs.non-packed")
        val KOTLIN_CLASSLOADER_CACHE_TIMEOUT = property("$KOTLIN_INTERNAL_NAMESPACE.classloaderCache.timeoutSeconds")
        val ABI_VALIDATION_BANNED_TARGETS = property(ABI_VALIDATION_BANNED_TARGETS_NAME)
        val ABI_VALIDATION_DISABLED = property(ABI_VALIDATION_DISABLED_NAME)
        val KOTLIN_PARSE_INLINED_LOCAL_CLASSES = property("$KOTLIN_INTERNAL_NAMESPACE.classpathSnapshot.parseInlinedLocalClasses")
    }

    companion object {

        private const val CACHED_PROVIDER_EXT_NAME = "kotlin.properties.provider"

        internal const val KOTLIN_SUPPRESS_GRADLE_PLUGIN_WARNINGS_PROPERTY = "kotlin.suppressGradlePluginWarnings"

        private const val KOTLIN_NATIVE_BINARY_OPTION_PREFIX = "kotlin.native.binary."

        internal const val KOTLIN_INTERNAL_NAMESPACE = "kotlin.internal"

        internal const val ABI_VALIDATION_BANNED_TARGETS_NAME = "$KOTLIN_INTERNAL_NAMESPACE.abi.validation.klib.targets.disabled.for.testing"

        internal const val ABI_VALIDATION_DISABLED_NAME = "kotlin.abi.validation.disabled"

        operator fun invoke(project: Project): PropertiesProvider =
            with(project.extensions.extraProperties) {
                if (!has(CACHED_PROVIDER_EXT_NAME)) {
                    set(CACHED_PROVIDER_EXT_NAME, PropertiesProvider(project))
                }
                return get(CACHED_PROVIDER_EXT_NAME) as? PropertiesProvider
                    ?: PropertiesProvider(project) // Fallback if multiple class loaders are involved
            }

        internal val Project.kotlinPropertiesProvider get() = invoke(this)

    }
}
