/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler.Companion.IGNORE_TCSM_OVERFLOW
import org.jetbrains.kotlin.gradle.plugin.Kotlin2JsPlugin.Companion.NOWARN_2JS_FLAG
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.Companion.jsCompilerProperty
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_ABI_SNAPSHOT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_OPTIMISTIC_NUMBER_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_PLATFORM_INTEGER_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_BY_DEFAULT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_DEPENDENCY_PROPAGATION
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.js.dukat.ExternalsOutputFormat
import org.jetbrains.kotlin.gradle.targets.js.dukat.ExternalsOutputFormat.Companion.externalsOutputFormatProperty
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrOutputGranularity
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackMajorVersion
import org.jetbrains.kotlin.gradle.targets.native.DisabledNativeTargetsReporter
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.File
import java.util.*

internal fun PropertiesProvider.mapKotlinTaskProperties(task: AbstractKotlinCompile<*>) {
    if (task is KotlinCompile) {
        incrementalJvm?.let { task.incremental = it }
        usePreciseJavaTracking?.let {
            task.usePreciseJavaTracking = it
        }
        task.classpathSnapshotProperties.useClasspathSnapshot.value(useClasspathSnapshot).disallowChanges()
        useFir?.let {
            if (it == true) {
                task.kotlinOptions.useFir = true
            }
        }
        task.jvmTargetValidationMode.set(jvmTargetValidationMode)
        task.useKotlinAbiSnapshot.value(useKotlinAbiSnapshot).disallowChanges()
    }

    if (task is Kotlin2JsCompile) {
        incrementalJs?.let { task.incremental = it }
        incrementalJsKlib?.let { task.incrementalJsKlib = it }
    }
}

internal fun PropertiesProvider.mapKotlinDaemonProperties(task: CompileUsingKotlinDaemon) {
    kotlinDaemonJvmArgs?.let {
        task.kotlinDaemonJvmArguments.set(it.split("\\s+".toRegex()))
    }
    if (!task.compilerExecutionStrategy.isPresent) {
        task.compilerExecutionStrategy.set(kotlinCompilerExecutionStrategy)
    }
}

internal class PropertiesProvider private constructor(private val project: Project) {
    private val localProperties: Properties by lazy {
        Properties().apply {
            val localPropertiesFile = project.rootProject.file("local.properties")
            if (localPropertiesFile.isFile) {
                localPropertiesFile.inputStream().use {
                    load(it)
                }
            }
        }
    }

    val coroutines: Coroutines?
        get() {
            val propValue = property("kotlin.coroutines")?.let { Coroutines.byCompilerArgument(it) }
            if (propValue != null) {
                SingleWarningPerBuild.show(
                    project,
                    """
                    'kotlin.coroutines' property does nothing since 1.5.0 release 
                    and scheduled to be removed in Kotlin 1.7.0 release!    
                    """.trimIndent()
                )
            }
            return propValue
        }

    val singleBuildMetricsFile: File?
        get() = property("kotlin.internal.single.build.metrics.file")?.let { File(it) }

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
        get() = property("kotlin.build.report.output")?.split(",") ?: emptyList()

    val buildReportLabel: String?
        get() = property("kotlin.build.report.label")

    val buildReportFileOutputDir: File?
        get() = property("kotlin.build.report.file.output_dir")?.let { File(it) }

    val buildReportHttpUrlProperty = "kotlin.build.report.http.url"

    val buildReportHttpUrl: String?
        get() = property(buildReportHttpUrlProperty)

    val buildReportHttpUser: String?
        get() = property("kotlin.build.report.http.user")

    val buildReportHttpPassword: String?
        get() = property("kotlin.build.report.http.password")

    val buildReportMetrics: Boolean
        get() = booleanProperty("kotlin.build.report.metrics") ?: false

    val buildReportVerbose: Boolean
        get() = booleanProperty("kotlin.build.report.verbose") ?: false

    @Deprecated("Please use \"kotlin.build.report.file.output_dir\" property instead")
    val buildReportDir: File?
        get() = property("kotlin.build.report.dir")?.let { File(it) }

