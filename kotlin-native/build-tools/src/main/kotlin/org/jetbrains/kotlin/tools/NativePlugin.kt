/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.dependencies.NativeDependenciesExtension
import org.jetbrains.kotlin.dependencies.NativeDependenciesPlugin
import org.jetbrains.kotlin.konan.target.HostManager.Companion.hostIsMac
import org.jetbrains.kotlin.konan.target.HostManager.Companion.hostIsMingw
import java.io.File
import javax.inject.Inject
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
        project.extensions.create("native", NativeToolsExtension::class.java, project)
    }
}

private const val RULE_OUT_PLACEHOLDER = "@@OUTPUT@@"
private const val RULE_IN_PLACEHOLDER = "@@INPUT@@"

sealed interface ToolArg {
    fun render(): List<String>

    class Plain(@get:Input val value: String) : ToolArg {
        override fun render() = listOf(value)
    }

    class RuleOut(@get:OutputFile val file: Provider<RegularFile>) : ToolArg {
        override fun render() = listOf(file.get().asFile.absolutePath)
    }

    class RuleIn(
            @get:InputFiles
            @get:PathSensitive(PathSensitivity.NONE) // computed manually relative to workingDir
            val files: FileCollection,
            @get:Internal("used to compute relative input file")
            val workingDir: Provider<Directory>
    ) : ToolArg {
        @get:Input
        val inputRelativePaths = files.elements.zip(workingDir) { files, base ->
            files.map {
                it.asFile.toRelativeString(base.asFile)
            }
        }

        override fun render() = inputRelativePaths.get()
    }
}

open class ToolExecutionTask @Inject constructor(
        private val execOperations: ExecOperations,
        objectFactory: ObjectFactory,
) : DefaultTask() {
    @get:Internal("handled by processedArgs")
    val output: RegularFileProperty = objectFactory.fileProperty()

    @get:Internal("handled by processedArgs")
    val input: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Input
    val cmd: Property<String> = objectFactory.property(String::class)

    @get:Internal("handled by processedArgs")
    val args: ListProperty<String> = objectFactory.listProperty(String::class)

    @get:Nested
    protected val processedArgs: Provider<List<ToolArg>> = args.map {
        it.map { arg ->
            when (arg) {
                RULE_OUT_PLACEHOLDER -> ToolArg.RuleOut(output)
                RULE_IN_PLACEHOLDER -> ToolArg.RuleIn(input, workingDir)
                else -> ToolArg.Plain(arg)
            }
        }
    }

    @get:Internal("used to compute relative input files")
    val workingDir: DirectoryProperty = objectFactory.directoryProperty()

    @TaskAction
    fun action() {
        output.asFile.get().delete()
        execOperations.exec {
            executable(cmd.get())
            args = processedArgs.get().flatMap { it.render() }
            workingDir = this@ToolExecutionTask.workingDir.get().asFile
        }
    }
}

class ToolPatternImpl(val extension: NativeToolsExtension, val output:String, vararg val input: String):ToolPattern {
    val tool = mutableListOf<String>()
    val args = mutableListOf<String>()
    override fun ruleOut(): String = RULE_OUT_PLACEHOLDER
    override fun ruleInFirst(): String = RULE_IN_PLACEHOLDER
    override fun ruleInAll(): Array<String> = arrayOf(RULE_IN_PLACEHOLDER)

    override fun flags(vararg args: String) {
        this.args.addAll(args)
    }

    override fun tool(vararg arg: String) {
        tool.addAll(arg)
    }

    override fun env(name: String) = emptyArray<String>()

    fun configure(task: ToolExecutionTask, configureDepencies:Boolean) {
        extension.cleanupFiles += output
        task.input.from(*input)
        val nativeDependenciesExtension = extension.project.extensions.getByType<NativeDependenciesExtension>()
        task.dependsOn(nativeDependenciesExtension.hostPlatformDependency)
        task.dependsOn(nativeDependenciesExtension.llvmDependency)
        if (configureDepencies)
            task.input.forEach { task.dependsOn(it.name) }
        task.output = extension.project.file(output)
        task.cmd = tool.first()
        task.args = listOf(*tool.drop(1).toTypedArray(), *args.toTypedArray())
        task.workingDir = extension.workingDir
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
        )
    }

    fun implicitTasks(): Array<TaskProvider<*>> {
        rule ?: return emptyArray()
        initialSourceSet?.implicitTasks()
        val collection = initialSourceSet!!.collection
        return collection
            .filter { !it.isDirectory() }
            .filter { it.name.endsWith(rule.first) }
            .map { it.relativeTo(initialSourceSet.initialDirectory) }
            .map { it.path }
            .map { it to (it.substring(0, it.lastIndexOf(rule.first)) + rule.second) }
            .map {
                file(it.second)
                sourceSets.project.file("${initialSourceSet.initialDirectory.path}/${it.first}") to sourceSets.project.file("${initialDirectory.path}/${it.second}")
            }.map {
                sourceSets.project.tasks.register<ToolExecutionTask>(it.second.name, ToolExecutionTask::class.java) {
                    val toolConfiguration = ToolPatternImpl(sourceSets.extension, it.second.path, it.first.path)
                    sourceSets.extension.toolPatterns[rule]!!.invoke(toolConfiguration)
                    toolConfiguration.configure(this, initialSourceSet.rule != null)
                    dependsOn(collection)
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

    val llvmDir by nativeDependenciesExtension::llvmPath
    val hostPlatform by nativeDependenciesExtension::hostPlatform

    val workingDir: DirectoryProperty = project.objects.directoryProperty().convention(project.layout.projectDirectory)

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
            val deps = objSet.flatMap { it.collection.files }.map { it.path }
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