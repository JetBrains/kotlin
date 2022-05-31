/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.KotlinCompilationsModuleGroups
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.newDependencyFilesHolder
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ofVariantCompileDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.unambiguousNameInProject
import org.jetbrains.kotlin.gradle.plugin.sources.defaultSourceSetLanguageSettingsChecker
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.FragmentMappedKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.dependsOnClosure
import org.jetbrains.kotlin.gradle.plugin.sources.withDependsOnClosure
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.metadata.getMetadataCompilationForSourceSet
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.project.model.LanguageSettings
import org.jetbrains.kotlin.tooling.core.closure
import java.util.*
import java.util.concurrent.Callable

interface CompilationDetails<T : KotlinCommonOptions> {
    val target: KotlinTarget

    val compileDependencyFilesHolder: GradleKpmDependencyFilesHolder

    val kotlinDependenciesHolder: HasKotlinDependencies

    val compilationData: KotlinCompilationData<T>

    fun associateWith(other: CompilationDetails<*>)
    val associateCompilations: Set<CompilationDetails<*>>

    fun source(sourceSet: KotlinSourceSet)

    val directlyIncludedKotlinSourceSets: Set<KotlinSourceSet>
    val defaultSourceSetName: String

    @Suppress("UNCHECKED_CAST")
    val compilation: KotlinCompilation<T>
        get() = target.compilations.getByName(compilationData.compilationPurpose) as KotlinCompilation<T>

    val defaultSourceSet: KotlinSourceSet
        get() = target.project.kotlinExtension.sourceSets.getByName(defaultSourceSetName)
}

interface CompilationDetailsWithRuntime<T : KotlinCommonOptions> : CompilationDetails<T> {
    val runtimeDependencyFilesHolder: GradleKpmDependencyFilesHolder
}

internal val CompilationDetails<*>.associateCompilationsClosure: Iterable<CompilationDetails<*>>
    get() = closure { it.associateCompilations }

open class DefaultCompilationDetails<T : KotlinCommonOptions>(
    final override val target: KotlinTarget,
    final override val compilationPurpose: String,
    createKotlinOptions: DefaultCompilationDetails<*>.() -> T
) : CompilationDetails<T>, KotlinCompilationData<T> {

    override val kotlinOptions: T by lazy { createKotlinOptions() }

    final override val project: Project
        get() = target.project

    override val owner: KotlinTarget
        get() = target

    override val compilationData: KotlinCompilationData<T>
        get() = this

    override val kotlinDependenciesHolder: HasKotlinDependencies
        get() = KotlinDependencyConfigurationsHolder(
            project,
            lowerCamelCaseName(
                target.disambiguationClassifier,
                compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            )
        )

    override val compileDependencyFilesHolder: GradleKpmDependencyFilesHolder = project.newDependencyFilesHolder(
        lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }.orEmpty(),
            "compileClasspath"
        )
    )

    override val directlyIncludedKotlinSourceSets: MutableSet<KotlinSourceSet> = mutableSetOf()

    override val defaultSourceSetName: String
        get() = lowerCamelCaseName(
            target.disambiguationClassifier.takeIf { target !is KotlinMetadataTarget },
            when {
                isMainCompilationData() && target is KotlinMetadataTarget ->
                    KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME // corner case: main compilation of the metadata target compiles commonMain
                else -> compilationPurpose
            }
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
        get() = project.kotlinExtension.sourceSets.getByName(defaultSourceSetName).languageSettings

    override val platformType: KotlinPlatformType
        get() = target.platformType

    override val moduleName: String
        get() = KotlinCompilationsModuleGroups.getModuleLeaderCompilation(this).takeIf { it != this }?.ownModuleName ?: ownModuleName

    override val ownModuleName: String
        get() {
            val baseName = project.archivesName
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


    override fun source(sourceSet: KotlinSourceSet) {
        if (directlyIncludedKotlinSourceSets.add(sourceSet)) {
            target.project.whenEvaluated {
                addExactSourceSetsEagerly(sourceSet.withDependsOnClosure)
            }
        }
    }

    open fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) =
        addSourcesToKotlinCompileTask(
            project,
            compileKotlinTaskName,
            sourceSet.customSourceFilesExtensions,
            addAsCommonSources
        ) { sourceSet.kotlin }

    internal fun addExactSourceSetsEagerly(sourceSets: Set<KotlinSourceSet>) {
        with(target.project) {
            //TODO possibly issue with forced instantiation
            sourceSets.forEach { sourceSet ->
                addSourcesToCompileTask(
                    sourceSet,
                    addAsCommonSources = lazy {
                        CompilationSourceSetUtil.sourceSetsInMultipleCompilations(project).contains(sourceSet.name)
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
}

open class DefaultCompilationDetailsWithRuntime<T : KotlinCommonOptions>(
    target: KotlinTarget,
    compilationPurpose: String,
    createKotlinOptions: DefaultCompilationDetails<*>.() -> T
) : DefaultCompilationDetails<T>(target, compilationPurpose, createKotlinOptions), CompilationDetailsWithRuntime<T> {
    override val runtimeDependencyFilesHolder: GradleKpmDependencyFilesHolder = project.newDependencyFilesHolder(
        lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }.orEmpty(),
            "runtimeClasspath"
        )
    )
}

open class NativeCompilationDetails(
    target: KotlinTarget,
    compilationPurpose: String,
    createKotlinOptions: DefaultCompilationDetails<*>.() -> KotlinCommonOptions
) : DefaultCompilationDetails<KotlinCommonOptions>(
    target,
    compilationPurpose,
    createKotlinOptions
) {
    override val compileDependencyFilesHolder: GradleKpmDependencyFilesHolder = project.newDependencyFilesHolder(
        lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }.orEmpty(),
            "compileKlibraries"
        )
    )

    override val compileAllTaskName: String
        get() = lowerCamelCaseName(target.disambiguationClassifier, compilationPurpose, "klibrary")

    override fun addAssociateCompilationDependencies(other: KotlinCompilation<*>) {
        compileDependencyFilesHolder.dependencyFiles +=
            other.output.classesDirs + project.filesProvider { other.compileDependencyFiles }

        target.project.configurations.named(compilation.implementationConfigurationName).configure { configuration ->
            configuration.extendsFrom(target.project.configurations.findByName(other.implementationConfigurationName))
        }
    }

    override fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) {
        addSourcesToKotlinNativeCompileTask(project, compileKotlinTaskName, { sourceSet.kotlin }, addAsCommonSources)
    }
}