    val incrementalJvm: Boolean?
        get() = booleanProperty("kotlin.incremental")

    val incrementalJs: Boolean?
        get() = booleanProperty("kotlin.incremental.js")

    val incrementalJsKlib: Boolean?
        get() = booleanProperty("kotlin.incremental.js.klib")

    val incrementalJsIr: Boolean
        get() = booleanProperty("kotlin.incremental.js.ir") ?: false

    val jsIrOutputGranularity: KotlinJsIrOutputGranularity
        get() = property("kotlin.js.ir.output.granularity")?.let { KotlinJsIrOutputGranularity.byArgument(it) }
            ?: KotlinJsIrOutputGranularity.PER_MODULE

    val incrementalMultiplatform: Boolean?
        get() = booleanProperty("kotlin.incremental.multiplatform")

    val usePreciseJavaTracking: Boolean?
        get() = booleanProperty("kotlin.incremental.usePreciseJavaTracking")

    val useClasspathSnapshot: Boolean
        get() {
            // The feature should be controlled by a Gradle property.
            // Currently, we also allow it to be controlled by a system property to make it easier to test the feature during development.
            // TODO: Remove the system property later.
            val gradleProperty = booleanProperty(CompilerSystemProperties.COMPILE_INCREMENTAL_WITH_ARTIFACT_TRANSFORM.property) ?: false
            val systemProperty = CompilerSystemProperties.COMPILE_INCREMENTAL_WITH_ARTIFACT_TRANSFORM.value.toBooleanLenient() ?: false
            return gradleProperty || systemProperty
        }

    val useKotlinAbiSnapshot: Boolean
        get() = booleanProperty(KOTLIN_ABI_SNAPSHOT) ?: false

    val useFir: Boolean?
        get() = booleanProperty("kotlin.useFir")

    val keepMppDependenciesIntactInPoms: Boolean?
        get() = booleanProperty("kotlin.mpp.keepMppDependenciesIntactInPoms")

    val ignorePluginLoadedInMultipleProjects: Boolean?
        get() = booleanProperty("kotlin.pluginLoadedInMultipleProjects.ignore")

