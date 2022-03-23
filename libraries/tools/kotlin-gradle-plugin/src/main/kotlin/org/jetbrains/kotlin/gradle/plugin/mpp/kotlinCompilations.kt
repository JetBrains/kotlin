/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.TaskState
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.sources.withDependsOnClosure
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.project.model.LanguageSettings
import org.jetbrains.kotlin.tooling.core.closure
import java.util.*
import java.util.concurrent.Callable

internal fun KotlinCompilation<*>.composeName(prefix: String? = null, suffix: String? = null): String {
    val compilationNamePart = compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }
    val targetNamePart = target.disambiguationClassifier

    return lowerCamelCaseName(prefix, targetNamePart, compilationNamePart, suffix)
}

internal fun KotlinCompilation<*>.isMain(): Boolean =
    name == KotlinCompilation.MAIN_COMPILATION_NAME

/**
 * see https://youtrack.jetbrains.com/issue/KT-45412
 * Some implementations of [KotlinCompilation] are not including their [KotlinCompilation.defaultSourceSet] into [kotlinSourceSet]s
 * This helper function might disappear in the future, once the behaviour of those [KotlinCompilation] implementations is streamlined.
 * @return [KotlinCompilation.kotlinSourceSets] + [KotlinCompilation.defaultSourceSet]
 */
internal val KotlinCompilation<*>.kotlinSourceSetsIncludingDefault: Set<KotlinSourceSet> get() = kotlinSourceSets + defaultSourceSet

abstract class AbstractKotlinCompilation<T : KotlinCommonOptions>(
    internal open val compilationDetails: CompilationDetails<T>,
) : KotlinCompilation<T>,
    HasKotlinDependencies,
    KotlinCompilationData<T> {

    //region HasKotlinDependencies delegation: delegate members manually for better control and prevention of accidental overrides
    private val kotlinDependenciesHolder get() = compilationDetails.kotlinDependenciesHolder
    override val apiConfigurationName: String get() = kotlinDependenciesHolder.apiConfigurationName
    override val implementationConfigurationName: String get() = kotlinDependenciesHolder.implementationConfigurationName
    override val compileOnlyConfigurationName: String get() = kotlinDependenciesHolder.compileOnlyConfigurationName
    override val runtimeOnlyConfigurationName: String get() = kotlinDependenciesHolder.runtimeOnlyConfigurationName
    final override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) = kotlinDependenciesHolder.dependencies(configure)
    final override fun dependencies(configureClosure: Closure<Any?>) = kotlinDependenciesHolder.dependencies(configureClosure)
    //endregion

    //region KotlinCompilationData delegation: delegate members manually for better control and prevention of accidental overrides
    private val compilationData get() = compilationDetails.compilationData
    override val compileAllTaskName: String get() = compilationData.compileAllTaskName
    final override val compilationPurpose: String get() = compilationData.compilationPurpose
    final override val compilationClassifier: String? get() = compilationData.compilationClassifier
    final override val languageSettings: LanguageSettings get() = compilationData.languageSettings
    final override val ownModuleName: String get() = compilationData.ownModuleName
    final override val moduleName: String get() = compilationData.moduleName
    final override val friendPaths: Iterable<FileCollection> get() = compilationData.friendPaths
    final override val platformType: KotlinPlatformType get() = compilationData.platformType
    final override val output: KotlinCompilationOutput get() = compilationData.output
    final override val compileKotlinTaskName: String get() = compilationData.compileKotlinTaskName
    final override val kotlinOptions: T get() = compilationData.kotlinOptions
    final override val kotlinSourceDirectoriesByFragmentName get() = compilationData.kotlinSourceDirectoriesByFragmentName
    //endregion

    override val target: KotlinTarget get() = compilationDetails.target

    final override val compileDependencyConfigurationName: String
        get() = compilationDetails.compileDependencyFilesHolder.dependencyConfigurationName

    final override var compileDependencyFiles: FileCollection
        get() = compilationDetails.compileDependencyFilesHolder.dependencyFiles
        set(value) { compilationDetails.compileDependencyFilesHolder.dependencyFiles = value }

    final override val kotlinSourceSets: MutableSet<KotlinSourceSet> get() =
        when (val details = compilationDetails) {
            is DefaultCompilationDetails -> details.directlyIncludedKotlinSourceSets // mutable in that subtype
            // TODO deprecate mutability of this set. We shouldn't allow mutating it directly anyway;
            else -> details.directlyIncludedKotlinSourceSets.toMutableSet()
        }

    final override val defaultSourceSetName: String get() = compilationDetails.defaultSourceSetName

    final override val compilationName: String get() = compilationDetails.compilationData.compilationPurpose

    override fun kotlinOptions(configure: T.() -> Unit) =
        configure(kotlinOptions)

    @Suppress("UNCHECKED_CAST")
    override val compileKotlinTask: KotlinCompile<T>
        get() = compileKotlinTaskProvider.get()

    @Suppress("UNCHECKED_CAST")
    override val compileKotlinTaskProvider: TaskProvider<out KotlinCompile<T>>
        get() = target.project.locateTask(compileKotlinTaskName) ?: throw GradleException("Couldn't locate  task $compileKotlinTaskName")

    private val attributeContainer by lazy { HierarchyAttributeContainer(target.attributes) }

    override fun getAttributes(): AttributeContainer = attributeContainer

    override val defaultSourceSet: KotlinSourceSet
        get() = target.project.kotlinExtension.sourceSets.getByName(defaultSourceSetName)

    override fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit) = defaultSourceSet.configure()

    override fun source(sourceSet: KotlinSourceSet) {
        compilationDetails.source(sourceSet)
    }

    override fun toString(): String = "compilation '$name' ($target)"

    override fun associateWith(other: KotlinCompilation<*>) {
        compilationDetails.associateWith((other as AbstractKotlinCompilation<*>).compilationDetails)
    }

    override val associateWith: List<KotlinCompilation<*>>
        get() = Collections.unmodifiableList(compilationDetails.associateCompilations.map { it.compilation })

    override val project: Project
        get() = target.project

    override val owner: KotlinTarget
        get() = target

    override val allKotlinSourceSets: Set<KotlinSourceSet>
        get() = compilationDetails.directlyIncludedKotlinSourceSets.withDependsOnClosure

    override val relatedConfigurationNames: List<String>
        get() = compilationDetails.kotlinDependenciesHolder.relatedConfigurationNames + compileDependencyConfigurationName
}

