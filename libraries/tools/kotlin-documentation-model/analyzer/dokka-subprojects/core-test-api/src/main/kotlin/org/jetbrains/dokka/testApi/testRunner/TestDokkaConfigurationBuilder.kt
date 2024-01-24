/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package testApi.testRunner

import org.jetbrains.dokka.*
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.properties.PropertyContainer
import java.io.File

public fun dokkaConfiguration(block: TestDokkaConfigurationBuilder.() -> Unit): DokkaConfigurationImpl =
    TestDokkaConfigurationBuilder().apply(block).build()

@DslMarker
public annotation class DokkaConfigurationDsl

// TODO this class heavily relies on `DokkaSourceSetImpl`, should be refactored to `DokkaSourceSet`
@DokkaConfigurationDsl
public class TestDokkaConfigurationBuilder {

    public var moduleName: String = "root"
        set(value) {
            check(lazySourceSets.isEmpty()) { "Cannot set moduleName after adding source sets" }
            field = value
        }
    public var moduleVersion: String = "1.0-SNAPSHOT"
    public var outputDir: File = File("out")
    public var format: String = "html"
    public var offlineMode: Boolean = false
    public var cacheRoot: String? = null
    public var pluginsClasspath: List<File> = emptyList()
    public var pluginsConfigurations: MutableList<PluginConfigurationImpl> = mutableListOf()
    public var failOnWarning: Boolean = false
    public var modules: List<DokkaModuleDescriptionImpl> = emptyList()
    public var suppressObviousFunctions: Boolean = DokkaDefaults.suppressObviousFunctions
    public var includes: List<File> = emptyList()
    public var suppressInheritedMembers: Boolean = DokkaDefaults.suppressInheritedMembers
    public var delayTemplateSubstitution: Boolean = DokkaDefaults.delayTemplateSubstitution
    private val lazySourceSets = mutableListOf<Lazy<DokkaSourceSetImpl>>()

    public fun build(): DokkaConfigurationImpl = DokkaConfigurationImpl(
        moduleName = moduleName,
        moduleVersion = moduleVersion,
        outputDir = outputDir,
        cacheRoot = cacheRoot?.let(::File),
        offlineMode = offlineMode,
        sourceSets = lazySourceSets.map { it.value }.toList(),
        pluginsClasspath = pluginsClasspath,
        pluginsConfiguration = pluginsConfigurations,
        modules = modules,
        failOnWarning = failOnWarning,
        suppressObviousFunctions = suppressObviousFunctions,
        includes = includes.toSet(),
        suppressInheritedMembers = suppressInheritedMembers,
        delayTemplateSubstitution = delayTemplateSubstitution,
        finalizeCoroutines = false
    )

    public fun sourceSets(block: SourceSetsBuilder.() -> Unit) {
        lazySourceSets.addAll(SourceSetsBuilder(moduleName).apply(block))
    }

    public fun sourceSet(block: DokkaSourceSetBuilder.() -> Unit): Lazy<DokkaSourceSetImpl> {
        val lazySourceSet = lazy { DokkaSourceSetBuilder(moduleName).apply(block).build() }
        lazySourceSets.add(lazySourceSet)
        return lazySourceSet
    }

    public fun unattachedSourceSet(block: DokkaSourceSetBuilder.() -> Unit): DokkaSourceSetImpl {
        return DokkaSourceSetBuilder(moduleName).apply(block).build()
    }
}

@DokkaConfigurationDsl
public class SourceSetsBuilder(
    public val moduleName: String
) : ArrayList<Lazy<DokkaSourceSetImpl>>() {
    public fun sourceSet(block: DokkaSourceSetBuilder.() -> Unit): Lazy<DokkaConfiguration.DokkaSourceSet> {
        return lazy { DokkaSourceSetBuilder(moduleName).apply(block).build() }.apply(::add)
    }
}

