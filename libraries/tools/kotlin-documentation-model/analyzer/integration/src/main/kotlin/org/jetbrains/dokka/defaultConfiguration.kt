package org.jetbrains.dokka

import kotlinx.serialization.Serializable
import java.io.File
import java.net.URL

@Serializable
data class DokkaConfigurationImpl(
    override val outputDir: String,
    override val format: String,
    override val generateIndexPages: Boolean,
    override val cacheRoot: String?,
    override val impliedPlatforms: List<String>,
    override val passesConfigurations: List<PassConfigurationImpl>
) : DokkaConfiguration

@Serializable
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
    override val sinceKotlin: String
) : DokkaConfiguration.PassConfiguration

@Serializable
data class SourceRootImpl(
    override val path: String
): DokkaConfiguration.SourceRoot

@Serializable
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

@Serializable
data class PackageOptionsImpl(
    override val prefix: String,
    override val includeNonPublic: Boolean,
    override val reportUndocumented: Boolean,
    override val skipDeprecated: Boolean,
    override val suppress: Boolean
): DokkaConfiguration.PackageOptions


@Serializable
data class ExternalDocumentationLinkImpl(@Serializable(with = UrlSerializer::class) override val url: URL,
                                         @Serializable(with = UrlSerializer::class) override val packageListUrl: URL
) : DokkaConfiguration.ExternalDocumentationLink