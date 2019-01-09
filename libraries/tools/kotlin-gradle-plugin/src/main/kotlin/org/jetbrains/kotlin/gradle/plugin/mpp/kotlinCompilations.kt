 /*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.sources.defaultSourceSetLanguageSettingsChecker
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File
import java.util.*
import java.util.concurrent.Callable

internal fun KotlinCompilation<*>.composeName(prefix: String? = null, suffix: String? = null): String {
    val compilationNamePart = compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }
    val targetNamePart = target.disambiguationClassifier

    return lowerCamelCaseName(prefix, targetNamePart, compilationNamePart, suffix)
}

internal val KotlinCompilation<*>.defaultSourceSetName: String
    get() = lowerCamelCaseName(target.disambiguationClassifier, compilationName)

abstract class AbstractKotlinCompilation<T : KotlinCommonOptions>(
    target: KotlinTarget,
    override val compilationName: String
) : KotlinCompilation<T>, HasKotlinDependencies {

    override val kotlinOptions: T
        get() = compileKotlinTask.kotlinOptions

    override fun kotlinOptions(configure: T.() -> Unit) =
        configure(kotlinOptions)

    @Suppress("UNCHECKED_CAST")
    override val compileKotlinTask: KotlinCompile<T>
        get() = (target.project.tasks.getByName(compileKotlinTaskName) as KotlinCompile<T>)

    // Don't declare this property in the constructor to avoid NPE
    // when an overriding property of a subclass is accessed instead.
    override val target: KotlinTarget = target

    private val attributeContainer = HierarchyAttributeContainer(target.attributes)

    override fun getAttributes(): AttributeContainer = attributeContainer

    override val kotlinSourceSets: MutableSet<KotlinSourceSet> = mutableSetOf()

    override val defaultSourceSet: KotlinSourceSet
        get() = target.project.kotlinExtension.sourceSets.getByName(defaultSourceSetName)

    override fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit) =
        configure(defaultSourceSet)

    override val output: KotlinCompilationOutput by lazy {
        DefaultKotlinCompilationOutput(
            target.project,
            Callable { target.project.buildDir.resolve("processedResources/${target.targetName}/$name") })
    }

    open fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Boolean) {
        (target.project.tasks.getByName(compileKotlinTaskName) as AbstractKotlinCompile<*>).apply {
            source(sourceSet.kotlin)
            sourceFilesExtensions(sourceSet.customSourceFilesExtensions)
            if (addAsCommonSources) {
                commonSourceSet += sourceSet.kotlin
            }
        }
    }

    override fun source(sourceSet: KotlinSourceSet) {
        if (kotlinSourceSets.add(sourceSet)) {
            with(target.project) {
                whenEvaluated {
                    sourceSet.getSourceSetHierarchy().forEach { sourceSet ->
                        val isCommonSource =
                            CompilationSourceSetUtil.sourceSetsInMultipleCompilations(project)?.contains(sourceSet) ?: false

                        addSourcesToCompileTask(sourceSet, addAsCommonSources = isCommonSource)

                        // Use `forced = false` since `api`, `implementation`, and `compileOnly` may be missing in some cases like
                        // old Java & Android projects:
                        addExtendsFromRelation(apiConfigurationName, sourceSet.apiConfigurationName, forced = false)
                        addExtendsFromRelation(implementationConfigurationName, sourceSet.implementationConfigurationName, forced = false)
                        addExtendsFromRelation(compileOnlyConfigurationName, sourceSet.compileOnlyConfigurationName, forced = false)

                        if (this is KotlinCompilationToRunnableFiles<*>) {
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
            }
        }
    }

    override val compileDependencyConfigurationName: String
        get() = lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }.orEmpty(),
            "compileClasspath"
        )

    override val compileKotlinTaskName: String
        get() = lowerCamelCaseName(
            "compile",
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            "Kotlin",
            target.targetName
        )

    override val compileAllTaskName: String
        get() = lowerCamelCaseName(target.disambiguationClassifier, compilationName, "classes")

    override lateinit var compileDependencyFiles: FileCollection

    override val apiConfigurationName: String
        get() = disambiguateName("api")

    override val implementationConfigurationName: String
        get() = disambiguateName("implementation")

    override val compileOnlyConfigurationName: String
        get() = disambiguateName("compileOnly")

    override val runtimeOnlyConfigurationName: String
        get() = disambiguateName("runtimeOnly")

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
        DefaultKotlinDependencyHandler(this, target.project).run(configure)

    override fun dependencies(configureClosure: Closure<Any?>) =
        dependencies f@{ ConfigureUtil.configure(configureClosure, this@f) }

    override fun toString(): String = "compilation '$compilationName' ($target)"
}

val KotlinCompilation<*>.allKotlinSourceSets: Set<KotlinSourceSet>
    get() = kotlinSourceSets.flatMapTo(mutableSetOf()) { it.getSourceSetHierarchy() }

abstract class AbstractKotlinCompilationToRunnableFiles<T : KotlinCommonOptions>(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilation<T>(target, name), KotlinCompilationToRunnableFiles<T> {
    override val runtimeDependencyConfigurationName: String
        get() = lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
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

open class KotlinJvmCompilation(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilationToRunnableFiles<KotlinJvmOptions>(target, name), KotlinCompilationWithResources<KotlinJvmOptions> {
    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    override val compileKotlinTask: org.jetbrains.kotlin.gradle.tasks.KotlinCompile
        get() = super.compileKotlinTask as org.jetbrains.kotlin.gradle.tasks.KotlinCompile
}

class KotlinWithJavaCompilation<KotlinOptionsType : KotlinCommonOptions>(
    target: KotlinWithJavaTarget<KotlinOptionsType>,
    name: String
) : AbstractKotlinCompilationToRunnableFiles<KotlinOptionsType>(target, name), KotlinCompilationWithResources<KotlinOptionsType> {
    lateinit var javaSourceSet: SourceSet

    override val output: KotlinCompilationOutput by lazy { KotlinWithJavaCompilationOutput(this) }

    override val processResourcesTaskName: String
        get() = javaSourceSet.processResourcesTaskName

    override var runtimeDependencyFiles: FileCollection
        get() = javaSourceSet.runtimeClasspath
        set(value) {
            javaSourceSet.runtimeClasspath = value
        }

    override val runtimeDependencyConfigurationName: String
        get() = javaSourceSet.runtimeClasspathConfigurationName

    override val compileDependencyConfigurationName: String
        get() = javaSourceSet.compileClasspathConfigurationName

    override val runtimeOnlyConfigurationName: String
        get() = javaSourceSet.runtimeOnlyConfigurationName

    override val implementationConfigurationName: String
        get() = javaSourceSet.implementationConfigurationName

    override val apiConfigurationName: String
        get() = javaSourceSet.apiConfigurationName

    override val compileOnlyConfigurationName: String
        get() = javaSourceSet.compileOnlyConfigurationName

    override val compileAllTaskName: String
        get() = javaSourceSet.classesTaskName

    override var compileDependencyFiles: FileCollection
        get() = javaSourceSet.compileClasspath
        set(value) {
            javaSourceSet.compileClasspath = value
        }

    fun source(javaSourceSet: SourceSet) {
        with(target.project) {
            afterEvaluate {
                (tasks.getByName(compileKotlinTaskName) as AbstractKotlinCompile<*>).source(javaSourceSet.java)
            }
        }
    }
}

class KotlinJvmAndroidCompilation(
    target: KotlinAndroidTarget,
    name: String
) : AbstractKotlinCompilationToRunnableFiles<KotlinJvmOptions>(target, name) {
    override val compileKotlinTask: org.jetbrains.kotlin.gradle.tasks.KotlinCompile
        get() = super.compileKotlinTask as org.jetbrains.kotlin.gradle.tasks.KotlinCompile
}

class KotlinJsCompilation(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilationToRunnableFiles<KotlinJsOptions>(target, name), KotlinCompilationWithResources<KotlinJsOptions> {
    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    override val compileKotlinTask: Kotlin2JsCompile
        get() = super.compileKotlinTask as Kotlin2JsCompile
}

class KotlinCommonCompilation(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilation<KotlinMultiplatformCommonOptions>(target, name) {
    override val compileKotlinTask: KotlinCompileCommon
        get() = super.compileKotlinTask as KotlinCompileCommon

    // TODO once we properly compile metadata for each source set, the default source sets will likely become just the source sets
    // which are transformed to metadata
    private val commonSourceSetName = when (compilationName) {
        KotlinCompilation.MAIN_COMPILATION_NAME -> KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
        else -> error("Custom metadata compilations are not supported yet")
    }

    override val defaultSourceSet: KotlinSourceSet
        get() = target.project.kotlinExtension.sourceSets.getByName(commonSourceSetName)
}

class KotlinNativeCompilation(
    override val target: KotlinNativeTarget,
    name: String
) : AbstractKotlinCompilation<KotlinCommonOptions>(target, name), KotlinCompilationWithResources<KotlinCommonOptions> {

    override val kotlinOptions: KotlinCommonOptions
        get() = compileKotlinTask.kotlinOptions

    override val compileKotlinTask: KotlinNativeCompile
        get() = super.compileKotlinTask as KotlinNativeCompile

    private val project: Project
        get() = target.project

    // A collection containing all source sets used by this compilation
    // (taking into account dependencies between source sets). Used by both compilation
    // and linking tasks. Unlike kotlinSourceSets, includes dependency source sets.
    // TODO: Move into the compilation task when the linking task does klib linking instead of compilation.
    internal val allSources: MutableSet<SourceDirectorySet> = mutableSetOf()

    // TODO: Move into the compilation task when the linking task does klib linking instead of compilation.
    internal val commonSources: MutableSet<SourceDirectorySet> = mutableSetOf()

    var isTestCompilation = false

    var friendCompilationName: String? = null

    internal val friendCompilation: KotlinNativeCompilation?
        get() = friendCompilationName?.let {
            target.compilations.getByName(it)
        }

     // Used only to support the old APIs. TODO: Remove when the old APIs are removed.
    internal val binaries = mutableMapOf<Pair<NativeOutputKind, NativeBuildType>, NativeBinary>()

    // Native-specific DSL.
    var extraOpts = mutableListOf<String>()

    fun extraOpts(vararg values: Any) = extraOpts(values.toList())
    fun extraOpts(values: List<Any>) {
        extraOpts.addAll(values.map { it.toString() })
    }

    var buildTypes = mutableListOf<NativeBuildType>()
    var outputKinds = mutableListOf<NativeOutputKind>()

    fun outputKind(kind: NativeOutputKind) = outputKinds.add(kind)

    fun outputKinds(vararg kinds: NativeOutputKind) {
        outputKinds = kinds.toMutableList()
    }

    fun outputKinds(vararg kinds: String) {
        outputKinds = kinds.map { NativeOutputKind.valueOf(it.toUpperCase()) }.toMutableList()
    }

    fun outputKinds(kinds: List<Any>) {
        outputKinds = kinds.map {
            when (it) {
                is NativeOutputKind -> it
                is String -> NativeOutputKind.valueOf(it.toUpperCase())
                else -> error("Cannot use $it as an output kind")
            }
        }.toMutableList()
    }

    var entryPoint: String? = null
    fun entryPoint(value: String) {
        entryPoint = value
    }

    // Interop DSL.
    val cinterops = project.container(DefaultCInteropSettings::class.java) { cinteropName ->
        DefaultCInteropSettings(project, cinteropName, this)
    }

    var linkerOpts = mutableListOf<String>()

    fun cinterops(action: NamedDomainObjectContainer<DefaultCInteropSettings>.() -> Unit) = cinterops.action()
    fun cinterops(action: Closure<Unit>) = cinterops(ConfigureUtil.configureUsing(action))
    fun cinterops(action: Action<NamedDomainObjectContainer<DefaultCInteropSettings>>) = action.execute(cinterops)

    fun linkerOpts(vararg values: String) = linkerOpts(values.toList())
    fun linkerOpts(values: List<String>) {
        linkerOpts.addAll(values)
    }

    // Task accessors.
    fun findLinkTask(kind: NativeOutputKind, buildType: NativeBuildType): KotlinNativeLink? = binaries[kind to buildType]?.linkTask

    fun getLinkTask(kind: NativeOutputKind, buildType: NativeBuildType): KotlinNativeLink =
        findLinkTask(kind, buildType)
            ?: throw IllegalArgumentException("Cannot find a link task for the binary kind '$kind' and the build type '$buildType'")

    fun findLinkTask(kind: String, buildType: String) =
        findLinkTask(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))

    fun getLinkTask(kind: String, buildType: String) =
        getLinkTask(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))

    fun findBinary(kind: NativeOutputKind, buildType: NativeBuildType): File? = findLinkTask(kind, buildType)?.outputFile?.get()

    fun getBinary(kind: NativeOutputKind, buildType: NativeBuildType): File = getLinkTask(kind, buildType).outputFile.get()

    fun findBinary(kind: String, buildType: String) =
        findBinary(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))

    fun getBinary(kind: String, buildType: String) =
        getBinary(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))

    // Naming
    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    fun linkTaskName(kind: NativeOutputKind, buildType: NativeBuildType): String =
        lowerCamelCaseName(
            "link",
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }.orEmpty(),
            buildType.name.toLowerCase(),
            kind.taskNameClassifier,
            target.disambiguationClassifier
        )

    fun linkTaskName(kind: String, buildType: String) =
        linkTaskName(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))

    override val compileDependencyConfigurationName: String
        get() = lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }.orEmpty(),
            "compileKlibraries"
        )

    override val compileAllTaskName: String
        get() = lowerCamelCaseName(target.disambiguationClassifier, compilationName, "klibrary")

    val binariesTaskName: String
        get() = lowerCamelCaseName(target.disambiguationClassifier, compilationName, "binaries")

    override fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Boolean) {
        allSources.add(sourceSet.kotlin)
        if (addAsCommonSources) {
            commonSources.add(sourceSet.kotlin)
        }
    }
}

private object CompilationSourceSetUtil {
    // Cache the results per project
    private val projectSourceSetsInMultipleCompilationsCache = WeakHashMap<Project, Set<KotlinSourceSet>>()

    fun sourceSetsInMultipleCompilations(project: Project) =
        projectSourceSetsInMultipleCompilationsCache.computeIfAbsent(project) { _ ->
            check(project.state.executed) { "Should only be computed after the project is evaluated" }

            val compilations = (project.kotlinExtension as? KotlinMultiplatformExtension)?.targets?.flatMap { it.compilations }
                ?: return@computeIfAbsent null

            compilations
                .flatMap { compilation -> compilation.allKotlinSourceSets.map { sourceSet -> compilation to sourceSet } }
                .groupingBy { (_, sourceSet) -> sourceSet }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        }
}