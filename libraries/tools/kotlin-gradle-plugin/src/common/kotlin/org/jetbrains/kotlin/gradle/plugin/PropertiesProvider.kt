/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.dsl.NativeCacheOrchestration
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler.Companion.IGNORE_TCSM_OVERFLOW
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.Companion.jsCompilerProperty
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_JS_STDLIB_DOM_API_INCLUDED
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_ABI_SNAPSHOT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_COMPILER_USE_PRECISE_COMPILATION_RESULTS_BACKUP
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_JS_KARMA_BROWSERS
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_ANDROID_STYLE_NO_WARN
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION_1_NO_WARN
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_COMPATIBILITY_METADATA_VARIANT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_INTRANSITIVE_METADATA_CONFIGURATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_OPTIMISTIC_NUMBER_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_PLATFORM_INTEGER_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_BY_DEFAULT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_DEPRECATED_PROPERTIES_NO_WARN
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_IMPORT_ENABLE_SLOW_SOURCES_JAR_RESOLVER
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_DEPENDENCY_PROPAGATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_USE_XCODE_MESSAGE_STYLE
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_STDLIB_DEFAULT_DEPENDENCY
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_STDLIB_JDK_VARIANTS_VERSION_ALIGNMENT
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinIrJsGeneratedTSValidationStrategy
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrOutputGranularity
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackMajorVersion
import org.jetbrains.kotlin.gradle.targets.native.DisabledNativeTargetsReporter
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import org.jetbrains.kotlin.tooling.core.UnsafeApi
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import org.jetbrains.kotlin.util.prefixIfNot
import java.io.File
import java.util.*

@OptIn(UnsafeApi::class)
internal class PropertiesProvider private constructor(private val project: Project) {
    private val localProperties: Properties by lazy {
        Properties().apply {
            val localPropertiesFile = File(project.rootDir, "local.properties")
            if (localPropertiesFile.isFile) {
                localPropertiesFile.inputStream().use {
                    load(it)
                }
            }
        }
    }

    @Deprecated(message = "Please use kotlin.build.report.output=SINGLE_FILE and kotlin.build.report.single_file ")
    val singleBuildMetricsFile: File?
        get() = this.property("kotlin.internal.single.build.metrics.file")?.let { File(it) }

    val buildReportSingleFile: File?
        get() = this.property(PropertyNames.KOTLIN_BUILD_REPORT_SINGLE_FILE)?.let { File(it) }

    @Deprecated(message = "Please use kotlin.build.report.output instead ")
    val buildReportEnabled: Boolean
        get() {
            val propValue = booleanProperty("kotlin.build.report.enable")?.also {
                SingleWarningPerBuild.show(
                    project,
                    """
                    'kotlin.build.report.enable' property does nothing since 1.7.0 release 
                    and scheduled to be removed in Kotlin 1.8.0 release!
                        Please use 'kotlin.build.report.output' instead
                    """.trimIndent()
                )
            }
            return propValue ?: false
        }

    val buildReportOutputs: List<String>
        get() = this.property("kotlin.build.report.output")?.split(",") ?: emptyList()

    val buildReportLabel: String?
        get() = this.property("kotlin.build.report.label")

    val buildReportFileOutputDir: File?
        get() = this.property("kotlin.build.report.file.output_dir")?.let { File(it) }

    val buildReportHttpUrl: String?
        get() = this.property(PropertyNames.KOTLIN_BUILD_REPORT_HTTP_URL)

    val buildReportHttpUser: String?
        get() = this.property("kotlin.build.report.http.user")

    val buildReportHttpPassword: String?
        get() = this.property("kotlin.build.report.http.password")

    val buildReportHttpVerboseEnvironment: Boolean
        get() = property("kotlin.build.report.http.verbose_environment")?.toBoolean() ?: false

    val buildReportHttpIncludeGitBranchName: Boolean
        get() = property("kotlin.build.report.http.include_git_branch.name")?.toBoolean() ?: false

    val buildReportIncludeCompilerArguments: Boolean
        get() = booleanProperty("kotlin.build.report.include_compiler_arguments") ?: true

    val buildReportBuildScanCustomValuesLimit: Int
        get() = property("kotlin.build.report.build_scan.custom_values_limit")?.toInt() ?: 1000

    val buildReportBuildScanMetrics: String?
        get() = property("kotlin.build.report.build_scan.metrics")

