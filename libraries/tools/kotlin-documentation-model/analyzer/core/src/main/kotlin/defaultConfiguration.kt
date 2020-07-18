package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import java.io.File
import java.net.URL
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

data class DokkaConfigurationImpl(
    override val outputDir: File = DokkaDefaults.outputDir,
    override val cacheRoot: File? = DokkaDefaults.cacheRoot,
    override val offlineMode: Boolean = DokkaDefaults.offlineMode,
    override val sourceSets: List<DokkaSourceSetImpl> = emptyList(),
    override val pluginsClasspath: List<File> = emptyList(),
    override val pluginsConfiguration: Map<String, String> = emptyMap(),
    override val modules: List<DokkaModuleDescriptionImpl> = emptyList(),
    override val failOnWarning: Boolean = DokkaDefaults.failOnWarning
) : DokkaConfiguration


data class DokkaSourceSetImpl(
    override val moduleDisplayName: String,
    override val displayName: String = DokkaDefaults.sourceSetDisplayName,
    override val sourceSetID: DokkaSourceSetID,
    override val classpath: List<File> = emptyList(),
    override val sourceRoots: List<SourceRootImpl>,
    override val dependentSourceSets: Set<DokkaSourceSetID> = emptySet(),
    override val samples: List<File> = emptyList(),
    override val includes: List<File> = emptyList(),
    override val includeNonPublic: Boolean = DokkaDefaults.includeNonPublic,
    override val includeRootPackage: Boolean = DokkaDefaults.includeRootPackage,
    override val reportUndocumented: Boolean = DokkaDefaults.reportUndocumented,
    override val skipEmptyPackages: Boolean = DokkaDefaults.skipEmptyPackages,
    override val skipDeprecated: Boolean = DokkaDefaults.skipDeprecated,
    override val jdkVersion: Int = DokkaDefaults.jdkVersion,
    override val sourceLinks: List<SourceLinkDefinitionImpl> = emptyList(),
    override val perPackageOptions: List<PackageOptionsImpl> = emptyList(),
    override var externalDocumentationLinks: List<ExternalDocumentationLinkImpl> = emptyList(),
    override val languageVersion: String? = null,
    override val apiVersion: String? = null,
    override val noStdlibLink: Boolean = DokkaDefaults.noStdlibLink,
    override val noJdkLink: Boolean = DokkaDefaults.noJdkLink,
    override val suppressedFiles: List<File> = emptyList(),
    override val analysisPlatform: Platform = DokkaDefaults.analysisPlatform
) : DokkaSourceSet

data class DokkaModuleDescriptionImpl(
    override val name: String,
    override val path: File,
    override val docFile: File
) : DokkaConfiguration.DokkaModuleDescription

data class SourceRootImpl(
    override val directory: File
) : DokkaConfiguration.SourceRoot {
    constructor(directoryPath: String) : this(File(directoryPath))
}

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
) : DokkaConfiguration.ExternalDocumentationLink
