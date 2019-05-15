package org.jetbrains.dokka

import java.io.File
import java.net.URL

data class DokkaConfigurationImpl(
    override val outputDir: String,
    override val format: String,
    override val generateIndexPages: Boolean,
    override val cacheRoot: String?,
    override val impliedPlatforms: List<String>,
    override val passesConfigurations: List<PassConfigurationImpl>
) : DokkaConfiguration

data class PassConfigurationImpl (
    override val moduleName: String,
    override val classpath: List<String>,
    override val sourceRoots: List<SourceRootImpl>,
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
    override val collectInheritedExtensionsFromLibraries: Boolean,
    override val analysisPlatform: Platform,
    override val targets: List<String>,
    override val sinceKotlin: String?
) : DokkaConfiguration.PassConfiguration


data class SourceRootImpl(
    override val path: String
): DokkaConfiguration.SourceRoot

data class SourceLinkDefinitionImpl(
    override val path: String,
    override val url: String,
    override val lineSuffix: String?
): DokkaConfiguration.SourceLinkDefinition {
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
    override val reportUndocumented: Boolean,
    override val skipDeprecated: Boolean,
    override val suppress: Boolean
): DokkaConfiguration.PackageOptions


data class ExternalDocumentationLinkImpl(override val url: URL,
                                         override val packageListUrl: URL
) : DokkaConfiguration.ExternalDocumentationLink