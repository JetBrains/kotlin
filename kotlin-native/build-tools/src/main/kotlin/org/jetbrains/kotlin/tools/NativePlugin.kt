/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.cpp.CompilationDatabaseExtension
import org.jetbrains.kotlin.cpp.CompilationDatabasePlugin
import org.jetbrains.kotlin.dependencies.NativeDependenciesExtension
import org.jetbrains.kotlin.dependencies.NativeDependenciesPlugin
import org.jetbrains.kotlin.konan.target.HostManager.Companion.hostIsMac
import org.jetbrains.kotlin.konan.target.HostManager.Companion.hostIsMingw
import org.jetbrains.kotlin.utils.reproduciblySortedFilePaths
import java.io.File
import javax.inject.Inject
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.addAll
import kotlin.collections.drop
import kotlin.collections.first
import kotlin.collections.flatMap
import kotlin.collections.forEach
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.collections.toTypedArray

open class NativePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.apply<BasePlugin>()
        project.apply<NativeDependenciesPlugin>()
        project.apply<CompilationDatabasePlugin>()
        project.extensions.create("native", NativeToolsExtension::class.java, project)
    }
}

abstract class ToolExecutionTask @Inject constructor(private val execOperations: ExecOperations): DefaultTask() {
    @get:OutputFile
    abstract var output: File

    @get:InputFiles
    abstract var input: List<File>

    @get:Input
    abstract var cmd: String

    @get:Input
    abstract var args: List<String>

    @TaskAction
    fun action() {
        if (output.exists()) output.delete()
        execOperations.exec {
            executable(cmd)
            args(*this@ToolExecutionTask.args.toTypedArray())
        }
    }
}

class ToolPatternImpl(val extension: NativeToolsExtension, val output:String, vararg val input: String):ToolPattern {
    val tool = mutableListOf<String>()
    val args = mutableListOf<String>()
    override fun ruleOut(): String = output
    override fun ruleInFirst(): String = input.first()
    override fun ruleInAll(): Array<String> = arrayOf(*input)

    override fun flags(vararg args: String) {
        this.args.addAll(args)
    }

    override fun tool(vararg arg: String) {
        tool.addAll(arg)
    }

    override fun env(name: String) = emptyArray<String>()

    fun registerCompilationDatabaseEntry() {
        extension.compilationDatabaseTarget.entry {
            this.directory.set(extension.project.layout.projectDirectory)
            this.files.from(*input)
            this.arguments.addAll(tool)
            this.arguments.addAll(args)
            this.output.set(this@ToolPatternImpl.output)
        }
    }

    fun configure(task: ToolExecutionTask, configureDepencies:Boolean) {
        extension.cleanupFiles += output
        task.input = input.map {
            extension.project.file(it)
        }
        val nativeDependenciesExtension = extension.project.extensions.getByType<NativeDependenciesExtension>()
        task.dependsOn(nativeDependenciesExtension.hostPlatformDependency)
        task.dependsOn(nativeDependenciesExtension.llvmDependency)
        if (configureDepencies)
            task.input.forEach { task.dependsOn(it.name) }
        val file = extension.project.file(output)
        file.parentFile.mkdirs()
        task.output = file
        task.cmd = tool.first()
        task.args = listOf(*tool.drop(1).toTypedArray(), *args.toTypedArray())
    }
}

open class SourceSet(
    val sourceSets: SourceSets,
    val name: String,
    val initialDirectory: File = sourceSets.project.projectDir,
    val initialSourceSet: SourceSet? = null,
    val rule: Pair<String, String>? = null
) {
    val collection = sourceSets.project.objects.fileCollection()

    fun file(path: String) {
        collection.from(sourceSets.project.files("${initialDirectory.absolutePath}/$path"))
    }

    fun dir(path: String) {
        collection.from(sourceSets.project.fileTree("${initialDirectory.absolutePath}/$path").files)
    }

    fun transform(suffixes: Pair<String, String>): SourceSet {
        return SourceSet(
            sourceSets,
            name,
            sourceSets.project.file(sourceSets.project.layout.buildDirectory.dir("$name/${suffixes.first}_${suffixes.second}/")),
            this,
            suffixes
        ).apply {
            resolvePatterns().forEach {
                it.first.registerCompilationDatabaseEntry()
            }
        }
    }

    private fun resolvePatterns(): List<Pair<ToolPatternImpl, String>> {
        rule ?: return emptyList()
        return initialSourceSet!!.collection
            .filter { !it.isDirectory() }
            .filter { it.name.endsWith(rule.first) }
            .map { it.relativeTo(initialSourceSet.initialDirectory) }
            .map { it.path }
            .map { it to (it.substring(0, it.lastIndexOf(rule.first)) + rule.second) }
            .map {
                file(it.second)
                sourceSets.project.file("${initialSourceSet.initialDirectory.path}/${it.first}") to sourceSets.project.file("${initialDirectory.path}/${it.second}")
            }.map {
                val toolConfiguration = ToolPatternImpl(sourceSets.extension, it.second.path, it.first.path)
                sourceSets.extension.toolPatterns[rule]!!.invoke(toolConfiguration)
                toolConfiguration to it.second.name
            }
    }

    fun implicitTasks(): Array<TaskProvider<*>> {
        initialSourceSet?.implicitTasks()
        return resolvePatterns().map {
            sourceSets.project.tasks.register<ToolExecutionTask>(it.second, ToolExecutionTask::class.java) {
                val toolConfiguration = it.first
                toolConfiguration.configure(this, initialSourceSet!!.rule != null)
                dependsOn(initialSourceSet.collection)
            }
        }.toTypedArray()
    }
}