    val buildReportMetrics: Boolean
        get() = booleanProperty("kotlin.build.report.metrics") ?: false

    val buildReportVerbose: Boolean
        get() = booleanProperty("kotlin.build.report.verbose") ?: false

    @Deprecated("Please use \"kotlin.build.report.file.output_dir\" property instead")
    val buildReportDir: File?
        get() = this.property("kotlin.build.report.dir")?.let { File(it) }

    val incrementalJvm: Boolean?
        get() = booleanProperty("kotlin.incremental")

    val incrementalJs: Boolean?
        get() = booleanProperty("kotlin.incremental.js")

    val incrementalJsKlib: Boolean?
        get() = booleanProperty("kotlin.incremental.js.klib")

    val incrementalJsIr: Boolean
        get() = booleanProperty("kotlin.incremental.js.ir") ?: true

    val jsIrOutputGranularity: KotlinJsIrOutputGranularity
        get() = this.property("kotlin.js.ir.output.granularity")?.let { KotlinJsIrOutputGranularity.byArgument(it) }
            ?: KotlinJsIrOutputGranularity.PER_MODULE

    val jsIrGeneratedTypeScriptValidationDevStrategy: KotlinIrJsGeneratedTSValidationStrategy
        get() = this.property("kotlin.js.ir.development.typescript.validation.strategy")?.let {
            KotlinIrJsGeneratedTSValidationStrategy.byArgument(
                it
            )
        } ?: KotlinIrJsGeneratedTSValidationStrategy.IGNORE

    val jsIrGeneratedTypeScriptValidationProdStrategy: KotlinIrJsGeneratedTSValidationStrategy
        get() = this.property("kotlin.js.ir.production.typescript.validation.strategy")?.let {
            KotlinIrJsGeneratedTSValidationStrategy.byArgument(
                it
            )
        } ?: KotlinIrJsGeneratedTSValidationStrategy.IGNORE

    val incrementalMultiplatform: Boolean?
        get() = booleanProperty("kotlin.incremental.multiplatform")

    val usePreciseJavaTracking: Boolean?
        get() = booleanProperty("kotlin.incremental.usePreciseJavaTracking")

    val useClasspathSnapshot: Boolean
        get() {
            val reporter = KotlinBuildStatsService.getInstance()
            // The feature should be controlled by a Gradle property.
            // Currently, we also allow it to be controlled by a system property to make it easier to test the feature during development.
            // TODO: Remove the system property later.

            val gradleProperty = booleanProperty(CompilerSystemProperties.COMPILE_INCREMENTAL_WITH_ARTIFACT_TRANSFORM.property)
            if (gradleProperty != null) {
                reporter?.report(StringMetrics.USE_CLASSPATH_SNAPSHOT, gradleProperty.toString())
                return gradleProperty
            }
            val systemProperty = CompilerSystemProperties.COMPILE_INCREMENTAL_WITH_ARTIFACT_TRANSFORM.value?.toBooleanLenient()
            if (systemProperty != null) {
                reporter?.report(StringMetrics.USE_CLASSPATH_SNAPSHOT, systemProperty.toString())
                return systemProperty
            }
            reporter?.report(StringMetrics.USE_CLASSPATH_SNAPSHOT, "default-true")
            return true
        }

    val useKotlinAbiSnapshot: Boolean
        get() = booleanProperty(KOTLIN_ABI_SNAPSHOT) ?: false

    val useK2: Boolean?
        get() = booleanProperty("kotlin.useK2")

    val keepMppDependenciesIntactInPoms: Boolean?
        get() = booleanProperty("kotlin.mpp.keepMppDependenciesIntactInPoms")

    val ignorePluginLoadedInMultipleProjects: Boolean?
        get() = booleanProperty("kotlin.pluginLoadedInMultipleProjects.ignore")

    val keepAndroidBuildTypeAttribute: Boolean
        get() = booleanProperty("kotlin.android.buildTypeAttribute.keep") ?: false

    val enableGranularSourceSetsMetadata: Boolean?
        get() = booleanProperty(KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA)

    val hierarchicalStructureSupport: Boolean
        get() = booleanProperty(KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT) ?: mppHierarchicalStructureByDefault

    val nativeDependencyPropagation: Boolean?
        get() = booleanProperty(KOTLIN_NATIVE_DEPENDENCY_PROPAGATION)

