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
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.sources.defaultSourceSetLanguageSettingsChecker
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*

internal fun KotlinCompilation.composeName(prefix: String? = null, suffix: String? = null): String {
    val compilationNamePart = compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }
    val targetNamePart = target.disambiguationClassifier

    return lowerCamelCaseName(prefix, targetNamePart, compilationNamePart, suffix)
}

internal val KotlinCompilation.defaultSourceSetName: String
    get() = lowerCamelCaseName(target.disambiguationClassifier, compilationName)

internal class DefaultKotlinDependencyHandler(
    val parent: HasKotlinDependencies,
    val project: Project
) : KotlinDependencyHandler {
    override fun api(dependencyNotation: Any) = addDependency(parent.apiConfigurationName, dependencyNotation)

    override fun implementation(dependencyNotation: Any) = addDependency(parent.implementationConfigurationName, dependencyNotation)

    override fun compileOnly(dependencyNotation: Any) = addDependency(parent.compileOnlyConfigurationName, dependencyNotation)

    override fun runtimeOnly(dependencyNotation: Any) = addDependency(parent.runtimeOnlyConfigurationName, dependencyNotation)

    private fun addDependency(configurationName: String, dependencyNotation: Any) {
        project.dependencies.add(configurationName, dependencyNotation)
    }
}

abstract class AbstractKotlinCompilation(
    target: KotlinTarget,
    override val compilationName: String
) : KotlinCompilation, HasKotlinDependencies {

    // Don't declare this property in the constructor to avoid NPE
    // when an overriding property of a subclass is accessed instead.
    override val target: KotlinTarget = target

    private val attributeContainer = HierarchyAttributeContainer(target.attributes)

    override fun getAttributes(): AttributeContainer = attributeContainer

    override val kotlinSourceSets: MutableSet<KotlinSourceSet> = mutableSetOf()

    open fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Boolean) {
        val compileTask = target.project.tasks.getByName(compileKotlinTaskName) as AbstractKotlinCompile<*>
        compileTask.source(sourceSet.kotlin)
        if (addAsCommonSources) {
            compileTask.commonSourceSet += sourceSet.kotlin
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

                        if (this is KotlinCompilationToRunnableFiles) {
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

val KotlinCompilation.allKotlinSourceSets: Set<KotlinSourceSet>
    get() = kotlinSourceSets.flatMapTo(mutableSetOf()) { it.getSourceSetHierarchy() }

abstract class AbstractKotlinCompilationToRunnableFiles(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilation(target, name), KotlinCompilationToRunnableFiles {
    override val runtimeDependencyConfigurationName: String
        get() = lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            "runtimeClasspath"
        )

    override lateinit var runtimeDependencyFiles: FileCollection
}

internal fun KotlinCompilation.disambiguateName(simpleName: String): String {
    return lowerCamelCaseName(
        target.disambiguationClassifier,
        compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
        simpleName
    )
}

open class KotlinJvmCompilation(
    target: KotlinTarget,
    name: String,
    override val output: SourceSetOutput
) : AbstractKotlinCompilationToRunnableFiles(target, name), KotlinCompilationWithResources {
    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")
}

class KotlinWithJavaCompilation(
    target: KotlinWithJavaTarget,
    name: String
) : AbstractKotlinCompilationToRunnableFiles(target, name), KotlinCompilationWithResources {
    lateinit var javaSourceSet: SourceSet

    override val output: SourceSetOutput
        get() = javaSourceSet.output

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
    name: String,
    override val output: SourceSetOutput
) : AbstractKotlinCompilationToRunnableFiles(target, name)

class KotlinJsCompilation(
    target: KotlinTarget,
    name: String,
    override val output: SourceSetOutput
) : AbstractKotlinCompilationToRunnableFiles(target, name)

class KotlinCommonCompilation(
    target: KotlinTarget,
    name: String,
    override val output: SourceSetOutput
) : AbstractKotlinCompilation(target, name)

class KotlinNativeCompilation(
    override val target: KotlinNativeTarget,
    name: String,
    override val output: SourceSetOutput
) : AbstractKotlinCompilation(target, name) {

    private val project: Project
        get() = target.project

    // A FileCollection containing source files from all source sets used by this compilation
    // (taking into account dependencies between source sets). Used by both compilation
    // and linking tasks.
    // TODO: Move into the compilation task when the linking task does klib linking instead of compilation.
    internal var allSources: FileCollection = target.project.files()

    var isTestCompilation = false

    var friendCompilationName: String? = null

    internal val friendCompilation: KotlinNativeCompilation?
        get() = friendCompilationName?.let {
            target.compilations.getByName(it)
        }

    internal val binaryTasks = mutableMapOf<Pair<NativeOutputKind, NativeBuildType>, KotlinNativeCompile>()

    // Native-specific DSL.
    val extraOpts = mutableListOf<String>()

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
    fun entryPoint(value: String) { entryPoint = value }

    // Interop DSL.
    val cinterops = project.container(DefaultCInteropSettings::class.java) { cinteropName ->
        DefaultCInteropSettings(project, cinteropName,this)
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

    fun findLinkTask(kind: NativeOutputKind, buildType: NativeBuildType): KotlinNativeCompile? = binaryTasks[kind to buildType]

    fun getLinkTask(kind: NativeOutputKind, buildType: NativeBuildType): KotlinNativeCompile =
        findLinkTask(kind, buildType) ?:
        throw IllegalArgumentException("Cannot find a link task for the binary kind '$kind' and the build type '$buildType'")

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

    val linkAllTaskName: String
        get() = lowerCamelCaseName(
            "link",
            compilationName.takeIf { it != "main" }.orEmpty(),
            target.disambiguationClassifier
        )

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
        get() = lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }.orEmpty(),
            "klibrary"
        )

    override fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Boolean) {
        // TODO: support optional expectations
        allSources += sourceSet.kotlin
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