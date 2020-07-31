@file:Suppress("FunctionName")

package org.jetbrains.dokka

import org.jetbrains.dokka.utilities.parseJson
import org.jetbrains.dokka.utilities.toJsonString
import java.io.File
import java.io.Serializable
import java.net.URL

object DokkaDefaults {
    val outputDir = File("./dokka")
    const val format: String = "html"
    val cacheRoot: File? = null
    const val offlineMode: Boolean = false
    const val failOnWarning: Boolean = false

    const val includeNonPublic: Boolean = false
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
                else -> throw IllegalArgumentException("Unrecognized platform: $key")
            }
        }
    }
}

interface DokkaConfigurationBuilder<T : Any> {
    fun build(): T
}

fun <T : Any> Iterable<DokkaConfigurationBuilder<T>>.build(): List<T> {
    return this.map { it.build() }
}

data class DokkaSourceSetID(
    val moduleName: String,
    val sourceSetName: String
) : Serializable {
    override fun toString(): String {
        return "$moduleName/$sourceSetName"
    }
}

fun DokkaConfigurationImpl(json: String): DokkaConfigurationImpl = parseJson(json)

fun DokkaConfiguration.toJsonString(): String = toJsonString(this)

interface DokkaConfiguration : Serializable {
    val outputDir: File
    val cacheRoot: File?
    val offlineMode: Boolean
    val failOnWarning: Boolean
    val sourceSets: List<DokkaSourceSet>
    val modules: List<DokkaModuleDescription>
    val pluginsClasspath: Set<File>
    val pluginsConfiguration: Map<String, String>

    interface DokkaSourceSet : Serializable {
        val sourceSetID: DokkaSourceSetID
        val displayName: String
        val moduleDisplayName: String
        val classpath: Set<File>
        val sourceRoots: Set<File>
        val dependentSourceSets: Set<DokkaSourceSetID>
        val samples: Set<File>
        val includes: Set<File>
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
    }

    interface SourceLinkDefinition : Serializable {
        val path: String
        val url: String
        val lineSuffix: String?
    }

    interface DokkaModuleDescription : Serializable {
        val name: String
        val path: File
        val docFile: File
    }

    interface PackageOptions : Serializable {
        val prefix: String
        val includeNonPublic: Boolean
        val reportUndocumented: Boolean?
        val skipDeprecated: Boolean
        val suppress: Boolean
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