internal open class SharedNativeCompilationDetails(
    target: KotlinTarget,
    compilationPurpose: String,
    createKotlinOptions: DefaultCompilationDetails<*>.() -> KotlinCommonOptions
) :
    DefaultCompilationDetails<KotlinCommonOptions>(
        target,
        compilationPurpose,
        createKotlinOptions
    ) {

    override val friendArtifacts: FileCollection
        get() = super.friendArtifacts.plus(run {
            val project = target.project
            val friendSourceSets = getVisibleSourceSetsFromAssociateCompilations(project, defaultSourceSet).toMutableSet().apply {
                // TODO: implement proper dependsOn/refines compiler args for Kotlin/Native and pass the dependsOn klibs separately;
                //       But for now, those dependencies don't have any special semantics, so passing all them as friends works, too
                addAll(defaultSourceSet.dependsOnClosure)
            }
            project.files(friendSourceSets.mapNotNull { project.getMetadataCompilationForSourceSet(it)?.output?.classesDirs })
        })

    override fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) {
        addSourcesToKotlinNativeCompileTask(project, compileKotlinTaskName, { sourceSet.kotlin }, addAsCommonSources)
    }
}

internal open class MetadataMappedCompilationDetails<T : KotlinCommonOptions>(
    override val target: KotlinMetadataTarget,
    final override val compilationData: AbstractKotlinFragmentMetadataCompilationData<T>
) : CompilationDetails<T> {
    override val compileDependencyFilesHolder: GradleKpmDependencyFilesHolder =
        GradleKpmDependencyFilesHolder.ofMetadataCompilationDependencies(compilationData)

    override val kotlinDependenciesHolder: HasKotlinDependencies
        get() = compilationData.fragment

    override fun associateWith(other: CompilationDetails<*>) {
        throw UnsupportedOperationException("not supported in the mapped model")
    }

    override val associateCompilations: Set<CompilationDetails<*>>
        get() = emptySet()

    override fun source(sourceSet: KotlinSourceSet) {
        throw UnsupportedOperationException("metadata compilations have predefined sources")
    }

    private val underlyingSourceSet: FragmentMappedKotlinSourceSet
        get() = target.project.kotlinExtension.sourceSets.withType<FragmentMappedKotlinSourceSet>().single {
            it.underlyingFragment == compilationData.fragment
        }

    override val directlyIncludedKotlinSourceSets: MutableSet<KotlinSourceSet>
        get() = Collections.unmodifiableSet(hashSetOf(underlyingSourceSet))

    override val defaultSourceSetName: String
        get() = underlyingSourceSet.name
}

