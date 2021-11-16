/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.KotlinCompilationsModuleGroups
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.defaultSourceSetLanguageSettingsChecker
import org.jetbrains.kotlin.gradle.plugin.sources.resolveAllDependsOnSourceSets
import org.jetbrains.kotlin.gradle.plugin.sources.withAllDependsOnSourceSets
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.*
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
    target: KotlinTarget,
    override val compilationPurpose: String
) : KotlinCompilation<T>, HasKotlinDependencies, KotlinCompilationData<T> {

    override val compilationName: String
        get() = compilationPurpose

    override fun kotlinOptions(configure: T.() -> Unit) =
        configure(kotlinOptions)

    @Suppress("UNCHECKED_CAST")
    override val compileKotlinTask: KotlinCompile<T>
        get() = compileKotlinTaskProvider.get()

    @Suppress("UNCHECKED_CAST")
    override val compileKotlinTaskProvider: TaskProvider<out KotlinCompile<T>>
        get() = target.project.locateTask(compileKotlinTaskName) ?: throw GradleException("Couldn't locate  task $compileKotlinTaskName")

    // Don't declare this property in the constructor to avoid NPE
    // when an overriding property of a subclass is accessed instead.
    @Suppress("CanBePrimaryConstructorProperty")
    override val target: KotlinTarget = target

    private val attributeContainer = HierarchyAttributeContainer(target.attributes)

    override fun getAttributes(): AttributeContainer = attributeContainer

    override val kotlinSourceSets: MutableSet<KotlinSourceSet> = mutableSetOf()

    override val allKotlinSourceSets: Set<KotlinSourceSet>
        get() = kotlinSourceSets + kotlinSourceSets.resolveAllDependsOnSourceSets()

    override val defaultSourceSetName: String
        get() = lowerCamelCaseName(
            target.disambiguationClassifier.takeIf { target !is KotlinMetadataTarget },
            when {
                isMain() && target is KotlinMetadataTarget ->
                    KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME // corner case: main compilation of the metadata target compiles commonMain
                else -> compilationPurpose
            }
        )

    override val defaultSourceSet: KotlinSourceSet
        get() = target.project.kotlinExtension.sourceSets.getByName(defaultSourceSetName)

    override fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit) = defaultSourceSet.configure()

    override val output: KotlinCompilationOutput by lazy {
        DefaultKotlinCompilationOutput(
            target.project,
            Callable { target.project.buildDir.resolve("processedResources/${target.targetName}/$name") })
    }

    open fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) =
        addSourcesToKotlinCompileTask(
            project,
            compileKotlinTaskName,
            sourceSet.customSourceFilesExtensions,
            addAsCommonSources
        ) { sourceSet.kotlin }


    private fun addExactSource(sourceSet: KotlinSourceSet) {
        with(target.project) {
            addSourcesToCompileTask(
                sourceSet,
                addAsCommonSources = lazy {
                    CompilationSourceSetUtil.sourceSetsInMultipleCompilations(project).contains(sourceSet.name)
                }
            )

            // Use `forced = false` since `api`, `implementation`, and `compileOnly` may be missing in some cases like
            // old Java & Android projects:
            addExtendsFromRelation(apiConfigurationName, sourceSet.apiConfigurationName, forced = false)
            addExtendsFromRelation(implementationConfigurationName, sourceSet.implementationConfigurationName, forced = false)
            addExtendsFromRelation(compileOnlyConfigurationName, sourceSet.compileOnlyConfigurationName, forced = false)

            if (this@AbstractKotlinCompilation is KotlinCompilationToRunnableFiles<*>) {
                addExtendsFromRelation(runtimeOnlyConfigurationName, sourceSet.runtimeOnlyConfigurationName, forced = false)
            }

            if (sourceSet.name != defaultSourceSetName) {
                kotlinExtension.sourceSets.findByName(defaultSourceSetName)?.let { defaultSourceSet ->
                    // Temporary solution for checking consistency across source sets participating in a compilation that may
                    // not be interconnected with the dependsOn relation: check the settings as if the default source set of
                    // the compilation depends on the one added to the compilation:
                    defaultSourceSetLanguageSettingsChecker.runAllChecks(
                        defaultSourceSet,
                        sourceSet
                    )
                }
            }
        }
    }

    final override fun source(sourceSet: KotlinSourceSet) {
        val tracked = mutableSetOf(sourceSet)

        fun trackDependsOnEdges(sourceSet: KotlinSourceSet) {
            if (sourceSet !is DefaultKotlinSourceSet) {
                throw UnsupportedOperationException("Source Set ${sourceSet.javaClass.name} not supported")
            }

            sourceSet.dependsOnContainer.all { dependsOnSourceSet ->
                if (tracked.add(dependsOnSourceSet)) {
                    addExactSource(dependsOnSourceSet)
                    trackDependsOnEdges(dependsOnSourceSet)
                }
            }
        }

        if (kotlinSourceSets.add(sourceSet)) {
            addExactSource(sourceSet)
            trackDependsOnEdges(sourceSet)
        }
    }

    override val compileDependencyConfigurationName: String
        get() = lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }.orEmpty(),
            "compileClasspath"
        )

    override val compileKotlinTaskName: String
        get() = lowerCamelCaseName(
            "compile",
            compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            "Kotlin",
            target.targetName
        )

    override val compileAllTaskName: String
        get() = lowerCamelCaseName(target.disambiguationClassifier, compilationPurpose, "classes")

    override lateinit var compileDependencyFiles: FileCollection

    override val apiConfigurationName: String
        get() = disambiguateName(API)

    override val implementationConfigurationName: String
        get() = disambiguateName(IMPLEMENTATION)

    override val compileOnlyConfigurationName: String
        get() = disambiguateName(COMPILE_ONLY)

    override val runtimeOnlyConfigurationName: String
        get() = disambiguateName(RUNTIME_ONLY)

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
        DefaultKotlinDependencyHandler(this, target.project).run(configure)

    override fun dependencies(configureClosure: Closure<Any?>) =
        dependencies f@{ ConfigureUtil.configure(configureClosure, this@f) }

    override fun toString(): String = "compilation '$compilationPurpose' ($target)"

    internal val friendArtifactsTask: TaskProvider<AbstractArchiveTask>? by lazy {
        if (associateWithTransitiveClosure.any { it.isMain() }) {
            val archiveTasks = target.project.tasks.withType(AbstractArchiveTask::class.java)
            if (!archiveTasks.isEmpty()) {
                try {
                    archiveTasks.named(target.artifactsTaskName)
                } catch (e: UnknownTaskException) {
                    // Native tasks does not extend AbstractArchiveTask
                    null
                }
            } else {
                null
            }
        } else {
            null
        }
    }

    /**
     * If a compilation is aware of its associate compilations' outputs being added to the classpath in a transformed or packaged way,
     * it should point to those friend artifact files via this property.
     */
    internal open val friendArtifacts: FileCollection
        get() = with(target.project) {
            val friendArtifactsTaskProvider = friendArtifactsTask
            if (friendArtifactsTaskProvider != null) {
                // In case the main artifact is transitively added to the test classpath via a test dependency on another module
                // that depends on this module's production part, include the main artifact in the friend artifacts, lazily:
                files(
                    Callable {
                        friendArtifactsTaskProvider.flatMap { it.archiveFile }
                    }
                )
            } else files()
        }

    override val friendPaths: Iterable<FileCollection>
        get() = mutableListOf<FileCollection>().also { allCollections ->
            associateWithTransitiveClosure.forEach { allCollections.add(it.output.classesDirs) }
            allCollections.add(friendArtifacts)
        }

    override val moduleName: String
        get() = KotlinCompilationsModuleGroups.getModuleLeaderCompilation(this).takeIf { it != this }?.ownModuleName() ?: ownModuleName

    override fun associateWith(other: KotlinCompilation<*>) {
        require(other.target == target) { "Only associations between compilations of a single target are supported" }
        other as AbstractKotlinCompilation<*>

        _associateWith += other

        addAssociateCompilationDependencies(other)
        KotlinCompilationsModuleGroups.unionModules(this, other)
    }

    /**
     * Adds `allDependencies` of configurations mentioned in `configurationNames` to configuration named [this] in
     * a lazy manner
     */
    private fun String.addAllDependenciesFromOtherConfigurations(project: Project, vararg configurationNames: String) {
        project.configurations.named(this).configure { receiverConfiguration ->
            receiverConfiguration.dependencies.addAllLater(
                project.objects.listProperty(Dependency::class.java).apply {
                    set(
                        project.provider {
                            configurationNames
                                .map { project.configurations.getByName(it) }
                                .flatMap { it.allDependencies }
                        }
                    )
                }
            )
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    protected open fun addAssociateCompilationDependencies(other: KotlinCompilation<*>) {
        /*
          we add dependencies to compileDependencyConfiguration ('compileClasspath' usually) and runtimeDependency
          ('runtimeClasspath') instead of modifying respective api/implementation/compileOnly/runtimeOnly configs

          This is needed because api/implementation/compileOnly/runtimeOnly are used in IDE Import and will leak
          to dependencies of IDE modules. But they are not needed here, because IDE resolution works inherently
          transitively and symbols from associated compilation will be resolved from source sets of associated
          compilation itself (moreover, direct dependencies are not equivalent to transitive ones because of
          resolution order - e.g. in case of FQNs clash, so it's even harmful)
        */
        project.dependencies.add(compileOnlyConfigurationName, project.files(Callable { other.output.classesDirs }))
        project.dependencies.add(runtimeOnlyConfigurationName, project.files(Callable { other.output.allOutputs }))

        compileDependencyConfigurationName.addAllDependenciesFromOtherConfigurations(
            project,
            other.apiConfigurationName,
            other.implementationConfigurationName,
            other.compileOnlyConfigurationName
        )

        runtimeDependencyConfigurationName?.addAllDependenciesFromOtherConfigurations(
            project,
            other.apiConfigurationName,
            other.implementationConfigurationName,
            other.runtimeOnlyConfigurationName
        )
    }

    private val _associateWith: MutableSet<AbstractKotlinCompilation<*>> = mutableSetOf()

    override val associateWith: List<KotlinCompilation<*>>
        get() = Collections.unmodifiableList(_associateWith.toList())

    override val project: Project
        get() = target.project

    override val owner: KotlinTarget
        get() = target

    override val compilationClassifier: String?
        get() = target.disambiguationClassifier

    override val kotlinSourceDirectoriesByFragmentName: Map<String, SourceDirectorySet>
        get() = defaultSourceSet.withAllDependsOnSourceSets().associate { it.name to it.kotlin }

    override val languageSettings: LanguageSettingsBuilder
        get() = defaultSourceSet.languageSettings

    override val platformType: KotlinPlatformType
        get() = target.platformType

    override val ownModuleName: String
        get() = ownModuleName()
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

private fun KotlinCompilation<*>.ownModuleName(): String {
    val project = target.project
    val baseName = project.convention.findPlugin(BasePluginConvention::class.java)?.archivesBaseName
        ?: project.name
    val suffix = if (isMain()) "" else "_$compilationName"
    return filterModuleName("$baseName$suffix")
}

internal val KotlinCompilation<*>.associateWithTransitiveClosure: Iterable<KotlinCompilation<*>>
    get() = mutableSetOf<KotlinCompilation<*>>().apply {
        fun visit(other: KotlinCompilation<*>) {
            if (add(other)) {
                other.associateWith.forEach(::visit)
            }
        }
        associateWith.forEach(::visit)
    }

abstract class AbstractKotlinCompilationToRunnableFiles<T : KotlinCommonOptions>(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilation<T>(target, name), KotlinCompilationToRunnableFiles<T> {
    override val runtimeDependencyConfigurationName: String
        get() = lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            "runtimeClasspath"
        )

    override lateinit var runtimeDependencyFiles: FileCollection
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

            project.gradle.taskGraph.whenReady { shouldFinalizeValue = false } // TODO NOW

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