    var mpp13XFlagsSetByPlugin: Boolean
        get() = booleanProperty("kotlin.internal.mpp.13X.flags.setByPlugin") ?: false
        set(value) {
            project.extensions.extraProperties.set("kotlin.internal.mpp.13X.flags.setByPlugin", "$value")
        }

    val mppHierarchicalStructureByDefault: Boolean
        get() = booleanProperty(KOTLIN_MPP_HIERARCHICAL_STRUCTURE_BY_DEFAULT) ?: true

    val enableCompatibilityMetadataVariant: Boolean
        get() {
            return (booleanProperty(KOTLIN_MPP_ENABLE_COMPATIBILITY_METADATA_VARIANT) ?: !mppHierarchicalStructureByDefault)
        }

    val enableKotlinToolingMetadataArtifact: Boolean
        get() = booleanProperty("kotlin.mpp.enableKotlinToolingMetadataArtifact") ?: true

    val mppEnableOptimisticNumberCommonization: Boolean
        get() = booleanProperty(KOTLIN_MPP_ENABLE_OPTIMISTIC_NUMBER_COMMONIZATION) ?: true

    val mppEnablePlatformIntegerCommonization: Boolean
        get() = booleanProperty(KOTLIN_MPP_ENABLE_PLATFORM_INTEGER_COMMONIZATION) ?: false

    val wasmStabilityNoWarn: Boolean
        get() = booleanProperty("kotlin.wasm.stability.nowarn") ?: false

    val jsCompilerNoWarn: Boolean
        get() = booleanProperty("$jsCompilerProperty.nowarn") ?: false

    val ignoreDisabledNativeTargets: Boolean?
        get() = booleanProperty(DisabledNativeTargetsReporter.DISABLE_WARNING_PROPERTY_NAME)

    val ignoreAbsentAndroidMultiplatformTarget: Boolean
        get() = booleanProperty("kotlin.mpp.absentAndroidTarget.nowarn") ?: false

    val ignoreAndroidGradlePluginCompatibilityIssues: Boolean
        get() = booleanProperty(KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN) ?: false

    val mppAndroidSourceSetLayoutVersion: Int?
        get() = this.property(KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION)?.toIntOrNull()

    val ignoreMppAndroidSourceSetLayoutVersion: Boolean
        get() = booleanProperty(KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION_1_NO_WARN) ?: false

    val ignoreMppAndroidSourceSetLayoutV2AndroidStyleDirs: Boolean
        get() = booleanProperty(KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_ANDROID_STYLE_NO_WARN) ?: false

    val ignoreDisabledCInteropCommonization: Boolean
        get() = booleanProperty("$KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION.nowarn") ?: false

    val ignoreIncorrectNativeDependencies: Boolean?
        get() = booleanProperty(KOTLIN_NATIVE_IGNORE_INCORRECT_DEPENDENCIES)

    val ignoreHmppDeprecationWarnings: Boolean?
        get() = booleanProperty(KOTLIN_MPP_DEPRECATED_PROPERTIES_NO_WARN)

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
        get() = this.property("kotlin.native.distribution.type")

    /**
     * Allows overriding Kotlin/Native base download url.
     *
     * When Kotlin/native will try to download native compiler, it will append compiler version and os type to this url.
     */
    val nativeBaseDownloadUrl: String
        get() = this.property("kotlin.native.distribution.baseDownloadUrl") ?: NativeCompilerDownloader.BASE_DOWNLOAD_URL

    /**
     * Allows downloading Kotlin/Native distribution with maven.
     *
     * Makes downloader search for bundles in maven repositories specified in the project.
     */
    val nativeDownloadFromMaven: Boolean
        get() = this.booleanProperty("kotlin.native.distribution.downloadFromMaven") ?: false

    /**
     * A property that was used to choose a restricted distribution in 1.3.
     */
    val nativeDeprecatedRestricted: Boolean?
        get() = booleanProperty("kotlin.native.restrictedDistribution")

    /**
     * Allows a user to force a particular cinterop mode for platform libraries generation. Available modes: sourcecode, metadata.
     * A main purpose of this property is working around potential problems with the metadata mode.
     */
    val nativePlatformLibrariesMode: String?
        get() = this.property("kotlin.native.platform.libraries.mode")

    /**
     * Allows a user to provide a local Kotlin/Native distribution instead of a downloaded one.
     */
    val nativeHome: String?
        get() = propertyWithDeprecatedVariant(KOTLIN_NATIVE_HOME, "org.jetbrains.kotlin.native.home")