internal open class VariantMappedCompilationDetails<T : KotlinCommonOptions>(
    open val variant: GradleKpmVariantInternal,
    override val target: KotlinTarget
) : CompilationDetails<T> {

    @Suppress("UNCHECKED_CAST")
    override val compilationData: KotlinCompilationData<T>
        get() = variant.compilationData as KotlinCompilationData<T>

    override val defaultSourceSetName: String
        get() = variant.unambiguousNameInProject

    override fun source(sourceSet: KotlinSourceSet) {
        compilation.defaultSourceSet.dependsOn(sourceSet)
    }

    override fun associateWith(other: CompilationDetails<*>) {
        if (other !is VariantMappedCompilationDetails<*>)
            error("a mapped variant can't be associated with a legacy one")
        val otherModule = other.variant.containingModule
        if (otherModule === variant.containingModule)
            error("cannot associate $compilation with ${other.compilation} as they are mapped to the same $otherModule")
        variant.containingModule.dependencies { implementation(otherModule) }
    }

    override val associateCompilations: Set<CompilationDetails<*>> get() = emptySet()

    override val compileDependencyFilesHolder: GradleKpmDependencyFilesHolder
        get() = GradleKpmDependencyFilesHolder.ofVariantCompileDependencies(variant)

    override val kotlinDependenciesHolder: HasKotlinDependencies
        get() = variant

    override val directlyIncludedKotlinSourceSets: Set<KotlinSourceSet>
        get() = compilation.defaultSourceSet.dependsOn
}

internal open class VariantMappedCompilationDetailsWithRuntime<T : KotlinCommonOptions>(
    override val variant: GradleKpmVariantWithRuntimeInternal,
    target: KotlinTarget
) : VariantMappedCompilationDetails<T>(variant, target),
    CompilationDetailsWithRuntime<T> {
    override val runtimeDependencyFilesHolder: GradleKpmDependencyFilesHolder
        get() = GradleKpmDependencyFilesHolder.ofVariantRuntimeDependencies(variant)
}

internal class WithJavaCompilationDetails<T : KotlinCommonOptions>(
    target: KotlinTarget,
    compilationPurpose: String,
    createKotlinOptions: DefaultCompilationDetails<*>.() -> T
) : DefaultCompilationDetailsWithRuntime<T>(target, compilationPurpose, createKotlinOptions) {
    override val compilation: KotlinWithJavaCompilation<T>
        get() = super.compilation as KotlinWithJavaCompilation<T>

    val javaSourceSet: SourceSet
        get() = compilation.javaSourceSet

    override val output: KotlinCompilationOutput by lazy { KotlinWithJavaCompilationOutput(compilation) }

    override val compileDependencyFilesHolder: GradleKpmDependencyFilesHolder
        get() = object : GradleKpmDependencyFilesHolder {
            override val dependencyConfigurationName: String by javaSourceSet::compileClasspathConfigurationName
            override var dependencyFiles: FileCollection by javaSourceSet::compileClasspath
        }

    override val runtimeDependencyFilesHolder: GradleKpmDependencyFilesHolder
        get() = object : GradleKpmDependencyFilesHolder {
            override val dependencyConfigurationName: String by javaSourceSet::runtimeClasspathConfigurationName
            override var dependencyFiles: FileCollection by javaSourceSet::runtimeClasspath
        }

    override fun addAssociateCompilationDependencies(other: KotlinCompilation<*>) {
        if (compilationPurpose != SourceSet.TEST_SOURCE_SET_NAME || other.name != SourceSet.MAIN_SOURCE_SET_NAME) {
            super.addAssociateCompilationDependencies(other)
        } // otherwise, do nothing: the Java Gradle plugin adds these dependencies for us, we don't need to add them to the classpath
    }
}

class AndroidCompilationDetails(
    target: KotlinTarget,
    compilationPurpose: String,
    /** Workaround mutual creation order: a compilation is not added to the target's compilations collection until some point, pass it here */
    private val getCompilationInstance: () -> KotlinJvmAndroidCompilation
) : DefaultCompilationDetailsWithRuntime<KotlinJvmOptions>(
    target,
    compilationPurpose,
    { KotlinJvmOptionsImpl() }
) {
    override val compilation: KotlinJvmAndroidCompilation get() = getCompilationInstance()

    val androidVariant get() = compilation.androidVariant

    override val friendArtifacts: FileCollection
        get() = target.project.files(super.friendArtifacts, compilation.testedVariantArtifacts)

    override fun addAssociateCompilationDependencies(other: KotlinCompilation<*>) {
        if ((other as? KotlinJvmAndroidCompilation)?.androidVariant != getTestedVariantData(androidVariant)) {
            super.addAssociateCompilationDependencies(other)
        } // otherwise, do nothing: the Android Gradle plugin adds these dependencies for us, we don't need to add them to the classpath
    }

    override val kotlinDependenciesHolder: HasKotlinDependencies
        get() = object : HasKotlinDependencies by super.kotlinDependenciesHolder {
            override val relatedConfigurationNames: List<String>
                get() = super.relatedConfigurationNames + listOf(
                    "${androidVariant.name}ApiElements",
                    "${androidVariant.name}RuntimeElements",
                    androidVariant.compileConfiguration.name,
                    androidVariant.runtimeConfiguration.name
                )
        }
}

