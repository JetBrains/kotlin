/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.KotlinCompilationsModuleGroups
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.isMainCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.GradleKpmDependencyFilesHolder
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.newDependencyFilesHolder
import org.jetbrains.kotlin.gradle.plugin.sources.defaultSourceSetLanguageSettingsChecker
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.withDependsOnClosure
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.project.model.LanguageSettings
import java.util.*
import java.util.concurrent.Callable

open class DefaultCompilationDetails<T : KotlinCommonOptions, CO : CompilerCommonOptions>(
    final override val target: KotlinTarget,
    final override val compilationPurpose: String,
    defaultSourceSet: KotlinSourceSet,
    createCompilerOptions: DefaultCompilationDetails<T, CO>.() -> HasCompilerOptions<CO>,
    createKotlinOptions: DefaultCompilationDetails<T, CO>.() -> T
) : AbstractCompilationDetails<T>(defaultSourceSet), KotlinCompilationData<T> {

    override val compilerOptions: HasCompilerOptions<CO> by lazy { createCompilerOptions() }

    @Deprecated("Replaced with compilerOptions.options", replaceWith = ReplaceWith("compilerOptions.options"))
    override val kotlinOptions: T by lazy { createKotlinOptions() }

    final override val project: Project
        get() = target.project

    override val owner: KotlinTarget
        get() = target

    override val compilationData: KotlinCompilationData<T>
        get() = this

    override val kotlinDependenciesHolder: HasKotlinDependencies
        get() = project.objects.newInstance(
            KotlinDependencyConfigurationsHolder::class.java,
            project,
            lowerCamelCaseName(
                target.disambiguationClassifier,
                compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
                "compilation",
            )
        )

    override val compileDependencyFilesHolder: GradleKpmDependencyFilesHolder = project.newDependencyFilesHolder(
        lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }.orEmpty(),
            "compileClasspath"
        )
    )

    override val compilationClassifier: String?
        get() = target.disambiguationClassifier

    override val kotlinSourceDirectoriesByFragmentName: Map<String, SourceDirectorySet>
        get() = directlyIncludedKotlinSourceSets.withDependsOnClosure.associate { it.name to it.kotlin }

    override val compileKotlinTaskName: String
        get() = lowerCamelCaseName(
            "compile",
            compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            "Kotlin",
            target.targetName
        )

    override val compileAllTaskName: String
        get() = lowerCamelCaseName(target.disambiguationClassifier, compilationPurpose, "classes")

    override val compileDependencyFiles: FileCollection
        get() = compileDependencyFilesHolder.dependencyFiles

    override val output: KotlinCompilationOutput = DefaultKotlinCompilationOutput(
        target.project,
        Callable { target.project.buildDir.resolve("processedResources/${target.targetName}/$compilationPurpose") }
    )

    override val languageSettings: LanguageSettings
        get() = defaultSourceSet.languageSettings

    override val platformType: KotlinPlatformType
        get() = target.platformType

    override val moduleName: String
        get() = KotlinCompilationsModuleGroups.getModuleLeaderCompilation(this).takeIf { it != this }?.ownModuleName ?: ownModuleName

    override val ownModuleName: String
        get() {
            val baseName = project.archivesName.orNull
                ?: project.name
            val suffix = if (isMainCompilationData()) "" else "_$compilationPurpose"
            return filterModuleName("$baseName$suffix")
        }

    override val friendPaths: Iterable<FileCollection>
        get() = mutableListOf<FileCollection>().also { allCollections ->
            associateCompilationsClosure.forEach { allCollections.add(it.compilationData.output.classesDirs) }
            allCollections.add(friendArtifacts)
        }

    private val friendArtifactsTask: TaskProvider<AbstractArchiveTask>? by lazy {
        if (associateCompilationsClosure.any { it.compilationData.isMainCompilationData() }) {
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

    override val associateCompilations: Set<CompilationDetails<*>>
        get() = Collections.unmodifiableSet(_associateCompilations)

    private val _associateCompilations = mutableSetOf<CompilationDetails<*>>()

    override fun associateWith(other: CompilationDetails<*>) {
        require(other.target == target) { "Only associations between compilations of a single target are supported" }
        _associateCompilations += other
        addAssociateCompilationDependencies(other.compilation)
        KotlinCompilationsModuleGroups.unionModules(this, other.compilationData)
        _associateCompilations.add(other)
    }

    @OptIn(ExperimentalStdlibApi::class)
    protected open fun addAssociateCompilationDependencies(other: KotlinCompilation<*>) = with(compilationData) {
        /*
          we add dependencies to compileDependencyConfiguration ('compileClasspath' usually) and runtimeDependency
          ('runtimeClasspath') instead of modifying respective api/implementation/compileOnly/runtimeOnly configs

          This is needed because api/implementation/compileOnly/runtimeOnly are used in IDE Import and will leak
          to dependencies of IDE modules. But they are not needed here, because IDE resolution works inherently
          transitively and symbols from associated compilation will be resolved from source sets of associated
          compilation itself (moreover, direct dependencies are not equivalent to transitive ones because of
          resolution order - e.g. in case of FQNs clash, so it's even harmful)
        */
        project.dependencies.add(compilation.compileOnlyConfigurationName, project.files(Callable { other.output.classesDirs }))
        project.dependencies.add(compilation.runtimeOnlyConfigurationName, project.files(Callable { other.output.allOutputs }))

        compilation.compileDependencyConfigurationName.addAllDependenciesFromOtherConfigurations(
            project,
            other.apiConfigurationName,
            other.implementationConfigurationName,
            other.compileOnlyConfigurationName
        )

        compilation.runtimeDependencyConfigurationName?.addAllDependenciesFromOtherConfigurations(
            project,
            other.apiConfigurationName,
            other.implementationConfigurationName,
            other.runtimeOnlyConfigurationName
        )
    }

    /**
     * Adds `allDependencies` of configurations mentioned in `configurationNames` to configuration named [this] in
     * a lazy manner
     */
    protected fun String.addAllDependenciesFromOtherConfigurations(project: Project, vararg configurationNames: String) {
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

    override fun whenSourceSetAdded(sourceSet: KotlinSourceSet) {
        sourceSet.internal.withDependsOnClosure.forAll { inWithDependsOnClosure ->
            addExactSourceSetEagerly(inWithDependsOnClosure)
        }
    }

    open fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) =
        addSourcesToKotlinCompileTask(
            project,
            compileKotlinTaskName,
            sourceSet.customSourceFilesExtensions,
            addAsCommonSources
        ) { sourceSet.kotlin }


    private val sourceSetsAddedEagerly = hashSetOf<KotlinSourceSet>()

    internal fun addExactSourceSetEagerly(sourceSet: KotlinSourceSet) {
        if (!sourceSetsAddedEagerly.add(sourceSet)) return

        with(target.project) {
            //TODO possibly issue with forced instantiation
            addSourcesToCompileTask(
                sourceSet,
                addAsCommonSources = lazy {
                    target.project.kotlinExtension.sourceSets.any { otherSourceSet ->
                        sourceSet in otherSourceSet.dependsOn
                    }
                }
            )

            // Use `forced = false` since `api`, `implementation`, and `compileOnly` may be missing in some cases like
            // old Java & Android projects:
            addExtendsFromRelation(compilation.apiConfigurationName, sourceSet.apiConfigurationName, forced = false)
            addExtendsFromRelation(
                compilation.implementationConfigurationName,
                sourceSet.implementationConfigurationName,
                forced = false
            )
            addExtendsFromRelation(compilation.compileOnlyConfigurationName, sourceSet.compileOnlyConfigurationName, forced = false)

            if (compilation is KotlinCompilationToRunnableFiles<*>) {
                addExtendsFromRelation(compilation.runtimeOnlyConfigurationName, sourceSet.runtimeOnlyConfigurationName, forced = false)
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
}