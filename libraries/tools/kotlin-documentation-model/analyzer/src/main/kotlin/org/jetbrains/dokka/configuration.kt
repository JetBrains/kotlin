/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

import org.jetbrains.dokka.utilities.cast
import java.io.File
import java.io.Serializable

public object DokkaDefaults {
    public val moduleName: String = "root"
    public val moduleVersion: String? = null
    public val outputDir: File = File("./dokka")
    public const val failOnWarning: Boolean = false
    public const val suppressObviousFunctions: Boolean = true
    public const val suppressInheritedMembers: Boolean = false

    public const val sourceSetDisplayName: String = "JVM"
    public const val sourceSetName: String = "main"
    public val analysisPlatform: Platform = Platform.DEFAULT

    public const val suppress: Boolean = false
    public const val suppressGeneratedFiles: Boolean = true

    public const val skipEmptyPackages: Boolean = true
    public const val skipDeprecated: Boolean = false

    public const val reportUndocumented: Boolean = false

    public const val jdkVersion: Int = 8

    public val documentedVisibilities: Set<DokkaConfiguration.Visibility> = setOf(DokkaConfiguration.Visibility.PUBLIC)

    public val pluginsConfiguration: List<PluginConfigurationImpl> = mutableListOf()

    /**
     * Default value for [DokkaConfiguration.DokkaSourceSet.suppressAnnotatedWith].
     */
    public val suppressAnnotatedWith: Set<String> = emptySet()

}

public enum class Platform(
    public val key: String
) {
    jvm("jvm"),
    js("js"),
    @Deprecated("Use `wasmWasi` or `wasmJs`")
    wasm("wasm"),
    wasmWasi("wasmWasi"),
    wasmJs("wasmJs"),
    native("native"),
    common("common");

    public companion object {
        public val DEFAULT: Platform = jvm

        public fun fromString(key: String): Platform {

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

public fun interface DokkaConfigurationBuilder<T : Any> {
    public fun build(): T
}

public fun <T : Any> Iterable<DokkaConfigurationBuilder<T>>.build(): List<T> = this.map { it.build() }

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
public data class DokkaSourceSetID(
    val scopeId: String,
    val sourceSetName: String
) : Serializable {
    override fun toString(): String {
        return "$scopeId/$sourceSetName"
    }
}

public interface DokkaConfiguration : Serializable {
    public val moduleName: String
    public val moduleVersion: String?
    public val outputDir: File
    public val failOnWarning: Boolean
    public val sourceSets: List<DokkaSourceSet>
    public val modules: List<DokkaModuleDescription>
    public val pluginsClasspath: List<File>
    public val pluginsConfiguration: List<PluginConfiguration>
    public val suppressObviousFunctions: Boolean
    public val includes: Set<File>
    public val suppressInheritedMembers: Boolean

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
    public val finalizeCoroutines: Boolean

    public enum class SerializationFormat : Serializable {
        JSON, XML
    }

    public interface PluginConfiguration : Serializable {
        public val fqPluginName: String
        public val serializationFormat: SerializationFormat
        public val values: String
    }

    /**
     * Each [DokkaSourceSet] is uniquely identified by its [sourceSetID].
     * This means that if two [DokkaSourceSet]s will have the same [sourceSetID] they will be interchangeable.
     * [equals] and [hashCode] must be defined only based on [sourceSetID].
     *
     * **See Also:** [Dokka#3246](https://github.com/Kotlin/dokka/issues/3246)
     */
    public interface DokkaSourceSet : Serializable {
        public val sourceSetID: DokkaSourceSetID
        public val displayName: String
        public val classpath: List<File>
        public val sourceRoots: Set<File>
        public val dependentSourceSets: Set<DokkaSourceSetID>
        public val samples: Set<File>
        public val includes: Set<File>
        public val analysisPlatform: Platform
        public val languageVersion: String?
        public val apiVersion: String?

        public val jdkVersion: Int // used only for builkding links

        public val reportUndocumented: Boolean

        public val perPackageOptions: List<PackageOptions>
        public val suppressedFiles: Set<File>
        public val skipEmptyPackages: Boolean
        public val skipDeprecated: Boolean
        /**
         * A set of annotation fully qualified names (FQNs) to suppress declarations annotated with.
         */
        public val suppressAnnotatedWith: Set<String>
        public val documentedVisibilities: Set<Visibility>
    }

    public enum class Visibility {
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

        public companion object {
            public fun fromString(value: String): Visibility = valueOf(value.uppercase())
        }
    }

    public interface DokkaModuleDescription : Serializable {
        public val name: String
        public val relativePathToOutputDirectory: File
        public val sourceOutputDirectory: File
        public val includes: Set<File>
    }

    public interface PackageOptions : Serializable {
        public val matchingRegex: String

        public val reportUndocumented: Boolean?
        public val skipDeprecated: Boolean
        public val suppress: Boolean
        public val documentedVisibilities: Set<Visibility>
    }

}