internal class MetadataCompilationDetails(target: KotlinTarget, name: String) :
    DefaultCompilationDetails<KotlinMultiplatformCommonOptions>(
        target,
        name,
        { KotlinMultiplatformCommonOptionsImpl() }
    ) {

    override val friendArtifacts: FileCollection
        get() = super.friendArtifacts.plus(run {
            val project = target.project
            val friendSourceSets = getVisibleSourceSetsFromAssociateCompilations(target.project, defaultSourceSet)
            project.files(friendSourceSets.mapNotNull { target.compilations.findByName(it.name)?.output?.classesDirs })
        })
}

internal open class JsCompilationDetails(
    target: KotlinTarget,
    compilationPurpose: String,
) : DefaultCompilationDetailsWithRuntime<KotlinJsOptions>(target, compilationPurpose, { KotlinJsOptionsImpl() }) {

    protected open class JsCompilationDependenciesHolder(
        val target: KotlinTarget,
        val compilationPurpose: String
    ) : HasKotlinDependencies {
        override val apiConfigurationName: String
            get() = disambiguateNameInPlatform(API)

        override val implementationConfigurationName: String
            get() = disambiguateNameInPlatform(IMPLEMENTATION)

        override val compileOnlyConfigurationName: String
            get() = disambiguateNameInPlatform(COMPILE_ONLY)

        override val runtimeOnlyConfigurationName: String
            get() = disambiguateNameInPlatform(RUNTIME_ONLY)

        protected open val disambiguationClassifierInPlatform: String?
            get() = when (target) {
                is KotlinJsTarget -> target.disambiguationClassifierInPlatform
                is KotlinJsIrTarget -> target.disambiguationClassifierInPlatform
                else -> error("Unexpected target type of $target")
            }

        private fun disambiguateNameInPlatform(simpleName: String): String {
            return lowerCamelCaseName(
                disambiguationClassifierInPlatform,
                compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
                simpleName
            )
        }

        override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
            DefaultKotlinDependencyHandler(this, target.project).run(configure)

        override fun dependencies(configureClosure: Closure<Any?>) =
            dependencies f@{ project.configure(this@f, configureClosure) }
    }

    override val kotlinDependenciesHolder: HasKotlinDependencies
        get() = JsCompilationDependenciesHolder(target, compilationPurpose)

    override val defaultSourceSetName: String
        get() {
            val classifier = if (target is KotlinJsTarget && target.irTarget != null)
                target.disambiguationClassifierInPlatform
            else target.disambiguationClassifier

            return lowerCamelCaseName(
                classifier,
                compilationPurpose
            )
        }
}

internal class JsIrCompilationDetails(target: KotlinTarget, compilationPurpose: String) :
    JsCompilationDetails(target, compilationPurpose) {

    override fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) {
        super.addSourcesToCompileTask(sourceSet, addAsCommonSources)
        (compilation as KotlinJsIrCompilation).allSources.add(sourceSet.kotlin)
    }

    override val defaultSourceSetName: String
        get() {
            val target = target as KotlinJsIrTarget
            return lowerCamelCaseName(
                if (target.mixedMode)
                    target.disambiguationClassifierInPlatform
                else
                    target.disambiguationClassifier,
                compilationPurpose
            )
        }

    private class JsIrCompilationDependencyHolder(target: KotlinTarget, compilationPurpose: String) :
        JsCompilationDependenciesHolder(target, compilationPurpose) {
        override val disambiguationClassifierInPlatform: String?
            get() = (target as KotlinJsIrTarget).disambiguationClassifierInPlatform
    }

    override val kotlinDependenciesHolder: HasKotlinDependencies
        get() = JsIrCompilationDependencyHolder(target, compilationPurpose)
}

internal class KotlinDependencyConfigurationsHolder(
    val project: Project,
    private val configurationNamesPrefix: String?,
) : HasKotlinDependencies {

    override val apiConfigurationName: String
        get() = lowerCamelCaseName(configurationNamesPrefix, API)

    override val implementationConfigurationName: String
        get() = lowerCamelCaseName(configurationNamesPrefix, IMPLEMENTATION)

    override val compileOnlyConfigurationName: String
        get() = lowerCamelCaseName(configurationNamesPrefix, COMPILE_ONLY)

    override val runtimeOnlyConfigurationName: String
        get() = lowerCamelCaseName(configurationNamesPrefix, RUNTIME_ONLY)

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
        DefaultKotlinDependencyHandler(this, project).run(configure)

    override fun dependencies(configureClosure: Closure<Any?>) =
        dependencies f@{ project.configure(this@f, configureClosure) }
}
