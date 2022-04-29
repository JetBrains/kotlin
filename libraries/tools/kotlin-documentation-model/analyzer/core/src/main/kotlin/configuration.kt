@file:Suppress("FunctionName")

package org.jetbrains.dokka

import org.jetbrains.dokka.plugability.ConfigurableBlock
import org.jetbrains.dokka.utilities.cast
import org.jetbrains.dokka.utilities.parseJson
import org.jetbrains.dokka.utilities.toJsonString
import java.io.File
import java.io.Serializable
import java.net.URL

object DokkaDefaults {
    val moduleName: String = "root"
    val outputDir = File("./dokka")
    const val format: String = "html"
    val cacheRoot: File? = null
    const val offlineMode: Boolean = false
    const val failOnWarning: Boolean = false
    const val delayTemplateSubstitution: Boolean = false

    const val includeNonPublic: Boolean = false
    val documentedVisibilities: Set<DokkaConfiguration.Visibility> = setOf(DokkaConfiguration.Visibility.PUBLIC)
    const val reportUndocumented: Boolean = false
    const val skipEmptyPackages: Boolean = true
    const val skipDeprecated: Boolean = false
    const val jdkVersion: Int = 8
    const val noStdlibLink: Boolean = false
    const val noJdkLink: Boolean = false
    val analysisPlatform: Platform = Platform.DEFAULT
    const val suppress: Boolean = false

    const val sourceSetDisplayName = "JVM"
    const val sourceSetName = "main"
    val moduleVersion: String? = null
    val pluginsConfiguration = mutableListOf<PluginConfigurationImpl>()
    const val suppressObviousFunctions = true
    const val suppressInheritedMembers = false
}

enum class Platform(val key: String) {
    jvm("jvm"),
    js("js"),
    native("native"),
    common("common");

    companion object {
        val DEFAULT = jvm

        fun fromString(key: String): Platform {
            return when (key.toLowerCase()) {
                jvm.key -> jvm
                js.key -> js
                native.key -> native
                common.key -> common
                "androidjvm", "android" -> jvm
                "metadata" -> common
                else -> throw IllegalArgumentException("Unrecognized platform: $key")
            }
        }
    }
}

fun interface DokkaConfigurationBuilder<T : Any> {
    fun build(): T
}

fun <T : Any> Iterable<DokkaConfigurationBuilder<T>>.build(): List<T> = this.map { it.build() }


data class DokkaSourceSetID(
    /**
     * Unique identifier of the scope that this source set is placed in.
     * Each scope provide only unique source set names.
     *
     * E.g. One DokkaTask inside the Gradle plugin represents one source set scope, since there cannot be multiple
     * source sets with the same name. However, a Gradle project will not be a proper scope, since there can be
     * multple DokkaTasks that contain source sets with the same name (but different configuration)
     */
    val scopeId: String,
    val sourceSetName: String
) : Serializable {
    override fun toString(): String {
        return "$scopeId/$sourceSetName"
    }
}

fun DokkaConfigurationImpl(json: String): DokkaConfigurationImpl = parseJson(json)

/**
 * Global options are applied to all packages and modules and overwrite package configuration.
 *
 * These are handy if we have multiple sourcesets sharing the same global options as it reduces the size of the boilerplate.
 * Otherwise, the user would be enforced to repeat all these options per each sourceset.
 */
data class GlobalDokkaConfiguration(
    val perPackageOptions: List<PackageOptionsImpl>?,
    val externalDocumentationLinks: List<ExternalDocumentationLinkImpl>?,
    val sourceLinks: List<SourceLinkDefinitionImpl>?
)

fun GlobalDokkaConfiguration(json: String): GlobalDokkaConfiguration = parseJson(json)

fun DokkaConfiguration.apply(globals: GlobalDokkaConfiguration): DokkaConfiguration = this.apply {
    sourceSets.forEach {
        it.perPackageOptions.cast<MutableList<DokkaConfiguration.PackageOptions>>().addAll(globals.perPackageOptions ?: emptyList())
    }

    sourceSets.forEach {
        it.externalDocumentationLinks.cast<MutableSet<DokkaConfiguration.ExternalDocumentationLink>>().addAll(globals.externalDocumentationLinks ?: emptyList())
    }

    sourceSets.forEach {
        it.sourceLinks.cast<MutableSet<SourceLinkDefinitionImpl>>().addAll(globals.sourceLinks ?: emptyList())
    }
}

