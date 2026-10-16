/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

import org.jetbrains.dokka.utilities.cast
import java.io.File
import java.io.Serializable

internal object DokkaDefaults {
    val moduleName: String = "root"
    val moduleVersion: String? = null
    val outputDir: File = File("./dokka")
    const val failOnWarning: Boolean = false
    const val suppressObviousFunctions: Boolean = true
    const val suppressInheritedMembers: Boolean = false

    const val sourceSetDisplayName: String = "JVM"
    const val sourceSetName: String = "main"
    val analysisPlatform: Platform = Platform.DEFAULT

    const val suppress: Boolean = false
    const val suppressGeneratedFiles: Boolean = true

    const val skipEmptyPackages: Boolean = true
    const val skipDeprecated: Boolean = false

    const val reportUndocumented: Boolean = false

    const val jdkVersion: Int = 8

    val documentedVisibilities: Set<DokkaConfiguration.Visibility> = setOf(DokkaConfiguration.Visibility.PUBLIC)

    val pluginsConfiguration: List<PluginConfigurationImpl> = mutableListOf()

    /**
     * Default value for [DokkaConfiguration.DokkaSourceSet.suppressAnnotatedWith].
     */
    val suppressAnnotatedWith: Set<String> = emptySet()

}

internal enum class Platform(
    val key: String
) {
    jvm("jvm"),
    js("js"),
    @Deprecated("Use `wasmWasi` or `wasmJs`")
    wasm("wasm"),
    wasmWasi("wasmWasi"),
    wasmJs("wasmJs"),
    native("native"),
    common("common");

        companion object {
        val DEFAULT: Platform = jvm

        fun fromString(key: String): Platform {

            return when (key.lowercase()) {
                jvm.key -> jvm
                js.key -> js
                @Suppress("DEPRECATION") wasm.key -> @Suppress("DEPRECATION") wasm
                wasmWasi.key.lowercase() -> wasmWasi
                wasmJs.key.lowercase() -> wasmJs
                native.key -> native
                common.key -> common
                "androidjvm", "android" -> jvm
                "metadata" -> common
                else -> throw IllegalArgumentException("Unrecognized platform: $key")
            }
        }
    }
}

internal fun interface DokkaConfigurationBuilder<T : Any> {
    fun build(): T
}

internal fun <T : Any> Iterable<DokkaConfigurationBuilder<T>>.build(): List<T> = this.map { it.build() }

/**
 * Represents a unique identifier for a [DokkaConfiguration.DokkaSourceSet].
 * It should be unique across the whole project.
 *
 * @property scopeId The unique identifier of the scope that this source set is placed in.
 *    Each scope provides only unique source set names.
 *    E.g. One DokkaTask inside the Gradle plugin represents one source set scope, since there cannot be multiple
 *    source sets with the same name. However, a Gradle project will not be a proper scope, since there can be
 *    multiple DokkaTasks that contain source sets with the same name (but different configuration)
 * @property sourceSetName The name of the source set.
 */
internal data class DokkaSourceSetID(
    val scopeId: String,
    val sourceSetName: String
) : Serializable {
    override fun toString(): String {
        return "$scopeId/$sourceSetName"
    }
}

internal interface DokkaConfiguration : Serializable {
    val moduleName: String
    val moduleVersion: String?
    val outputDir: File
    val failOnWarning: Boolean
    val sourceSets: List<DokkaSourceSet>
    val modules: List<DokkaModuleDescription>
    val pluginsClasspath: List<File>
    val pluginsConfiguration: List<PluginConfiguration>
    val suppressObviousFunctions: Boolean
    val includes: Set<File>
    val suppressInheritedMembers: Boolean

    /**
     * Whether coroutines dispatchers should be shutdown after
     * generating documentation via [DokkaGenerator.generate].
     * Additionally, whether the Analysis API and IJ platform global
     * services should be shutdown.
     *
     * If this is enabled, Coroutines *and* the Analysis API *and* IntelliJ
     * platform classes should no longer be used after the documentation is generated once.
     *
     * For example, it effectively stops all background threads associated with
     * coroutines in order to make classes unloadable by the JVM,
     * and rejects all new tasks with [RejectedExecutionException]
     *
     * This is primarily useful for multi-module builds where global services
     * can be shut down after each module's partial task to avoid
     * possible memory leaks.
     *
     * However, this can lead to problems in specific lifecycles where
     * global services are shared and will be reused after documentation generation,
     * and closing it down will leave the build in an inoperable state.
     * One such example is unit tests, for which finalization should be disabled.
     */
    val finalizeCoroutines: Boolean

    enum class SerializationFormat : Serializable {
        JSON, XML
    }

    interface PluginConfiguration : Serializable {
        val fqPluginName: String
        val serializationFormat: SerializationFormat
        val values: String
    }

    /**
     * Each [DokkaSourceSet] is uniquely identified by its [sourceSetID].
     * This means that if two [DokkaSourceSet]s will have the same [sourceSetID] they will be interchangeable.
     * [equals] and [hashCode] must be defined only based on [sourceSetID].
     *
     * **See Also:** [Dokka#3246](https://github.com/Kotlin/dokka/issues/3246)
     */
    interface DokkaSourceSet : Serializable {
        val sourceSetID: DokkaSourceSetID
        val displayName: String
        val classpath: List<File>
        val sourceRoots: Set<File>
        val dependentSourceSets: Set<DokkaSourceSetID>
        val samples: Set<File>
        val includes: Set<File>
        val analysisPlatform: Platform
        val languageVersion: String?
        val apiVersion: String?

        val jdkVersion: Int // used only for builkding links

        val reportUndocumented: Boolean

        val perPackageOptions: List<PackageOptions>
        val suppressedFiles: Set<File>
        val skipEmptyPackages: Boolean
        val skipDeprecated: Boolean
        /**
         * A set of annotation fully qualified names (FQNs) to suppress declarations annotated with.
         */
        val suppressAnnotatedWith: Set<String>
        val documentedVisibilities: Set<Visibility>
    }

    enum class Visibility {
        /**
         * `public` modifier for Java, default visibility for Kotlin
         */
        PUBLIC,

        /**
         * `private` modifier for both Kotlin and Java
         */
        PRIVATE,

        /**
         * `protected` modifier for both Kotlin and Java
         */
        PROTECTED,

        /**
         * Kotlin-specific `internal` modifier
         */
        INTERNAL,

        /**
         * Java-specific package-private visibility (no modifier)
         */
        PACKAGE;

    companion object {
            fun fromString(value: String): Visibility = valueOf(value.uppercase())
        }
    }

    interface DokkaModuleDescription : Serializable {
        val name: String
        val relativePathToOutputDirectory: File
        val sourceOutputDirectory: File
        val includes: Set<File>
    }

    interface PackageOptions : Serializable {
        val matchingRegex: String

        val reportUndocumented: Boolean?
        val skipDeprecated: Boolean
        val suppress: Boolean
        val documentedVisibilities: Set<Visibility>
    }

}
