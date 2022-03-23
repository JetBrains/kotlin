/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import groovy.lang.Closure
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.tooling.core.closure
import org.jetbrains.kotlin.tooling.core.withClosure
import java.io.File
import java.util.*

const val METADATA_CONFIGURATION_NAME_SUFFIX = "DependenciesMetadata"

class DefaultKotlinSourceSet(
    private val project: Project,
    val displayName: String
) : KotlinSourceSet {

    override val apiConfigurationName: String
        get() = disambiguateName(API)

    override val implementationConfigurationName: String
        get() = disambiguateName(IMPLEMENTATION)

    override val compileOnlyConfigurationName: String
        get() = disambiguateName(COMPILE_ONLY)

    override val runtimeOnlyConfigurationName: String
        get() = disambiguateName(RUNTIME_ONLY)

    override val apiMetadataConfigurationName: String
        get() = lowerCamelCaseName(apiConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    override val implementationMetadataConfigurationName: String
        get() = lowerCamelCaseName(implementationConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    override val compileOnlyMetadataConfigurationName: String
        get() = lowerCamelCaseName(compileOnlyConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    override val runtimeOnlyMetadataConfigurationName: String
        get() = lowerCamelCaseName(runtimeOnlyConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    /**
     * Dependencies added to this configuration will not be exposed to any other source set.
     */
    val intransitiveMetadataConfigurationName: String
        get() = lowerCamelCaseName(disambiguateName(INTRANSITIVE), METADATA_CONFIGURATION_NAME_SUFFIX)

    override val kotlin: SourceDirectorySet = createDefaultSourceDirectorySet(project, "$name Kotlin source").apply {
        filter.include("**/*.java")
        filter.include("**/*.kt")
        filter.include("**/*.kts")
    }

    override val languageSettings: LanguageSettingsBuilder = DefaultLanguageSettingsBuilder()

    override val resources: SourceDirectorySet = createDefaultSourceDirectorySet(project, "$name resources")

    override fun kotlin(configureClosure: Closure<Any?>): SourceDirectorySet =
        kotlin.apply { ConfigureUtil.configure(configureClosure, this) }

    override fun languageSettings(configureClosure: Closure<Any?>): LanguageSettingsBuilder = languageSettings.apply {
        ConfigureUtil.configure(configureClosure, this)
    }

    override fun languageSettings(configure: LanguageSettingsBuilder.() -> Unit): LanguageSettingsBuilder =
        languageSettings.apply { configure(this) }

    override fun getName(): String = displayName

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
        DefaultKotlinDependencyHandler(this, project).run(configure)

    override fun dependencies(configureClosure: Closure<Any?>) =
        dependencies f@{ ConfigureUtil.configure(configureClosure, this@f) }

    override fun dependsOn(other: KotlinSourceSet) {
        dependsOnSourceSetsImpl.add(other)

        // Fail-fast approach: check on each new added edge and report a circular dependency at once when the edge is added.
        checkForCircularDependencies()

        project.runProjectConfigurationHealthCheckWhenEvaluated {
            defaultSourceSetLanguageSettingsChecker.runAllChecks(this@DefaultKotlinSourceSet, other)
        }
    }

    private val dependsOnSourceSetsImpl = mutableSetOf<KotlinSourceSet>()

    override val dependsOn: Set<KotlinSourceSet>
        get() = dependsOnSourceSetsImpl

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

    internal val dependencyTransformations: MutableMap<KotlinDependencyScope, GranularMetadataTransformation> = mutableMapOf()

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

    @Suppress("unused") // Used in IDE import
    fun getDependenciesTransformation(configurationName: String): Iterable<MetadataDependencyTransformation> {
        val scope = KotlinDependencyScope.values().find {
            project.sourceSetMetadataConfigurationByScope(this, it).name == configurationName
        } ?: return emptyList()

        return getDependenciesTransformation(scope)
    }

    fun getAdditionalVisibleSourceSets(): List<KotlinSourceSet> =
        getVisibleSourceSetsFromAssociateCompilations(project, this)

    internal fun getDependenciesTransformation(scope: KotlinDependencyScope): Iterable<MetadataDependencyTransformation> {
        val metadataDependencyResolutionByModule =
            dependencyTransformations[scope]?.metadataDependencyResolutions
                ?.associateBy { ModuleIds.fromComponent(project, it.dependency) }
                ?: emptyMap()

        val baseDir = SourceSetMetadataStorageForIde.sourceSetStorageWithScope(project, this@DefaultKotlinSourceSet.name, scope)

        if (metadataDependencyResolutionByModule.values.any { it is MetadataDependencyResolution.ChooseVisibleSourceSets }) {
            if (baseDir.isDirectory) {
                baseDir.deleteRecursively()
            }
            baseDir.mkdirs()
        }

        return metadataDependencyResolutionByModule.mapNotNull { (groupAndName, resolution) ->
            val (group, name) = groupAndName
            val projectPath = resolution.projectDependency?.path
            when (resolution) {
                is MetadataDependencyResolution.KeepOriginalDependency -> null

                is MetadataDependencyResolution.ExcludeAsUnrequested ->
                    MetadataDependencyTransformation(group, name, projectPath, null, emptySet(), emptyMap())

                is MetadataDependencyResolution.ChooseVisibleSourceSets -> {
                    val filesBySourceSet = resolution.visibleSourceSetNamesExcludingDependsOn.associateWith { visibleSourceSetName ->
                        resolution.metadataProvider.getSourceSetCompiledMetadata(
                            project,
                            sourceSetName = visibleSourceSetName,
                            outputDirectoryWhenMaterialised = baseDir,
                            materializeFilesIfNecessary = true
                        )
                    }.filter { (_, files) -> files.any(File::exists) }

                    MetadataDependencyTransformation(
                        group, name, projectPath,
                        resolution.projectStructureMetadata,
                        resolution.allVisibleSourceSetNames,
                        filesBySourceSet
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

private fun KotlinSourceSet.checkForCircularDependencies() {
    // If adding an edge creates a cycle, than the source node of the edge belongs to the cycle, so run DFS from that node
    // to check whether it became reachable from itself
    val visited = hashSetOf<KotlinSourceSet>()
    val stack = LinkedHashSet<KotlinSourceSet>() // Store the stack explicitly to pretty-print the cycle

    fun checkReachableRecursively(from: KotlinSourceSet) {
        stack += from
        visited += from

        for (to in from.dependsOn) {
            if (to == this@checkForCircularDependencies)
                throw InvalidUserCodeException(
                    "Circular dependsOn hierarchy found in the Kotlin source sets: " +
                            (stack.toList() + to).joinToString(" -> ") { it.name }
                )

            if (to !in visited) {
                checkReachableRecursively(to)
            }
        }
        stack -= from
    }

    checkReachableRecursively(this@checkForCircularDependencies)
}

internal fun KotlinSourceSet.disambiguateName(simpleName: String): String {
    val nameParts = listOfNotNull(this.name.takeIf { it != "main" }, simpleName)
    return lowerCamelCaseName(*nameParts.toTypedArray())
}

internal fun createDefaultSourceDirectorySet(project: Project, name: String?): SourceDirectorySet =
    project.objects.sourceDirectorySet(name!!, name)


val KotlinSourceSet.dependsOnClosure get() = closure { it.dependsOn }

val KotlinSourceSet.withDependsOnClosure get() = withClosure { it.dependsOn }

val Iterable<KotlinSourceSet>.dependsOnClosure get() = closure<KotlinSourceSet> { it.dependsOn }

val Iterable<KotlinSourceSet>.withDependsOnClosure get() = withClosure<KotlinSourceSet> { it.dependsOn }

internal fun KotlinMultiplatformExtension.findSourceSetsDependingOn(sourceSet: KotlinSourceSet): Set<KotlinSourceSet> {
    return sourceSet.closure { seedSourceSet -> sourceSets.filter { otherSourceSet -> seedSourceSet in otherSourceSet.dependsOn } }
}