@DokkaConfigurationDsl
public class DokkaSourceSetBuilder(
    private val moduleName: String,
    public var name: String = "main",
    public var displayName: String = "JVM",
    public var classpath: List<String> = emptyList(),
    public var sourceRoots: List<String> = emptyList(),
    public var dependentSourceSets: Set<DokkaSourceSetID> = emptySet(),
    public var samples: List<String> = emptyList(),
    public var includes: List<String> = emptyList(),
    @Deprecated(message = "Use [documentedVisibilities] property for a more flexible control over documented visibilities")
    public var includeNonPublic: Boolean = false,
    public var documentedVisibilities: Set<DokkaConfiguration.Visibility> = DokkaDefaults.documentedVisibilities,
    public var reportUndocumented: Boolean = false,
    public var skipEmptyPackages: Boolean = false,
    public var skipDeprecated: Boolean = false,
    public var jdkVersion: Int = 8,
    public var languageVersion: String? = null,
    public var apiVersion: String? = null,
    public var noStdlibLink: Boolean = false,
    public var noJdkLink: Boolean = false,
    public var suppressedFiles: List<String> = emptyList(),
    public var analysisPlatform: String = "jvm",
    public var perPackageOptions: List<PackageOptionsImpl> = emptyList(),
    public var externalDocumentationLinks: List<ExternalDocumentationLinkImpl> = emptyList(),
    public var sourceLinks: List<SourceLinkDefinitionImpl> = emptyList()
) {
    @Suppress("DEPRECATION")
    public fun build(): DokkaSourceSetImpl {
        return DokkaSourceSetImpl(
            displayName = displayName,
            sourceSetID = DokkaSourceSetID(moduleName, name),
            classpath = classpath.map(::File),
            sourceRoots = sourceRoots.map(::File).toSet(),
            dependentSourceSets = dependentSourceSets,
            samples = samples.map(::File).toSet(),
            includes = includes.map(::File).toSet(),
            includeNonPublic = includeNonPublic,
            documentedVisibilities = documentedVisibilities,
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
}

public val defaultSourceSet: DokkaSourceSetImpl = DokkaSourceSetImpl(
    displayName = "DEFAULT",
    sourceSetID = DokkaSourceSetID("DEFAULT", "DEFAULT"),
    classpath = emptyList(),
    sourceRoots = emptySet(),
    dependentSourceSets = emptySet(),
    samples = emptySet(),
    includes = emptySet(),
    includeNonPublic = false,
    documentedVisibilities = DokkaDefaults.documentedVisibilities,
    reportUndocumented = false,
    skipEmptyPackages = true,
    skipDeprecated = false,
    jdkVersion = 8,
    sourceLinks = emptySet(),
    perPackageOptions = emptyList(),
    externalDocumentationLinks = emptySet(),
    languageVersion = null,
    apiVersion = null,
    noStdlibLink = false,
    noJdkLink = false,
    suppressedFiles = emptySet(),
    analysisPlatform = Platform.DEFAULT
)

public fun sourceSet(name: String): DokkaConfiguration.DokkaSourceSet {
    return defaultSourceSet.copy(
        displayName = name,
        sourceSetID = defaultSourceSet.sourceSetID.copy(sourceSetName = name)
    )
}

public fun dModule(
    name: String,
    packages: List<DPackage> = emptyList(),
    documentation: SourceSetDependent<DocumentationNode> = emptyMap(),
    expectPresentInSet: DokkaConfiguration.DokkaSourceSet? = null,
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet> = emptySet(),
    extra: PropertyContainer<DModule> = PropertyContainer.empty()
): DModule = DModule(
    name = name,
    packages = packages,
    documentation = documentation,
    expectPresentInSet = expectPresentInSet,
    sourceSets = sourceSets,
    extra = extra
)

public fun dPackage(
    dri: DRI,
    functions: List<DFunction> = emptyList(),
    properties: List<DProperty> = emptyList(),
    classlikes: List<DClasslike> = emptyList(),
    typealiases: List<DTypeAlias> = emptyList(),
    documentation: SourceSetDependent<DocumentationNode> = emptyMap(),
    expectPresentInSet: DokkaConfiguration.DokkaSourceSet? = null,
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet> = emptySet(),
    extra: PropertyContainer<DPackage> = PropertyContainer.empty()
): DPackage = DPackage(
    dri = dri,
    functions = functions,
    properties = properties,
    classlikes = classlikes,
    typealiases = typealiases,
    documentation = documentation,
    expectPresentInSet = expectPresentInSet,
    sourceSets = sourceSets,
    extra = extra
)

public fun documentationNode(vararg texts: String): DocumentationNode {
    return DocumentationNode(
        texts.toList()
            .map { Description(CustomDocTag(listOf(Text(it)), name = "MARKDOWN_FILE")) })
}