    val setJvmTargetFromAndroidCompileOptions: Boolean?
        get() = booleanProperty("kotlin.setJvmTargetFromAndroidCompileOptions")

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
            return (booleanProperty("kotlin.mpp.enableCompatibilityMetadataVariant") ?: !mppHierarchicalStructureByDefault) &&
                    !experimentalKpmModelMapping
        }

    val enableKotlinToolingMetadataArtifact: Boolean
        get() = booleanProperty("kotlin.mpp.enableKotlinToolingMetadataArtifact") ?: true

    val mppStabilityNoWarn: Boolean?
        get() = booleanProperty(KotlinMultiplatformPlugin.STABILITY_NOWARN_FLAG)

    val mppEnableOptimisticNumberCommonization: Boolean
        get() = booleanProperty(KOTLIN_MPP_ENABLE_OPTIMISTIC_NUMBER_COMMONIZATION) ?: true

    val mppEnablePlatformIntegerCommonization: Boolean
        get() = booleanProperty(KOTLIN_MPP_ENABLE_PLATFORM_INTEGER_COMMONIZATION) ?: false

    val wasmStabilityNoWarn: Boolean
        get() = booleanProperty("kotlin.wasm.stability.nowarn") ?: false

    val experimentalKpmModelMapping: Boolean
        get() = booleanProperty(PropertyNames.KOTLIN_KPM_EXPERIMENTAL_MODEL_MAPPING) ?: false

    val ignoreDisabledNativeTargets: Boolean?
        get() = booleanProperty(DisabledNativeTargetsReporter.DISABLE_WARNING_PROPERTY_NAME)

    val ignoreAbsentAndroidMultiplatformTarget: Boolean
        get() = booleanProperty("kotlin.mpp.absentAndroidTarget.nowarn") ?: false

    val ignoreDisabledCInteropCommonization: Boolean
        get() = booleanProperty("$KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION.nowarn") ?: false

    val ignoreIncorrectNativeDependencies: Boolean?
        get() = booleanProperty(KOTLIN_NATIVE_IGNORE_INCORRECT_DEPENDENCIES)

    /**
     * Enables individual test task reporting for aggregated test tasks.
     *
     * By default individual test tasks will not fail build if this task will be executed,
     * also individual html and xml reports will replaced by one consolidated html report.
     */
    val individualTaskReports: Boolean?
        get() = booleanProperty("kotlin.tests.individualTaskReports")

    /**
     * Allow a user to choose distribution type. The following distribution types are available:
     *  - light - Doesn't include platform libraries and generates them at the user side. For 1.3 corresponds to the restricted distribution.
     *  - prebuilt - Includes all platform libraries.
     */
    val nativeDistributionType: String?
        get() = property("kotlin.native.distribution.type")

    /**
     * Allows overriding Kotlin/Native base download url.
     *
     * When Kotlin/native will try to download native compiler, it will append compiler version and os type to this url.
     */
    val nativeBaseDownloadUrl: String
        get() = property("kotlin.native.distribution.baseDownloadUrl") ?: NativeCompilerDownloader.BASE_DOWNLOAD_URL

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
        get() = property("kotlin.native.platform.libraries.mode")

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
        get() = property("kotlin.native.linkArgs").orEmpty().split(' ').filterNot { it.isBlank() }

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
     * Allows a user to specify additional arguments of a JVM executing KLIB commonizer.
     */
    val commonizerJvmArgs: String?
        get() = propertyWithDeprecatedVariant("kotlin.mpp.commonizerJvmArgs", "kotlin.commonizer.jvmArgs")

    /**
     * Enables experimental commonization of user defined c-interop libraries.
     */
    val enableCInteropCommonization: Boolean
        get() = booleanProperty(KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION) ?: false


    val commonizerLogLevel: String?
        get() = property("kotlin.mpp.commonizerLogLevel")

    val enableNativeDistributionCommonizationCache: Boolean
        get() = booleanProperty("kotlin.mpp.enableNativeDistributionCommonizationCache") ?: true

    val enableIntransitiveMetadataConfiguration: Boolean
        get() = booleanProperty("kotlin.mpp.enableIntransitiveMetadataConfiguration") ?: false

    /**
     * Dependencies caching strategy for all targets that support caches.
     */
    val nativeCacheKind: NativeCacheKind?
        get() = property("kotlin.native.cacheKind")?.let { NativeCacheKind.byCompilerArgument(it) }

    /**
     * Dependencies caching strategy for [target].
     */
    fun nativeCacheKindForTarget(target: KonanTarget): NativeCacheKind? =
        property("kotlin.native.cacheKind.${target.presetName}")?.let { NativeCacheKind.byCompilerArgument(it) }

    /**
     * Ignore overflow in [org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler]
     */
    val ignoreTcsmOverflow: Boolean
        get() = booleanProperty(IGNORE_TCSM_OVERFLOW) ?: false

    /**
     * Generate kotlin/js external declarations from all .d.ts files found in npm modules
     */
    val jsGenerateExternals: Boolean
        get() = booleanProperty("kotlin.js.generate.externals") ?: DEFAULT_GENERATE_EXTERNALS

    /**
     * Automaticaly discover external .d.ts declarations
     */
    val jsDiscoverTypes: Boolean?
        get() = booleanProperty("kotlin.js.experimental.discoverTypes")

    /**
     * Use Kotlin/JS backend compiler type
     */
    val jsCompiler: KotlinJsCompilerType
        get() = property(jsCompilerProperty)?.let { KotlinJsCompilerType.byArgumentOrNull(it) } ?: KotlinJsCompilerType.LEGACY

    /**
     * Use Webpack 4 for compatibility
     */
    val webpackMajorVersion: WebpackMajorVersion
        get() = property(WebpackMajorVersion.webpackMajorVersion)?.let { WebpackMajorVersion.byArgument(it) }
            ?.also { version ->
                if (!WebpackMajorVersion.webpackVersionWarning && version != WebpackMajorVersion.DEFAULT) {
                    WebpackMajorVersion.webpackVersionWarning = true
                    project.logger.warn(WebpackMajorVersion.warningMessage)
                }
            }
            ?: WebpackMajorVersion.DEFAULT


    /**
     * Default mode of generating of Dukat
     */
    val externalsOutputFormat: ExternalsOutputFormat?
        get() = property(externalsOutputFormatProperty)?.let { ExternalsOutputFormat.byArgumentOrNull(it) }

    /**
     * Use Kotlin/JS backend compiler type
     */
    val jsGenerateExecutableDefault: Boolean
        get() = (booleanProperty("kotlin.js.generate.executable.default") ?: true).also {
            KotlinBuildStatsService.getInstance()
                ?.report(StringMetrics.JS_GENERATE_EXECUTABLE_DEFAULT, it.toString())
        }

    val noWarn2JsPlugin: Boolean
        get() = booleanProperty(NOWARN_2JS_FLAG) ?: false

    val stdlibDefaultDependency: Boolean
        get() = booleanProperty("kotlin.stdlib.default.dependency") ?: true

    val kotlinTestInferJvmVariant: Boolean
        get() = booleanProperty("kotlin.test.infer.jvm.variant") ?: true

    enum class JvmTargetValidationMode {
        IGNORE, WARNING, ERROR
    }

    val jvmTargetValidationMode: JvmTargetValidationMode
        get() = enumProperty("kotlin.jvm.target.validation.mode", JvmTargetValidationMode.WARNING)

    val kotlinDaemonJvmArgs: String?
        get() = property("kotlin.daemon.jvmargs")

    val kotlinCompilerExecutionStrategy: KotlinCompilerExecutionStrategy
        get() {
            val gradleProperty = property("kotlin.compiler.execution.strategy")
            // system property is for backward compatibility
            val value = (gradleProperty ?: System.getProperty("kotlin.compiler.execution.strategy"))?.toLowerCase()
            return KotlinCompilerExecutionStrategy.fromProperty(value)
        }

    private fun propertyWithDeprecatedVariant(propName: String, deprecatedPropName: String): String? {
        val deprecatedProperty = property(deprecatedPropName)
        if (deprecatedProperty != null) {
            SingleWarningPerBuild.show(project, "Project property '$deprecatedPropName' is deprecated. Please use '$propName' instead.")
        }
        return property(propName) ?: deprecatedProperty
    }

    private fun booleanProperty(propName: String): Boolean? =
        property(propName)?.toBoolean()

    private inline fun <reified T : Enum<T>> enumProperty(
        propName: String,
        defaultValue: T
    ): T = property(propName)?.let { enumValueOf<T>(it.toUpperCase()) } ?: defaultValue

    private fun property(propName: String): String? =
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
        const val KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA = "kotlin.mpp.enableGranularSourceSetsMetadata"
        const val KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION = "kotlin.mpp.enableCInteropCommonization"
        const val KOTLIN_MPP_HIERARCHICAL_STRUCTURE_BY_DEFAULT = "kotlin.internal.mpp.hierarchicalStructureByDefault"
        const val KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT = "kotlin.mpp.hierarchicalStructureSupport"
        const val KOTLIN_NATIVE_DEPENDENCY_PROPAGATION = "kotlin.native.enableDependencyPropagation"
        const val KOTLIN_MPP_ENABLE_OPTIMISTIC_NUMBER_COMMONIZATION = "kotlin.mpp.enableOptimisticNumberCommonization"
        const val KOTLIN_KPM_EXPERIMENTAL_MODEL_MAPPING = "kotlin.kpm.experimentalModelMapping"
        const val KOTLIN_MPP_ENABLE_PLATFORM_INTEGER_COMMONIZATION = "kotlin.mpp.enablePlatformIntegerCommonization"
        const val KOTLIN_ABI_SNAPSHOT = "kotlin.incremental.classpath.snapshot.enabled"
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

        internal val Project.kotlinPropertiesProvider get() = PropertiesProvider(this)
    }
}
