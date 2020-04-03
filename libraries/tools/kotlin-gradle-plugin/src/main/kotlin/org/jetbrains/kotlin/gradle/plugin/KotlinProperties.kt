/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.dsl.NativeDistributionType
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.Companion.jsCompilerProperty
import org.jetbrains.kotlin.gradle.targets.native.DisabledNativeTargetsReporter
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.CacheBuilder
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
import java.io.File
import java.util.*

internal fun PropertiesProvider.mapKotlinTaskProperties(task: AbstractKotlinCompile<*>) {
    coroutines?.let { task.coroutinesFromGradleProperties = it }
    useFallbackCompilerSearch?.let { task.useFallbackCompilerSearch = it }

    if (task is KotlinCompile) {
        incrementalJvm?.let { task.incremental = it }
        usePreciseJavaTracking?.let {
            task.usePreciseJavaTracking = it
        }
    }

    if (task is Kotlin2JsCompile) {
        incrementalJs?.let { task.incremental = it }
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
        get() = property("kotlin.coroutines")?.let { Coroutines.byCompilerArgument(it) }

    val buildReportEnabled: Boolean
        get() = booleanProperty("kotlin.build.report.enable") ?: false

    val buildReportVerbose: Boolean
        get() = booleanProperty("kotlin.build.report.verbose") ?: false

    val buildReportDir: File?
        get() = property("kotlin.build.report.dir")?.let { File(it) }

    val incrementalJvm: Boolean?
        get() = booleanProperty("kotlin.incremental")

    val incrementalJs: Boolean?
        get() = booleanProperty("kotlin.incremental.js")

    val incrementalMultiplatform: Boolean?
        get() = booleanProperty("kotlin.incremental.multiplatform")

    val usePreciseJavaTracking: Boolean?
        get() = booleanProperty("kotlin.incremental.usePreciseJavaTracking")

    val useFallbackCompilerSearch: Boolean?
        get() = booleanProperty("kotlin.useFallbackCompilerSearch")

    val keepMppDependenciesIntactInPoms: Boolean?
        get() = booleanProperty("kotlin.mpp.keepMppDependenciesIntactInPoms")

    val ignorePluginLoadedInMultipleProjects: Boolean?
        get() = booleanProperty("kotlin.pluginLoadedInMultipleProjects.ignore")

    val setJvmTargetFromAndroidCompileOptions: Boolean?
        get() = booleanProperty("kotlin.setJvmTargetFromAndroidCompileOptions")

    val enableGranularSourceSetsMetadata: Boolean?
        get() = booleanProperty("kotlin.mpp.enableGranularSourceSetsMetadata")

    val enableCompatibilityMetadataVariant: Boolean?
        get() = booleanProperty("kotlin.mpp.enableCompatibilityMetadataVariant")

    val ignoreDisabledNativeTargets: Boolean?
        get() = booleanProperty(DisabledNativeTargetsReporter.DISABLE_WARNING_PROPERTY_NAME)

    /**
     * Enables parallel tasks execution within a project with Workers API.
     * Does not enable using actual worker proccesses
     * (Kotlin Daemon can be shared which uses less memory)
     */
    val parallelTasksInProject: Boolean?
        get() = booleanProperty("kotlin.parallel.tasks.in.project")

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
     *  - regular - The default distribution. Includes all platform libraries in 1.3 and generates them at the user side in 1.4.
     *  - restricted - Doesn't include Apple platform libraries. Available for MacOS only and in 1.3 only.
     *  - prebuilt - Includes all platform libraries. Available in 1.4 only. Used to workaround possible problems with library generation at the use side.
     */
    val nativeDistributionType: NativeDistributionType?
        get() {
            var result = property("kotlin.native.distribution.type")?.let {
                NativeDistributionType.byCompilerArgument(it)
            }

            val deprecatedRestricted = booleanProperty("kotlin.native.restrictedDistribution")
            if (result == null && deprecatedRestricted != null) {
                SingleWarningPerBuild.show(
                    project,
                    "Project property 'kotlin.native.restrictedDistribution' is deprecated. Please use 'kotlin.native.distribution.type=restricted' instead"
                )
                result = if (deprecatedRestricted) NativeDistributionType.RESTRICTED else NativeDistributionType.REGULAR
            }

            return result
        }

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
     * Forces to run a compilation in a separate JVM.
     */
    val nativeDisableCompilerDaemon: Boolean?
        get() = booleanProperty("kotlin.native.disableCompilerDaemon")

    /**
     * Dependencies caching strategy. The default is static.
     */
    val nativeCacheKind: NativeCacheKind
        get() = property("kotlin.native.cacheKind")?.let { NativeCacheKind.byCompilerArgument(it) } ?: CacheBuilder.DEFAULT_CACHE_KIND

    /**
     * Generate kotlin/js external declarations from all .d.ts files found in npm modules
     */
    val jsGenerateExternals: Boolean?
        get() = booleanProperty("kotlin.js.experimental.generateKotlinExternals")

    /**
     * Automaticaly discover external .d.ts declarations
     */
    val jsDiscoverTypes: Boolean?
        get() = booleanProperty("kotlin.js.experimental.discoverTypes")

    /**
     * Use Kotlin/JS backend compiler type
     */
    val jsCompiler: KotlinJsCompilerType
        get() = property(jsCompilerProperty)?.let { KotlinJsCompilerType.byArgument(it) } ?: KotlinJsCompilerType.LEGACY

    private fun propertyWithDeprecatedVariant(propName: String, deprecatedPropName: String): String? {
        val deprecatedProperty = property(deprecatedPropName)
        if (deprecatedProperty != null) {
            SingleWarningPerBuild.show(project, "Project property '$deprecatedPropName' is deprecated. Please use '$propName' instead.")
        }
        return property(propName) ?: deprecatedProperty
    }

    private fun booleanProperty(propName: String): Boolean? =
        property(propName)?.toBoolean()

    private fun property(propName: String): String? =
        if (project.hasProperty(propName)) {
            project.property(propName) as? String
        } else {
            localProperties.getProperty(propName)
        }

    companion object {
        internal const val KOTLIN_NATIVE_HOME = "kotlin.native.home"

        private const val CACHED_PROVIDER_EXT_NAME = "kotlin.properties.provider"

        operator fun invoke(project: Project): PropertiesProvider =
            with(project.extensions.extraProperties) {
                if (!has(CACHED_PROVIDER_EXT_NAME)) {
                    set(CACHED_PROVIDER_EXT_NAME, PropertiesProvider(project))
                }
                return get(CACHED_PROVIDER_EXT_NAME) as? PropertiesProvider
                    ?: PropertiesProvider(project) // Fallback if multiple class loaders are involved
            }
    }
}