internal fun addCommonSourcesToKotlinCompileTask(
    project: Project,
    taskName: String,
    sourceFileExtensions: Iterable<String>,
    sources: () -> Any
) = addSourcesToKotlinCompileTask(project, taskName, sourceFileExtensions, lazyOf(true), sources)

// FIXME this function dangerously ignores an incorrect type of the task (e.g. if the actual task is a K/N one); consider reporting a failure
internal fun addSourcesToKotlinCompileTask(
    project: Project,
    taskName: String,
    sourceFileExtensions: Iterable<String>,
    addAsCommonSources: Lazy<Boolean> = lazyOf(false),
    /** Evaluated as project.files(...) */
    sources: () -> Any
) {
    fun AbstractKotlinCompile<*>.configureAction() {
        // In this call, the super-implementation of `source` adds the directories files to the roots of the union file tree,
        // so it's OK to pass just the source roots.
        source(Callable(sources))
        sourceFilesExtensions.addAll(sourceFileExtensions)

        // The `commonSourceSet` is passed to the compiler as-is, converted with toList
        commonSourceSet.from(
            Callable<Any> { if (addAsCommonSources.value) sources else emptyList<Any>() }
        )
    }

    project.tasks
        // To configure a task that may have not yet been created at this point, use 'withType-matching-configureEach`:
        .withType(AbstractKotlinCompile::class.java)
        .matching { it.name == taskName }
        .configureEach { compileKotlinTask ->
            compileKotlinTask.configureAction()
        }
}

internal val KotlinCompilation<*>.associateWithClosure: Iterable<KotlinCompilation<*>>
    get() = this.closure { it.associateWith }

