package org.jetbrains.dokka

import ru.yole.jkid.CustomSerializer
import ru.yole.jkid.ValueSerializer
import ru.yole.jkid.deserialization.JKidException
import java.io.Serializable
import java.net.URL


class UrlSerializer : ValueSerializer<URL?> {
    override fun fromJsonValue(jsonValue: Any?): URL? {
        if (jsonValue !is String?)
            throw JKidException("Expected string representation of URL, got: $jsonValue")
        return jsonValue?.let { URL(jsonValue) }
    }

    override fun toJsonValue(value: URL?): Any? = value?.toExternalForm()
}

interface DokkaConfiguration {
    val moduleName: String
    val classpath: List<String>
    val sourceRoots: List<SourceRoot>
    val samples: List<String>
    val includes: List<String>
    val outputDir: String
    val format: String
    val includeNonPublic: Boolean
    val includeRootPackage: Boolean
    val reportUndocumented: Boolean
    val skipEmptyPackages: Boolean
    val skipDeprecated: Boolean
    val jdkVersion: Int
    val generateIndexPages: Boolean
    val sourceLinks: List<SourceLinkDefinition>
    val impliedPlatforms: List<String>
    val perPackageOptions: List<PackageOptions>
    val externalDocumentationLinks: List<DokkaConfiguration.ExternalDocumentationLink>
    val languageVersion: String?
    val apiVersion: String?
    val noStdlibLink: Boolean
    val cacheRoot: String?
    val suppressedFiles: List<String>
    val collectInheritedExtensionsFromLibraries: Boolean

    interface SourceRoot {
        val path: String
        val platforms: List<String>
    }

    interface SourceLinkDefinition {
        val path: String
        val url: String
        val lineSuffix: String?
    }

    interface PackageOptions {
        val prefix: String
        val includeNonPublic: Boolean
        val reportUndocumented: Boolean
        val skipDeprecated: Boolean
        val suppress: Boolean
    }

    interface ExternalDocumentationLink {
        @CustomSerializer(UrlSerializer::class) val url: URL
        @CustomSerializer(UrlSerializer::class) val packageListUrl: URL

        open class Builder(open var url: URL? = null,
                           open var packageListUrl: URL? = null) {

            constructor(root: String, packageList: String? = null) : this(URL(root), packageList?.let { URL(it) })

            fun build(): DokkaConfiguration.ExternalDocumentationLink =
                    if (packageListUrl != null && url != null)
                        ExternalDocumentationLinkImpl(url!!, packageListUrl!!)
                    else if (url != null)
                        ExternalDocumentationLinkImpl(url!!, URL(url!!, "package-list"))
                    else
                        throw IllegalArgumentException("url or url && packageListUrl must not be null for external documentation link")
        }
    }
}

data class SerializeOnlyDokkaConfiguration(
    override val moduleName: String,
    override val classpath: List<String>,
    override val sourceRoots: List<DokkaConfiguration.SourceRoot>,
    override val samples: List<String>,
    override val includes: List<String>,
    override val outputDir: String,
    override val format: String,
    override val includeNonPublic: Boolean,
    override val includeRootPackage: Boolean,
    override val reportUndocumented: Boolean,
    override val skipEmptyPackages: Boolean,
    override val skipDeprecated: Boolean,
    override val jdkVersion: Int,
    override val generateIndexPages: Boolean,
    override val sourceLinks: List<DokkaConfiguration.SourceLinkDefinition>,
    override val impliedPlatforms: List<String>,
    override val perPackageOptions: List<DokkaConfiguration.PackageOptions>,
    override val externalDocumentationLinks: List<DokkaConfiguration.ExternalDocumentationLink>,
    override val noStdlibLink: Boolean,
    override val cacheRoot: String?,
    override val suppressedFiles: List<String>,
    override val languageVersion: String?,
    override val apiVersion: String?,
    override val collectInheritedExtensionsFromLibraries: Boolean
) : DokkaConfiguration


data class ExternalDocumentationLinkImpl(@CustomSerializer(UrlSerializer::class) override val url: URL,
                                         @CustomSerializer(UrlSerializer::class) override val packageListUrl: URL) : Serializable, DokkaConfiguration.ExternalDocumentationLink