    /**
     * Allows a user to override Kotlin/Native version.
     */
    val nativeVersion: String?
        get() = propertyWithDeprecatedVariant("kotlin.native.version", "org.jetbrains.kotlin.native.version")

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
     * Allows a user to specify additional arguments of a JVM executing a K/N compiler.
     */
    val nativeJvmArgs: String?
        get() = propertyWithDeprecatedVariant("kotlin.native.jvmArgs", "org.jetbrains.kotlin.native.jvmArgs")

    /**
     * Allows a user to specify free compiler arguments for K/N linker.
     */
    val nativeLinkArgs: List<String>
        get() = this.property("kotlin.native.linkArgs").orEmpty().split(' ').filterNot { it.isBlank() }

    /**
     * Forces to run a compilation in a separate JVM.
     */
    val nativeDisableCompilerDaemon: Boolean?
        get() = booleanProperty("kotlin.native.disableCompilerDaemon")

    /**
     * Switches Kotlin/Native tasks to using embeddable compiler jar,
     * allowing to apply backend-agnostic compiler plugin artifacts.
     * Will be default after proper migration.
     */
    val nativeUseEmbeddableCompilerJar: Boolean
        get() = booleanProperty("kotlin.native.useEmbeddableCompilerJar") ?: true

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
     * Forces K/N compiler to print messages which could be parsed by Xcode
     */
    val nativeUseXcodeMessageStyle: Boolean?
        get() = booleanProperty(KOTLIN_NATIVE_USE_XCODE_MESSAGE_STYLE)

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
    val enableCInteropCommonization: Boolean
        get() = booleanProperty(KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION) ?: false


    val commonizerLogLevel: String?
        get() = this.property("kotlin.mpp.commonizerLogLevel")

    val enableNativeDistributionCommonizationCache: Boolean
        get() = booleanProperty("kotlin.mpp.enableNativeDistributionCommonizationCache") ?: true

    val enableIntransitiveMetadataConfiguration: Boolean
        get() = booleanProperty(KOTLIN_MPP_ENABLE_INTRANSITIVE_METADATA_CONFIGURATION) ?: false

    val enableSlowIdeSourcesJarResolver: Boolean
        get() = booleanProperty(KOTLIN_MPP_IMPORT_ENABLE_SLOW_SOURCES_JAR_RESOLVER) ?: true

    /**
     * Dependencies caching strategy for all targets that support caches.
     */
    val nativeCacheKind: NativeCacheKind?
        get() = this.property("kotlin.native.cacheKind")?.let { NativeCacheKind.byCompilerArgument(it) }

    /**
     * Dependencies caching strategy for [target].
     */
    fun nativeCacheKindForTarget(target: KonanTarget): NativeCacheKind? =
        this.property("kotlin.native.cacheKind.${target.presetName}")?.let { NativeCacheKind.byCompilerArgument(it) }

    /**
     * Dependencies caching orchestration machinery.
     */
    val nativeCacheOrchestration: NativeCacheOrchestration?
        get() = this.property(PropertyNames.KOTLIN_NATIVE_CACHE_ORCHESTRATION)?.let { NativeCacheOrchestration.byCompilerArgument(it) }

    /**
     * Ignore overflow in [org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler]
     */
    val ignoreTcsmOverflow: Boolean
        get() = booleanProperty(IGNORE_TCSM_OVERFLOW) ?: false

    val errorJsGenerateExternals: Boolean?
        get() = booleanProperty("kotlin.js.generate.externals")

    /**
     * Use Kotlin/JS backend compiler type
     */
    val jsCompiler: KotlinJsCompilerType?
        get() = this.property(jsCompilerProperty)?.let { KotlinJsCompilerType.byArgumentOrNull(it) }

    /**
     * Use Kotlin/JS backend compiler publishing attribute
     */
    val publishJsCompilerAttribute: Boolean
        get() = this.booleanProperty("$jsCompilerProperty.publish.attribute") ?: true

    /**
     * Use Webpack 4 for compatibility
     */
    val webpackMajorVersion: WebpackMajorVersion
        get() = this.property(WebpackMajorVersion.webpackMajorVersion)?.let { WebpackMajorVersion.byArgument(it) }
            ?.also { version ->
                if (!WebpackMajorVersion.webpackVersionWarning && version != WebpackMajorVersion.DEFAULT) {
                    WebpackMajorVersion.webpackVersionWarning = true
                    project.logger.warn(WebpackMajorVersion.warningMessage)
                }
            }
            ?: WebpackMajorVersion.DEFAULT

