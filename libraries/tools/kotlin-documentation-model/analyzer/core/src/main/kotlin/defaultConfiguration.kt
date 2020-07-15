package org.jetbrains.dokka

import kotlinx.serialization.*
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import java.io.File
import java.net.URL

@Serializable
data class DokkaConfigurationImpl(
    override val outputDir: String = DokkaDefaults.outputDir,
    override val cacheRoot: String? = DokkaDefaults.cacheRoot,
    override val offlineMode: Boolean = DokkaDefaults.offlineMode,
    override val sourceSets: List<DokkaSourceSetImpl> = emptyList(),
    override val pluginsClasspath: List<@Serializable(with = FileSerializer::class) File> = emptyList(),
    override val pluginsConfiguration: Map<String, String> = emptyMap(),
    override val modules: List<DokkaModuleDescriptionImpl> = emptyList(),
    override val failOnWarning: Boolean = DokkaDefaults.failOnWarning
) : DokkaConfiguration

@Serializable
data class DokkaSourceSetImpl(
    override val moduleDisplayName: String,
    override val displayName: String = DokkaDefaults.sourceSetDisplayName,
    override val sourceSetID: DokkaSourceSetID,
    override val classpath: List<String> = emptyList(),
    override val sourceRoots: List<SourceRootImpl>,
    override val dependentSourceSets: Set<DokkaSourceSetID> = emptySet(),
    override val samples: List<String> = emptyList(),
    override val includes: List<String> = emptyList(),
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
    override val suppressedFiles: List<String> = emptyList(),
    override val analysisPlatform: Platform = DokkaDefaults.analysisPlatform
) : DokkaSourceSet

@Serializable
data class DokkaModuleDescriptionImpl(
    override val name: String,
    override val path: String,
    override val docFile: String
) : DokkaConfiguration.DokkaModuleDescription

@Serializable
data class SourceRootImpl(
    override val path: String
) : DokkaConfiguration.SourceRoot

@Serializable
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

@Serializable
data class PackageOptionsImpl(
    override val prefix: String,
    override val includeNonPublic: Boolean,
    override val reportUndocumented: Boolean?,
    override val skipDeprecated: Boolean,
    override val suppress: Boolean
) : DokkaConfiguration.PackageOptions

@Serializable
data class ExternalDocumentationLinkImpl(
    @Serializable(with = URLSerializer::class) override val url: URL,
    @Serializable(with = URLSerializer::class) override val packageListUrl: URL
) : DokkaConfiguration.ExternalDocumentationLink

private object FileSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor = PrimitiveDescriptor("File", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: File) {
        encoder.encodeString(value.path)
    }

    override fun deserialize(decoder: Decoder): File = File(decoder.decodeString())
}

private object URLSerializer : KSerializer<URL> {
    override val descriptor: SerialDescriptor = PrimitiveDescriptor("URL", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: URL) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): URL = URL(decoder.decodeString())
}
