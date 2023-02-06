/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.metadata.dependsOnClosureWithInterCompilationDependencies
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.closure
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import java.io.File
import java.util.*
import javax.inject.Inject

const val METADATA_CONFIGURATION_NAME_SUFFIX = "DependenciesMetadata"

abstract class DefaultKotlinSourceSet @Inject constructor(
    final override val project: Project,
    val displayName: String
) : AbstractKotlinSourceSet() {

    override val extras: MutableExtras = mutableExtrasOf()

    override val apiConfigurationName: String
        get() = disambiguateName(API)

    override val implementationConfigurationName: String
        get() = disambiguateName(IMPLEMENTATION)

    override val compileOnlyConfigurationName: String
        get() = disambiguateName(COMPILE_ONLY)

    override val runtimeOnlyConfigurationName: String
        get() = disambiguateName(RUNTIME_ONLY)

    @Deprecated("KT-55312")
    override val apiMetadataConfigurationName: String
        get() = lowerCamelCaseName(apiConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    @Deprecated("KT-55312")
    override val implementationMetadataConfigurationName: String
        get() = lowerCamelCaseName(implementationConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    @Deprecated("KT-55312")
    override val compileOnlyMetadataConfigurationName: String
        get() = lowerCamelCaseName(compileOnlyConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    @Deprecated(message = "KT-55230: RuntimeOnly scope is not supported for metadata dependency transformation")
    override val runtimeOnlyMetadataConfigurationName: String
        get() = lowerCamelCaseName(runtimeOnlyConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    /**
     * Dependencies added to this configuration will not be exposed to any other source set.
     */
    val intransitiveMetadataConfigurationName: String
        get() = lowerCamelCaseName(disambiguateName(INTRANSITIVE), METADATA_CONFIGURATION_NAME_SUFFIX)

    override val kotlin: SourceDirectorySet = createDefaultSourceDirectorySet(project, "$name Kotlin source")

    override val languageSettings: LanguageSettingsBuilder = DefaultLanguageSettingsBuilder()

    override val resources: SourceDirectorySet = createDefaultSourceDirectorySet(project, "$name resources")

    override fun kotlin(configure: SourceDirectorySet.() -> Unit): SourceDirectorySet = kotlin.apply {
        configure(this)
    }

    override fun kotlin(configure: Action<SourceDirectorySet>): SourceDirectorySet =
        kotlin { configure.execute(this) }

    override fun languageSettings(configure: Action<LanguageSettingsBuilder>): LanguageSettingsBuilder = languageSettings {
        configure.execute(this)
    }

    override fun languageSettings(configure: LanguageSettingsBuilder.() -> Unit): LanguageSettingsBuilder =
        languageSettings.apply { configure(this) }

    override fun getName(): String = displayName

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
        DefaultKotlinDependencyHandler(this, project).run(configure)

    override fun dependencies(configure: Action<KotlinDependencyHandler>) =
        dependencies { configure.execute(this) }

    override fun afterDependsOnAdded(other: KotlinSourceSet) {
        project.runProjectConfigurationHealthCheckWhenEvaluated {
            defaultSourceSetLanguageSettingsChecker.runAllChecks(this@DefaultKotlinSourceSet, other)
        }
    }

    override fun toString(): String = "source set $name"

    private val explicitlyAddedCustomSourceFilesExtensions = ArrayList<String>()

    override val customSourceFilesExtensions: Iterable<String>
        get() = Iterable {
            val fromExplicitFilters = kotlin.filter.includes.mapNotNull { pattern ->
                pattern.substringAfterLast('.')
            }
            val merged = (fromExplicitFilters + explicitlyAddedCustomSourceFilesExtensions).filterNot { extension ->
                DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS.any { extension.equals(it, ignoreCase = true) }
                        || extension.any { it == '\\' || it == '/' }
            }.distinct()
            merged.iterator()
        }

    override fun addCustomSourceFilesExtensions(extensions: List<String>) {
        explicitlyAddedCustomSourceFilesExtensions.addAll(extensions)
    }

    /**
     * Returns [GranularMetadataTransformation] for all requested compile dependencies
     * scopes: API, IMPLEMENTATION, COMPILE_ONLY; See [KotlinDependencyScope.compileScopes]
     */
    internal val compileDependenciesTransformation: GranularMetadataTransformation? by lazy {
        createAndConfigureDependencyTransformationForSourceSet()
    }

    private val _requiresVisibilityOf = mutableSetOf<KotlinSourceSet>()

    override val requiresVisibilityOf: MutableSet<KotlinSourceSet>
        get() = Collections.unmodifiableSet(_requiresVisibilityOf)

    override fun requiresVisibilityOf(other: KotlinSourceSet) {
        _requiresVisibilityOf += other
    }

    //region IDE import for Granular source sets metadata

    data class MetadataDependencyTransformation(
        val groupId: String?,
        val moduleName: String,
        val projectPath: String?,
        val projectStructureMetadata: KotlinProjectStructureMetadata?,
        val allVisibleSourceSets: Set<String>,
        /** If empty, then this source set does not see any 'new' source sets of the dependency, compared to its dependsOn parents, but it
         * still does see all what the dependsOn parents see. */
        val useFilesForSourceSets: Map<String, Iterable<File>>
    )

    @Suppress("unused", "UNUSED_PARAMETER") // Used in IDE import, [configurationName] is kept for backward compatibility
    fun getDependenciesTransformation(configurationName: String): Iterable<MetadataDependencyTransformation> {
        return getDependenciesTransformation()
    }

    fun getAdditionalVisibleSourceSets(): List<KotlinSourceSet> = getVisibleSourceSetsFromAssociateCompilations(this)

    internal fun getDependenciesTransformation(): Iterable<MetadataDependencyTransformation> {
        val metadataDependencyResolutionByModule =
            compileDependenciesTransformation.metadataDependencyResolutionsOrEmpty
                .associateBy { ModuleIds.fromComponent(project, it.dependency) }

        return metadataDependencyResolutionByModule.mapNotNull { (groupAndName, resolution) ->
            val (group, name) = groupAndName
            val projectPath = resolution.dependency.currentBuildProjectIdOrNull?.projectPath
            when (resolution) {
                // No metadata transformation leads to original dependency being used during import
                is MetadataDependencyResolution.KeepOriginalDependency -> null

                // We should pass empty transformation for excluded dependencies.
                // No transformation at all will result in a composite metadata jar being used as a dependency.
                is MetadataDependencyResolution.Exclude ->
                    MetadataDependencyTransformation(group, name, projectPath, null, emptySet(), emptyMap())

                is MetadataDependencyResolution.ChooseVisibleSourceSets -> {
                    MetadataDependencyTransformation(
                        group, name, projectPath,
                        resolution.projectStructureMetadata,
                        resolution.allVisibleSourceSetNames,
                        project.transformMetadataLibrariesForIde(resolution)
                    )
                }
            }
        }
    }

    //endregion
}


internal val defaultSourceSetLanguageSettingsChecker =
    FragmentConsistencyChecker<KotlinSourceSet>(
        unitsName = "source sets",
        name = { name },
        checks = FragmentConsistencyChecks<KotlinSourceSet>(
            unitName = "source set",
            languageSettings = { languageSettings }
        ).allChecks
    )


internal fun KotlinSourceSet.disambiguateName(simpleName: String): String {
    val nameParts = listOfNotNull(this.name.takeIf { it != "main" }, simpleName)
    return lowerCamelCaseName(*nameParts.toTypedArray())
}

internal fun createDefaultSourceDirectorySet(project: Project, name: String?): SourceDirectorySet =
    project.objects.sourceDirectorySet(name!!, name)

@Suppress("Unused") // Still part of public API
@Deprecated("Use InternalKotlinSourceSet.dependsOnClosure instead. Will be removed in Kotlin 1.9")
val KotlinSourceSet.dependsOnClosure: Set<KotlinSourceSet> get() = this.internal.dependsOnClosure

@Suppress("Unused") // Still part of public API
@Deprecated("Use InternalKotlinSourceSet.withDependsOnClosure instead. Will be removed in Kotlin 1.9")
val KotlinSourceSet.withDependsOnClosure: Set<KotlinSourceSet> get() = this.internal.withDependsOnClosure

val Iterable<KotlinSourceSet>.dependsOnClosure: Set<KotlinSourceSet>
    get() = flatMap { it.internal.dependsOnClosure }.toSet() - this.toSet()

val Iterable<KotlinSourceSet>.withDependsOnClosure: Set<KotlinSourceSet>
    get() = flatMap { it.internal.withDependsOnClosure }.toSet()

fun KotlinMultiplatformExtension.findSourceSetsDependingOn(sourceSet: KotlinSourceSet): Set<KotlinSourceSet> {
    return sourceSet.closure { seedSourceSet -> sourceSets.filter { otherSourceSet -> seedSourceSet in otherSourceSet.dependsOn } }
}

private fun DefaultKotlinSourceSet.createAndConfigureDependencyTransformationForSourceSet(): GranularMetadataTransformation? {
    // Create only for source sets in multiplatform plugin
    project.multiplatformExtensionOrNull ?: return null

    val parentSourceSetVisibilityProvider = ParentSourceSetVisibilityProvider { componentIdentifier ->
        dependsOnClosureWithInterCompilationDependencies(this).filterIsInstance<DefaultKotlinSourceSet>()
            .mapNotNull { it.compileDependenciesTransformation }
            .flatMap { it.visibleSourceSetsByComponentId[componentIdentifier].orEmpty() }
            .toSet()
    }

    val granularMetadataTransformation = GranularMetadataTransformation(
        params = GranularMetadataTransformation.Params(project, this),
        parentSourceSetVisibilityProvider = parentSourceSetVisibilityProvider
    )

    /**
     *
     * This method is only intended to be called on deprecated DependenciesMetadata configurations to ensure
     * correct behaviour in import.
     *
     * KGP based dependency resolution is therefore unaffected.
     *
     * Ensure that the [configuration] excludes the dependencies that are classified by this [GranularMetadataTransformation] as
     * [MetadataDependencyResolution.Exclude], and uses exactly the same versions as were resolved for the requested
     * dependencies during the transformation.
     */
    fun applyTransformationToLegacyDependenciesMetadataConfiguration(
        configuration: Configuration,
        transformation: GranularMetadataTransformation
    ) {
        // Run this action immediately before the configuration first takes part in dependency resolution:
        configuration.withDependencies {
            val (unrequested, requested) = transformation.metadataDependencyResolutions
                .partition { it is MetadataDependencyResolution.Exclude }

            unrequested.forEach {
                val (group, name) = it.projectDependency(project)?.run {
                    /** Note: the project dependency notation here should be exactly this, group:name,
                     * not from [ModuleIds.fromProjectPathDependency], as `exclude` checks it against the project's group:name  */
                    ModuleDependencyIdentifier(group.toString(), name)
                } ?: ModuleIds.fromComponent(project, it.dependency)
                configuration.exclude(mapOf("group" to group, "module" to name))
            }

            requested.filter { it.dependency.currentBuildProjectIdOrNull == null }.forEach {
                val (group, name) = ModuleIds.fromComponent(project, it.dependency)
                val notation = listOfNotNull(group.orEmpty(), name, it.dependency.moduleVersion?.version).joinToString(":")
                configuration.resolutionStrategy.force(notation)
            }
        }
    }

    @Suppress("DEPRECATION")
    /*
    Older IDEs still rely on resolving the metadata configurations explicitly.
    Dependencies will be coming from extending the newer 'resolvableMetadataConfiguration'.

    the intransitiveMetadataConfigurationName will not extend this mechanism, since it only
    relies on dependencies being added explicitly by the Kotlin Gradle Plugin
    */
    listOf(
        apiMetadataConfigurationName,
        implementationMetadataConfigurationName,
        compileOnlyMetadataConfigurationName
    ).forEach { configurationName ->
        val configuration = project.configurations.getByName(configurationName)
        applyTransformationToLegacyDependenciesMetadataConfiguration(configuration, granularMetadataTransformation)
    }

    return granularMetadataTransformation
}