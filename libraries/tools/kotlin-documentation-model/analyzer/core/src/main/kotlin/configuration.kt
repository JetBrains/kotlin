@file:Suppress("FunctionName")

package org.jetbrains.dokka

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL

object DokkaDefaults {
    const val outputDir = "./dokka"
    const val format: String = "html"
    val cacheRoot: String? = null
    const val offlineMode: Boolean = false
    const val failOnWarning: Boolean = false

    const val includeNonPublic: Boolean = false
    const val includeRootPackage: Boolean = false
    const val reportUndocumented: Boolean = false
    const val skipEmptyPackages: Boolean = false
    const val skipDeprecated: Boolean = false
    const val jdkVersion: Int = 8
    const val noStdlibLink: Boolean = false
    const val noJdkLink: Boolean = false
    val analysisPlatform: Platform = Platform.DEFAULT
    const val suppress: Boolean = false

    const val displayName = "JVM"
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

@Serializable
data class DokkaSourceSetID(
    val moduleName: String,
    val sourceSetName: String
) {
    override fun toString(): String {
        return "$moduleName/$sourceSetName"
    }
}

@OptIn(UnstableDefault::class)
fun DokkaConfigurationImpl(json: String): DokkaConfigurationImpl {
    return Json.parse(DokkaConfigurationImpl.serializer(), json)
}

interface DokkaConfiguration {
    val outputDir: String
    val cacheRoot: String?
    val offlineMode: Boolean
    val failOnWarning: Boolean
    val sourceSets: List<DokkaSourceSet>
    val modules: List<DokkaModuleDescription>
    val pluginsClasspath: List<File>
    val pluginsConfiguration: Map<String, String>

    interface DokkaSourceSet {
        val sourceSetID: DokkaSourceSetID
        val displayName: String
        val moduleDisplayName: String
        val classpath: List<String>
        val sourceRoots: List<SourceRoot>
        val dependentSourceSets: Set<DokkaSourceSetID>
        val samples: List<String>
        val includes: List<String>
        val includeNonPublic: Boolean
        val includeRootPackage: Boolean
        val reportUndocumented: Boolean
        val skipEmptyPackages: Boolean
        val skipDeprecated: Boolean
        val jdkVersion: Int
        val sourceLinks: List<SourceLinkDefinition>
        val perPackageOptions: List<PackageOptions>
        val externalDocumentationLinks: List<ExternalDocumentationLink>
        val languageVersion: String?
        val apiVersion: String?
        val noStdlibLink: Boolean
        val noJdkLink: Boolean
        val suppressedFiles: List<String>
        val analysisPlatform: Platform
    }

    interface SourceRoot {
        val path: String
    }

    interface SourceLinkDefinition {
        val path: String
        val url: String
        val lineSuffix: String?
    }

    interface DokkaModuleDescription {
        val name: String
        val path: String
        val docFile: String
    }

    interface PackageOptions {
        val prefix: String
        val includeNonPublic: Boolean
        val reportUndocumented: Boolean?
        val skipDeprecated: Boolean
        val suppress: Boolean
    }

    interface ExternalDocumentationLink {
        val url: URL
        val packageListUrl: URL

        open class Builder(
            open var url: URL? = null,
            open var packageListUrl: URL? = null
        ) {

            constructor(root: String, packageList: String? = null) : this(URL(root), packageList?.let { URL(it) })

            fun build(): ExternalDocumentationLink =
                if (packageListUrl != null && url != null)
                    ExternalDocumentationLinkImpl(url!!, packageListUrl!!)
                else if (url != null)
                    ExternalDocumentationLinkImpl(url!!, URL(url!!, "package-list"))
                else
                    throw IllegalArgumentException("url or url && packageListUrl must not be null for external documentation link")
        }
    }
}


