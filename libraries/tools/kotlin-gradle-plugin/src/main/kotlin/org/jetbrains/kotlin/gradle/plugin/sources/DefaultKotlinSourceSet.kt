/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import groovy.lang.Closure
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.GranularMetadataTransformation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File
import java.lang.reflect.Constructor
import java.util.*

const val METADATA_CONFIGURATION_NAME_SUFFIX = "DependenciesMetadata"

class DefaultKotlinSourceSet(
    private val project: Project,
    val displayName: String,
    fileResolver: FileResolver
) : KotlinSourceSet {

    override val apiConfigurationName: String
        get() = disambiguateName("api")

    override val implementationConfigurationName: String
        get() = disambiguateName("implementation")

    override val compileOnlyConfigurationName: String
        get() = disambiguateName("compileOnly")

    override val runtimeOnlyConfigurationName: String
        get() = disambiguateName("runtimeOnly")

    override val apiMetadataConfigurationName: String
        get() = lowerCamelCaseName(apiConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    override val implementationMetadataConfigurationName: String
        get() = lowerCamelCaseName(implementationConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    override val compileOnlyMetadataConfigurationName: String
        get() = lowerCamelCaseName(compileOnlyConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    override val runtimeOnlyMetadataConfigurationName: String
        get() = lowerCamelCaseName(runtimeOnlyConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    override val kotlin: SourceDirectorySet = createDefaultSourceDirectorySet(project, "$name Kotlin source", fileResolver).apply {
        filter.include("**/*.java")
        filter.include("**/*.kt")
        filter.include("**/*.kts")
    }

    override val languageSettings: LanguageSettingsBuilder = DefaultLanguageSettingsBuilder()

    override val resources: SourceDirectorySet = createDefaultSourceDirectorySet(project, "$name resources", fileResolver)

    override fun kotlin(configureClosure: Closure<Any?>): SourceDirectorySet =
        kotlin.apply { ConfigureUtil.configure(configureClosure, this) }

    override fun languageSettings(configureClosure: Closure<Any?>): LanguageSettingsBuilder = languageSettings.apply {
        ConfigureUtil.configure(configureClosure, this)
    }

    override fun getName(): String = displayName

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
        DefaultKotlinDependencyHandler(this, project).run(configure)

    override fun dependencies(configureClosure: Closure<Any?>) =
        dependencies f@{ ConfigureUtil.configure(configureClosure, this@f) }

    override fun dependsOn(other: KotlinSourceSet) {
        dependsOnSourceSetsImpl.add(other)

        // Fail-fast approach: check on each new added edge and report a circular dependency at once when the edge is added.
        checkForCircularDependencies()

        project.afterEvaluate { defaultSourceSetLanguageSettingsChecker.runAllChecks(this, other) }
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

    internal fun getDependenciesTransformation(scope: KotlinDependencyScope): Iterable<MetadataDependencyTransformation> {
        val metadataDependencyResolutionByModule =
            dependencyTransformations[scope]?.metadataDependencyResolutions
                ?.associateBy { it.dependency.moduleGroup to it.dependency.moduleName }
                ?: emptyMap()

        val baseDir = project.buildDir.resolve("tmp/kotlinMetadata/$name/${scope.scopeName}")
        if (metadataDependencyResolutionByModule.values.any { it is MetadataDependencyResolution.ChooseVisibleSourceSets }) {
            if (baseDir.isDirectory) {
                baseDir.deleteRecursively()
            }
            baseDir.mkdirs()
        }

        return metadataDependencyResolutionByModule.mapNotNull { (groupAndName, resolution) ->
            val (group, name) = groupAndName
            val projectPath = resolution.projectDependency?.dependencyProject?.path
            when (resolution) {
                is MetadataDependencyResolution.KeepOriginalDependency -> null

                is MetadataDependencyResolution.ExcludeAsUnrequested ->
                    MetadataDependencyTransformation(group, name, projectPath, null, emptySet(), emptyMap())

                is MetadataDependencyResolution.ChooseVisibleSourceSets -> {
                    val filesBySourceSet = resolution.getMetadataFilesBySourceSet(
                        baseDir,
                        doProcessFiles = true
                    )
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

private fun createDefaultSourceDirectorySet(project: Project, name: String?, resolver: FileResolver?): SourceDirectorySet {
    if (isGradleVersionAtLeast(5, 0)) {
        @Suppress("UnstableApiUsage")
        val objects = project.objects
        val sourceDirectorySetMethod = objects.javaClass.methods.single { it.name == "sourceDirectorySet" && it.parameterCount == 2 }
        return sourceDirectorySetMethod(objects, name, name) as SourceDirectorySet
    }

    val klass = DefaultSourceDirectorySet::class.java
    val defaultConstructor = klass.constructorOrNull(String::class.java, FileResolver::class.java)

    return if (defaultConstructor != null && defaultConstructor.getAnnotation(java.lang.Deprecated::class.java) == null) {
        // TODO: drop when gradle < 2.12 are obsolete
        defaultConstructor.newInstance(name, resolver)
    } else {
        val directoryFileTreeFactoryClass = Class.forName("org.gradle.api.internal.file.collections.DirectoryFileTreeFactory")
        val alternativeConstructor = klass.getConstructor(String::class.java, FileResolver::class.java, directoryFileTreeFactoryClass)

        val defaultFileTreeFactoryClass = Class.forName("org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory")
        val defaultFileTreeFactory = defaultFileTreeFactoryClass.getConstructor().newInstance()
        alternativeConstructor.newInstance(name, resolver, defaultFileTreeFactory)
    }
}

internal fun KotlinSourceSet.getSourceSetHierarchy(): Set<KotlinSourceSet> {
    val result = mutableSetOf<KotlinSourceSet>()

    fun processSourceSet(sourceSet: KotlinSourceSet) {
        if (result.add(sourceSet)) {
            sourceSet.dependsOn.forEach { processSourceSet(it) }
        }
    }

    processSourceSet(this)
    return result
}


private fun <T> Class<T>.constructorOrNull(vararg parameterTypes: Class<*>): Constructor<T>? =
    try {
        getConstructor(*parameterTypes)
    } catch (e: NoSuchMethodException) {
        null
    }