fun DokkaConfiguration.toJsonString(): String = toJsonString(this)
fun <T : ConfigurableBlock> T.toJsonString(): String = toJsonString(this)

interface DokkaConfiguration : Serializable {
    val moduleName: String
    val moduleVersion: String?
    val outputDir: File
    val cacheRoot: File?
    val offlineMode: Boolean
    val failOnWarning: Boolean
    val sourceSets: List<DokkaSourceSet>
    val modules: List<DokkaModuleDescription>
    val pluginsClasspath: List<File>
    val pluginsConfiguration: List<PluginConfiguration>
    val delayTemplateSubstitution: Boolean
    val suppressObviousFunctions: Boolean
    val includes: Set<File>
    val suppressInheritedMembers: Boolean

    /**
     * Whether coroutines dispatchers should be shutdown after
     * generating documentation via [DokkaGenerator.generate].
     *
     * It effectively stops all background threads associated with
     * coroutines in order to make classes unloadable by the JVM,
     * and rejects all new tasks with [RejectedExecutionException]
     *
     * This is primarily useful for multi-module builds where coroutines
     * can be shut down after each module's partial task to avoid
     * possible memory leaks.
     *
     * However, this can lead to problems in specific lifecycles where
     * coroutines are shared and will be reused after documentation generation,
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

    interface DokkaSourceSet : Serializable {
        val sourceSetID: DokkaSourceSetID
        val displayName: String
        val classpath: List<File>
        val sourceRoots: Set<File>
        val dependentSourceSets: Set<DokkaSourceSetID>
        val samples: Set<File>
        val includes: Set<File>
        @Deprecated(message = "Use [documentedVisibilities] property for a more flexible control over documented visibilities")
        val includeNonPublic: Boolean
        val reportUndocumented: Boolean
        val skipEmptyPackages: Boolean
        val skipDeprecated: Boolean
        val jdkVersion: Int
        val sourceLinks: Set<SourceLinkDefinition>
        val perPackageOptions: List<PackageOptions>
        val externalDocumentationLinks: Set<ExternalDocumentationLink>
        val languageVersion: String?
        val apiVersion: String?
        val noStdlibLink: Boolean
        val noJdkLink: Boolean
        val suppressedFiles: Set<File>
        val analysisPlatform: Platform
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
            fun fromString(value: String) = valueOf(value.toUpperCase())
        }
    }

    interface SourceLinkDefinition : Serializable {
        val localDirectory: String
        val remoteUrl: URL
        val remoteLineSuffix: String?
    }

    interface DokkaModuleDescription : Serializable {
        val name: String
        val relativePathToOutputDirectory: File
        val sourceOutputDirectory: File
        val includes: Set<File>
    }

    interface PackageOptions : Serializable {
        val matchingRegex: String
        @Deprecated("Use [documentedVisibilities] property for a more flexible control over documented visibilities")
        val includeNonPublic: Boolean
        val reportUndocumented: Boolean?
        val skipDeprecated: Boolean
        val suppress: Boolean
        val documentedVisibilities: Set<Visibility>
    }

    interface ExternalDocumentationLink : Serializable {
        val url: URL
        val packageListUrl: URL

        companion object
    }
}

fun ExternalDocumentationLink(
    url: URL? = null,
    packageListUrl: URL? = null
): ExternalDocumentationLinkImpl {
    return if (packageListUrl != null && url != null)
        ExternalDocumentationLinkImpl(url, packageListUrl)
    else if (url != null)
        ExternalDocumentationLinkImpl(url, URL(url, "package-list"))
    else
        throw IllegalArgumentException("url or url && packageListUrl must not be null for external documentation link")
}


fun ExternalDocumentationLink(
    url: String, packageListUrl: String? = null
): ExternalDocumentationLinkImpl =
    ExternalDocumentationLink(url.let(::URL), packageListUrl?.let(::URL))