class SourceSets(val project: Project, val extension: NativeToolsExtension, val sources: MutableMap<String, SourceSet>) :
    MutableMap<String, SourceSet> by sources {
    operator fun String.invoke(initialDirectory: File = project.projectDir, configuration: SourceSet.() -> Unit) {
        sources[this] = SourceSet(this@SourceSets, this, initialDirectory).also {
            configuration(it)
        }
    }
}


interface Environment {
    operator fun String.invoke(vararg values: String)
}

interface ToolPattern {
    fun ruleOut(): String
    fun ruleInFirst(): String
    fun ruleInAll(): Array<String>
    fun flags(vararg args: String): Unit
    fun tool(vararg arg: String): Unit
    fun env(name: String): Array<String>
}


typealias ToolPatternConfiguration = ToolPattern.() -> Unit
typealias EnvironmentConfiguration = Environment.() -> Unit

class ToolConfigurationPatterns(
    val extension: NativeToolsExtension,
    val patterns: MutableMap<Pair<String, String>, ToolPatternConfiguration>
) : MutableMap<Pair<String, String>, ToolPatternConfiguration> by patterns {
    operator fun Pair<String, String>.invoke(configuration: ToolPatternConfiguration) {
        patterns[this] = configuration
    }
}


open class NativeToolsExtension(val project: Project) {
    private val nativeDependenciesExtension = project.extensions.getByType<NativeDependenciesExtension>()
    internal val compilationDatabaseTarget =
        project.extensions.getByType<CompilationDatabaseExtension>().hostTarget {}

    val llvmDir by nativeDependenciesExtension::llvmPath
    val hostPlatform by nativeDependenciesExtension::hostPlatform

    private val jdkDir: File
        get() = project.extensions.getByType(JavaToolchainService::class.java).launcherFor {
            languageVersion.set(JavaLanguageVersion.of(11))
        }.get().metadata.installationPath.asFile

    val jniIncludeFlags: Array<String>
        get() = arrayOf(
                "-I${jdkDir.absolutePath}/include",
                "-I${jdkDir.absolutePath}/include/${org.jetbrains.kotlin.konan.target.HostManager.jniHostPlatformIncludeDir}"
        )

    val jniHostCompilerArgs: Array<String>
        get() {
            val fromPlatform = hostPlatform.clangForJni.hostCompilerArgsForJni
            val firstPath = fromPlatform.firstOrNull { it.startsWith("-I") }?.removePrefix("-I")
            if (firstPath != null && File(firstPath).resolve("jni.h").exists()) {
                return fromPlatform
            }
            return jniIncludeFlags
        }

    private val reproducibilityRootsMap: Map<File, String>
        get() = mapOf(
                // This applies for both sources of the current project, and dependencies on other
                // projects inside the repo.
                project.isolated.rootProject.let {
                    it.projectDirectory.asFile to it.name
                },
                // This is the common root for native dependencies: sysroots, llvm, ...
                nativeDependenciesExtension.nativeDependenciesRoot to "NATIVE_DEPS",
                // Not every user of `NativePlugin` uses JNI, but there's no harm to keep it for all.
                jdkDir to "JDK",
        )

    /**
     * Use these flags for `clang` invocations, so that the generated binaries do not contain
     * absolute paths.
     */
    val reproducibilityCompilerFlags: Array<String>
        get() = reproducibilityRootsMap.map {
            "-ffile-prefix-map=${it.key}=${it.value}"
        }.toTypedArray()

    /**
     * Whenever a `FileCollection` is passed as arguments, it's order must be stable sorted for reproducibility.
     */
    fun reproduciblySortedFilePaths(fileCollection: FileCollection): List<File> =
            fileCollection.reproduciblySortedFilePaths(reproducibilityRootsMap)

    val sourceSets = SourceSets(project, this, mutableMapOf<String, SourceSet>())
    val toolPatterns = ToolConfigurationPatterns(this, mutableMapOf<Pair<String, String>, ToolPatternConfiguration>())
    val cleanupFiles = mutableListOf<String>()
    fun sourceSet(configuration: SourceSets.() -> Unit) {
        sourceSets.configuration()
    }

    var environmentConfiguration: EnvironmentConfiguration? = null
    fun environment(configuration: EnvironmentConfiguration) {
        environmentConfiguration = configuration
    }

    fun suffixes(configuration: ToolConfigurationPatterns.() -> Unit) = toolPatterns.configuration()

    fun target(name: String, vararg objSet: SourceSet, configuration: ToolPatternConfiguration) {
        project.tasks.named(LifecycleBasePlugin.CLEAN_TASK_NAME, Delete::class.java).configure {
            delete(*this@NativeToolsExtension.cleanupFiles.toTypedArray())
        }

        sourceSets.project.tasks.create(name, ToolExecutionTask::class.java) {
            objSet.forEach {
                dependsOn(it.implicitTasks())
            }
            val deps = objSet.flatMap { reproduciblySortedFilePaths(it.collection) }.map { it.path }
            val toolConfiguration = ToolPatternImpl(sourceSets.extension, "${project.layout.buildDirectory.get().asFile.path}/$name", *deps.toTypedArray())
            toolConfiguration.configuration()
            toolConfiguration.configure(this, false )
        }
    }
}


fun solib(name: String) = when {
    hostIsMingw -> "$name.dll"
    hostIsMac -> "lib$name.dylib"
    else -> "lib$name.so"
}

fun lib(name:String) = when {
    hostIsMingw -> "$name.lib"
    else -> "lib$name.a"
}

fun libname(file: File): String = when {
    hostIsMingw -> file.nameWithoutExtension
    else -> file.nameWithoutExtension.removePrefix("lib")
}

fun obj(name: String) = when {
    hostIsMingw -> "$name.obj"
    else -> "$name.o"
}