abstract class AbstractKotlinCompilationToRunnableFiles<T : KotlinCommonOptions>(
    override val compilationDetails: CompilationDetailsWithRuntime<T>,
) : AbstractKotlinCompilation<T>(compilationDetails),
    KotlinCompilationToRunnableFiles<T> {

    final override val runtimeDependencyConfigurationName: String get() = compilationDetails.runtimeDependencyFilesHolder.dependencyConfigurationName
    final override var runtimeDependencyFiles: FileCollection
        get() = compilationDetails.runtimeDependencyFilesHolder.dependencyFiles
        set(value) {
            compilationDetails.runtimeDependencyFilesHolder.dependencyFiles = value
        }

    override val relatedConfigurationNames: List<String>
        get() = super<AbstractKotlinCompilation>.relatedConfigurationNames + runtimeDependencyConfigurationName
}

internal fun KotlinCompilation<*>.disambiguateName(simpleName: String): String {
    return lowerCamelCaseName(
        target.disambiguationClassifier,
        compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
        simpleName
    )
}

private typealias CompilationsBySourceSet = Map<KotlinSourceSet, Set<KotlinCompilation<*>>>

internal object CompilationSourceSetUtil {
    private const val EXT_NAME = "kotlin.compilations.bySourceSets"

    @Suppress("UNCHECKED_CAST")
    private fun getOrCreateProperty(
        project: Project,
        initialize: Property<CompilationsBySourceSet>.() -> Unit
    ): Property<CompilationsBySourceSet> {
        val ext = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        if (!ext.has(EXT_NAME)) {
            ext.set(EXT_NAME, project.objects.property(Any::class.java as Class<CompilationsBySourceSet>).also(initialize))
        }
        return ext.get(EXT_NAME) as Property<CompilationsBySourceSet>
    }

    // FIXME: the results include the compilations of the metadata target; the callers should care about filtering them out
    //        if they need only the platform compilations
    // TODO: create a separate util function: `platformCompilationsBySourceSets`
    // TODO: visit all call sites, check if they handle the metadata compilations correctly
    fun compilationsBySourceSets(project: Project): CompilationsBySourceSet {
        val compilationNamesBySourceSetName = getOrCreateProperty(project) {
            var shouldFinalizeValue = false

            set(project.provider {
                val kotlinExtension = project.kotlinExtension
                val targets = when (kotlinExtension) {
                    is KotlinMultiplatformExtension -> kotlinExtension.targets
                    is KotlinSingleTargetExtension -> listOf(kotlinExtension.target)
                    else -> emptyList()
                }

                val compilations = targets.flatMap { it.compilations }

                val result = mutableMapOf<KotlinSourceSet, MutableSet<KotlinCompilation<*>>>().apply {
                    compilations.forEach { compilation ->
                        compilation.allKotlinSourceSets.forEach { sourceSet ->
                            getOrPut(sourceSet) { mutableSetOf() }.add(compilation)
                        }
                    }
                    kotlinExtension.sourceSets.forEach { sourceSet ->
                        // For source sets not taking part in any compilation, keep an empty set to avoid errors on access by key
                        getOrPut(sourceSet) { mutableSetOf() }
                    }
                }

                if (shouldFinalizeValue) {
                    set(result)
                }

                return@provider result
            })

            project.gradle.taskGraph.whenReady { shouldFinalizeValue = true }

            // In case the value is first queried after the task graph has been calculated, finalize the value as soon as a task executes:
            object : TaskExecutionListener {
                override fun beforeExecute(task: Task) = Unit
                override fun afterExecute(task: Task, state: TaskState) {
                    shouldFinalizeValue = true
                }
            }
        }

        return compilationNamesBySourceSetName.get()
    }

    fun sourceSetsInMultipleCompilations(project: Project) =
        compilationsBySourceSets(project).mapNotNullTo(mutableSetOf()) { (sourceSet, compilations) ->
            sourceSet.name.takeIf { compilations.size > 1 }
        }
}

private val invalidModuleNameCharactersRegex = """[\\/\r\n\t]""".toRegex()

internal fun filterModuleName(moduleName: String): String =
    moduleName.replace(invalidModuleNameCharactersRegex, "_")
