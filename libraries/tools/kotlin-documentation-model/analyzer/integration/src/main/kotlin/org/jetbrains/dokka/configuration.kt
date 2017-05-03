package org.jetbrains.dokka


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
    }
}

data class SerializeOnlyDokkaConfiguration(override val moduleName: String,
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
                                           override val perPackageOptions: List<DokkaConfiguration.PackageOptions>) : DokkaConfiguration
