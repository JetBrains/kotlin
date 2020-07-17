package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import java.io.File
import java.net.URL
import java.io.Serializable

data class DokkaConfigurationImpl(
    override val outputDir: String,
    override val cacheRoot: String?,
    override val offlineMode: Boolean,
    override val sourceSets: List<DokkaSourceSetImpl>,
    override val pluginsClasspath: List<File>,
    override val pluginsConfiguration: Map<String, String>,
    override val modules: List<DokkaModuleDescriptionImpl>,
    override val failOnWarning: Boolean
) : DokkaConfiguration

data class DokkaSourceSetImpl(
    override val moduleDisplayName: String,
    override val displayName: String,
    override val sourceSetID: DokkaSourceSetID,
    override val classpath: List<String>,
    override val sourceRoots: List<SourceRootImpl>,
    override val dependentSourceSets: Set<DokkaSourceSetID>,
    override val samples: List<String>,
    override val includes: List<String>,
    override val includeNonPublic: Boolean,
    override val includeRootPackage: Boolean,
    override val reportUndocumented: Boolean,
    override val skipEmptyPackages: Boolean,
    override val skipDeprecated: Boolean,
    override val jdkVersion: Int,
    override val sourceLinks: List<SourceLinkDefinitionImpl>,
    override val perPackageOptions: List<PackageOptionsImpl>,
    override var externalDocumentationLinks: List<ExternalDocumentationLinkImpl>,
    override val languageVersion: String?,
    override val apiVersion: String?,
    override val noStdlibLink: Boolean,
    override val noJdkLink: Boolean,
    override val suppressedFiles: List<String>,
    override val analysisPlatform: Platform
) : DokkaSourceSet

data class DokkaModuleDescriptionImpl(
    override val name: String,
    override val path: String,
    override val docFile: String
) : DokkaConfiguration.DokkaModuleDescription

data class SourceRootImpl(
    override val path: String
) : DokkaConfiguration.SourceRoot

data class SourceLinkDefinitionImpl(
    override val path: String,
    override val url: String,
    override val lineSuffix: String?
) : DokkaConfiguration.SourceLinkDefinition {
    companion object {
        fun parseSourceLinkDefinition(srcLink: String): SourceLinkDefinitionImpl {
            val (path, urlAndLine) = srcLink.split('=')
            return SourceLinkDefinitionImpl(
                File(path).canonicalPath,
                urlAndLine.substringBefore("#"),
                urlAndLine.substringAfter("#", "").let { if (it.isEmpty()) null else "#$it" })
        }
    }
}

data class PackageOptionsImpl(
    override val prefix: String,
    override val includeNonPublic: Boolean,
    override val reportUndocumented: Boolean?,
    override val skipDeprecated: Boolean,
    override val suppress: Boolean
) : DokkaConfiguration.PackageOptions


data class ExternalDocumentationLinkImpl(
    override val url: URL,
    override val packageListUrl: URL
) : DokkaConfiguration.ExternalDocumentationLink, Serializable