    /**
     * Use Kotlin/JS backend compiler type
     */
    val jsGenerateExecutableDefault: Boolean
        get() = (booleanProperty("kotlin.js.generate.executable.default") ?: true).also {
            KotlinBuildStatsService.getInstance()
                ?.report(StringMetrics.JS_GENERATE_EXECUTABLE_DEFAULT, it.toString())
        }

    val stdlibDefaultDependency: Boolean
        get() = booleanProperty(KOTLIN_STDLIB_DEFAULT_DEPENDENCY) ?: true

    val stdlibJdkVariantsVersionAlignment: Boolean
        get() = booleanProperty(KOTLIN_STDLIB_JDK_VARIANTS_VERSION_ALIGNMENT) ?: true

    val stdlibDomApiIncluded: Boolean
        get() = booleanProperty(KOTLIN_JS_STDLIB_DOM_API_INCLUDED) ?: true

    val kotlinTestInferJvmVariant: Boolean
        get() = booleanProperty("kotlin.test.infer.jvm.variant") ?: true

    val kotlinOptionsSuppressFreeArgsModificationWarning: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_OPTIONS_SUPPRESS_FREEARGS_MODIFICATION_WARNING) ?: false

    enum class JvmTargetValidationMode {
        IGNORE, WARNING, ERROR
    }

    val jvmTargetValidationMode: JvmTargetValidationMode
        get() = enumProperty(
            "kotlin.jvm.target.validation.mode",
            if (GradleVersion.current().baseVersion >= GradleVersion.version("8.0")) JvmTargetValidationMode.ERROR else JvmTargetValidationMode.WARNING
        )

    val kotlinDaemonJvmArgs: String?
        get() = this.property("kotlin.daemon.jvmargs")

    val kotlinCompilerExecutionStrategy: KotlinCompilerExecutionStrategy
        get() = KotlinCompilerExecutionStrategy.fromProperty(
            this.property("kotlin.compiler.execution.strategy")?.toLowerCaseAsciiOnly()
        )

    val kotlinDaemonUseFallbackStrategy: Boolean
        get() = booleanProperty("kotlin.daemon.useFallbackStrategy") ?: true

    val preciseCompilationResultsBackup: Boolean
        get() = booleanProperty(KOTLIN_COMPILER_USE_PRECISE_COMPILATION_RESULTS_BACKUP) ?: false

    /**
     * Retrieves a comma-separated list of browsers to use when running karma tests for [target]
     * @see KOTLIN_JS_KARMA_BROWSERS
     */
    fun jsKarmaBrowsers(target: KotlinTarget? = null): String? =
        target?.name?.prefixIfNot("$KOTLIN_JS_KARMA_BROWSERS.")?.let(::property) ?: property(KOTLIN_JS_KARMA_BROWSERS)

    private fun propertyWithDeprecatedVariant(propName: String, deprecatedPropName: String): String? {
        val deprecatedProperty = this.property(deprecatedPropName)
        if (deprecatedProperty != null) {
            SingleWarningPerBuild.show(project, "Project property '$deprecatedPropName' is deprecated. Please use '$propName' instead.")
        }
        return this.property(propName) ?: deprecatedProperty
    }

    private fun booleanProperty(propName: String): Boolean? =
        this.property(propName)?.toBoolean()

    private inline fun <reified T : Enum<T>> enumProperty(
        propName: String,
        defaultValue: T
    ): T = this.property(propName)?.let { enumValueOf<T>(it.toUpperCaseAsciiOnly()) } ?: defaultValue

    /**
     * Looks up the property in the following sources with decreasing priority:
     * 1. Project properties (-P, gradle.properties, etc...)
     * 2. `local.properties`
     *
     * Please prefer using dedicated properties for proper defaults handling.
     * Use this API only if you specifically need declared project properties disregarding defaults.
     */
    @UnsafeApi
    internal fun property(propName: String): String? =
        if (project.hasProperty(propName)) {
            project.property(propName) as? String
        } else {
            localProperties.getProperty(propName)
        }

    private fun propertiesWithPrefix(prefix: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        project.properties.forEach { (name, value) ->
            if (name.startsWith(prefix) && value is String) {
                result.put(name, value)
            }
        }
        localProperties.forEach { (name, value) ->
            if (name is String && name.startsWith(prefix) && value is String) {
                // Project properties have higher priority.
                result.putIfAbsent(name, value)
            }
        }
        return result
    }

    object PropertyNames {
        const val KOTLIN_STDLIB_DEFAULT_DEPENDENCY = "kotlin.stdlib.default.dependency"
        const val KOTLIN_STDLIB_JDK_VARIANTS_VERSION_ALIGNMENT = "kotlin.stdlib.jdk.variants.version.alignment"
        const val KOTLIN_JS_STDLIB_DOM_API_INCLUDED = "kotlin.js.stdlib.dom.api.included"
        const val KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA = "kotlin.mpp.enableGranularSourceSetsMetadata"
        const val KOTLIN_MPP_ENABLE_COMPATIBILITY_METADATA_VARIANT = "kotlin.mpp.enableCompatibilityMetadataVariant"
        const val KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION = "kotlin.mpp.enableCInteropCommonization"
        const val KOTLIN_MPP_HIERARCHICAL_STRUCTURE_BY_DEFAULT = "kotlin.internal.mpp.hierarchicalStructureByDefault"
        const val KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT = "kotlin.mpp.hierarchicalStructureSupport"
        const val KOTLIN_MPP_DEPRECATED_PROPERTIES_NO_WARN = "kotlin.mpp.deprecatedProperties.nowarn"
        const val KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN = "kotlin.mpp.androidGradlePluginCompatibility.nowarn"
        const val KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION = "kotlin.mpp.androidSourceSetLayoutVersion"
        const val KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION_1_NO_WARN = "${KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION}1.nowarn"
        const val KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_ANDROID_STYLE_NO_WARN = "kotlin.mpp.androidSourceSetLayoutV2AndroidStyleDirs.nowarn"
        const val KOTLIN_MPP_IMPORT_ENABLE_SLOW_SOURCES_JAR_RESOLVER = "kotlin.mpp.import.enableSlowSourcesJarResolver"
        const val KOTLIN_MPP_ENABLE_INTRANSITIVE_METADATA_CONFIGURATION = "kotlin.mpp.enableIntransitiveMetadataConfiguration"
        const val KOTLIN_NATIVE_DEPENDENCY_PROPAGATION = "kotlin.native.enableDependencyPropagation"
        const val KOTLIN_NATIVE_CACHE_ORCHESTRATION = "kotlin.native.cacheOrchestration"
        const val KOTLIN_MPP_ENABLE_OPTIMISTIC_NUMBER_COMMONIZATION = "kotlin.mpp.enableOptimisticNumberCommonization"
        const val KOTLIN_MPP_ENABLE_PLATFORM_INTEGER_COMMONIZATION = "kotlin.mpp.enablePlatformIntegerCommonization"
        const val KOTLIN_ABI_SNAPSHOT = "kotlin.incremental.classpath.snapshot.enabled"
        const val KOTLIN_JS_KARMA_BROWSERS = "kotlin.js.browser.karma.browsers"
        const val KOTLIN_BUILD_REPORT_SINGLE_FILE = "kotlin.build.report.single_file"
        const val KOTLIN_BUILD_REPORT_HTTP_URL = "kotlin.build.report.http.url"
        const val KOTLIN_OPTIONS_SUPPRESS_FREEARGS_MODIFICATION_WARNING = "kotlin.options.suppressFreeCompilerArgsModificationWarning"
        const val KOTLIN_NATIVE_USE_XCODE_MESSAGE_STYLE = "kotlin.native.useXcodeMessageStyle"
        const val KOTLIN_COMPILER_USE_PRECISE_COMPILATION_RESULTS_BACKUP = "kotlin.compiler.preciseCompilationResultsBackup"
    }

    companion object {
        internal const val KOTLIN_NATIVE_HOME = "kotlin.native.home"

        private const val CACHED_PROVIDER_EXT_NAME = "kotlin.properties.provider"

        internal const val KOTLIN_NATIVE_IGNORE_INCORRECT_DEPENDENCIES = "kotlin.native.ignoreIncorrectDependencies"

        private const val KOTLIN_NATIVE_BINARY_OPTION_PREFIX = "kotlin.native.binary."

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
