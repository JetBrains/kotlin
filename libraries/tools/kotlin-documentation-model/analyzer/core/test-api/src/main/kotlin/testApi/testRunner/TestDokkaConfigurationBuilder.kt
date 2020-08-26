package testApi.testRunner

import com.intellij.openapi.application.PathManager
import org.jetbrains.dokka.*
import java.io.File
import java.net.URL

fun dokkaConfiguration(block: TestDokkaConfigurationBuilder.() -> Unit): DokkaConfigurationImpl =
    TestDokkaConfigurationBuilder().apply(block).build()

@DslMarker
annotation class DokkaConfigurationDsl

@DokkaConfigurationDsl
class TestDokkaConfigurationBuilder {
    var outputDir: String = "out"
    var format: String = "html"
    var offlineMode: Boolean = false
    var cacheRoot: String? = null
    var pluginsClasspath: List<File> = emptyList()
    var pluginsConfigurations: Map<String, String> = emptyMap()
    var failOnWarning: Boolean = false
    private val sourceSets = mutableListOf<DokkaSourceSetImpl>()
    fun build() = DokkaConfigurationImpl(
        outputDir = File(outputDir),
        cacheRoot = cacheRoot?.let(::File),
        offlineMode = offlineMode,
        sourceSets = sourceSets.toList(),
        pluginsClasspath = pluginsClasspath,
        pluginsConfiguration = pluginsConfigurations,
        modules = emptyList(),
        failOnWarning = failOnWarning
    )

    fun sourceSets(block: SourceSetsBuilder.() -> Unit) {
        sourceSets.addAll(SourceSetsBuilder().apply(block))
    }
}

@DokkaConfigurationDsl
class SourceSetsBuilder : ArrayList<DokkaSourceSetImpl>() {
    fun sourceSet(block: DokkaSourceSetBuilder.() -> Unit): DokkaConfiguration.DokkaSourceSet =
        DokkaSourceSetBuilder().apply(block).build().apply(::add)
}

@DokkaConfigurationDsl
class DokkaSourceSetBuilder(
    var moduleName: String = "root",
    var moduleDisplayName: String? = null,
    var name: String = "main",
    var displayName: String = "JVM",
    var classpath: List<String> = emptyList(),
    var sourceRoots: List<String> = emptyList(),
    var dependentSourceSets: Set<DokkaSourceSetID> = emptySet(),
    var samples: List<String> = emptyList(),
    var includes: List<String> = emptyList(),
    var includeNonPublic: Boolean = false,
    var includeRootPackage: Boolean = true,
    var reportUndocumented: Boolean = false,
    var skipEmptyPackages: Boolean = false,
    var skipDeprecated: Boolean = false,
    var jdkVersion: Int = 8,
    var languageVersion: String? = null,
    var apiVersion: String? = null,
    var noStdlibLink: Boolean = false,
    var noJdkLink: Boolean = false,
    var suppressedFiles: List<String> = emptyList(),
    var analysisPlatform: String = "jvm",
    var perPackageOptions: List<PackageOptionsImpl> = emptyList(),
    var externalDocumentationLinks: List<ExternalDocumentationLinkImpl> = emptyList(),
    var sourceLinks: List<SourceLinkDefinitionImpl> = emptyList()
) {
    fun build() = DokkaSourceSetImpl(
        moduleDisplayName = moduleDisplayName ?: moduleName,
        displayName = displayName,
        sourceSetID = DokkaSourceSetID(moduleName, name),
        classpath = classpath.map(::File),
        sourceRoots = sourceRoots.map(::File).toSet(),
        dependentSourceSets = dependentSourceSets,
        samples = samples.map(::File).toSet(),
        includes = includes.map(::File).toSet(),
        includeNonPublic = includeNonPublic,
        reportUndocumented = reportUndocumented,
        skipEmptyPackages = skipEmptyPackages,
        skipDeprecated = skipDeprecated,
        jdkVersion = jdkVersion,
        sourceLinks = sourceLinks.toSet(),
        perPackageOptions = perPackageOptions.toList(),
        externalDocumentationLinks = externalDocumentationLinks.toSet(),
        languageVersion = languageVersion,
        apiVersion = apiVersion,
        noStdlibLink = noStdlibLink,
        noJdkLink = noJdkLink,
        suppressedFiles = suppressedFiles.map(::File).toSet(),
        analysisPlatform = Platform.fromString(analysisPlatform)
    )
}
