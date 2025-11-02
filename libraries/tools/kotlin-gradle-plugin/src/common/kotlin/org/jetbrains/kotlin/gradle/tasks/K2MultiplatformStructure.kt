/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.work.Incremental
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSideEffect
import java.io.File

@InternalKotlinGradlePluginApi
abstract class K2MultiplatformStructure {

    @InternalKotlinGradlePluginApi
    data class RefinesEdge(
        @Input
        val fromFragmentName: String,
        @Input
        val toFragmentName: String,
    )

    @InternalKotlinGradlePluginApi
    data class Fragment(
        @Input
        val fragmentName: String,

        @get:InputFiles
        @get:IgnoreEmptyDirectories
        @get:Incremental
        @get:NormalizeLineEndings
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val sources: FileCollection,

        /**
         * Populated only if the separate KMP compilation is enabled
         * @see org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.separateKmpCompilation
         */
        @get:Classpath
        @get:IgnoreEmptyDirectories
        @get:Incremental
        val dependencies: FileCollection,

        @get:Classpath
        @get:IgnoreEmptyDirectories
        @get:Incremental
        val friends: FileCollection,
    )

    @get:Nested
    abstract val refinesEdges: SetProperty<RefinesEdge>

    @get:Nested
    abstract val fragments: ListProperty<Fragment>

    /**
     * If new sources were added to the Compile Task,
     * and they weren't mapped to any of the fragments then [defaultFragmentName] will be used
     *
     * It is marked with @Optional as an extra protection measure for cases when some task extends
     * a compile task but doesn't need K2 Structure for example [KotlinJsIrLink]
     *
     * @see KotlinCompileTool.source
     */
    @get:Input
    @get:Optional
    abstract val defaultFragmentName: Property<String>
}

internal val K2MultiplatformStructure.fragmentsCompilerArgs: Array<String>
    get() = fragments.get().map { it.fragmentName }.toSet().toTypedArray()

private fun fragmentSourceCompilerArg(sourceFile: File, fragmentName: String) = "$fragmentName:${sourceFile.absolutePath}"

internal fun K2MultiplatformStructure.fragmentSourcesCompilerArgs(
    allSources: Collection<File>,
    sourceFileFilter: PatternFilterable? = null
): Array<String> {
    val sourcesWithKnownFragment = mutableSetOf<File>()
    val fragmentSourcesCompilerArgs = fragments.get().flatMap { sourceSet ->
        sourceSet.sources
            .run { if (sourceFileFilter != null) asFileTree.matching(sourceFileFilter) else this }
            .files.map { sourceFile ->
                sourcesWithKnownFragment.add(sourceFile)
                fragmentSourceCompilerArg(sourceFile, sourceSet.fragmentName)
            }
    }.toMutableList()

    val sourcesWithUnknownFragment = allSources - sourcesWithKnownFragment
    val defaultFragmentName = defaultFragmentName.orNull
    if (defaultFragmentName != null) {
        sourcesWithUnknownFragment.mapTo(fragmentSourcesCompilerArgs) { fragmentSourceCompilerArg(it, defaultFragmentName) }
    }

    return fragmentSourcesCompilerArgs.toTypedArray()
}

internal val K2MultiplatformStructure.fragmentDependenciesCompilerArgs: Array<String>
    get() = fragments.get().flatMap { fragment ->
        fragment.dependencies.files.map { dependencyFile ->
            "${fragment.fragmentName}:${dependencyFile.absolutePath}"
        }
    }.toTypedArray()

internal val K2MultiplatformStructure.fragmentFriendsCompilerArgs: Array<String>
    get() = fragments.get().flatMap { fragment ->
        fragment.friends.files.map { dependencyFile ->
            "${fragment.fragmentName}:${dependencyFile.absolutePath}"
        }
    }.toTypedArray()

internal val K2MultiplatformStructure.fragmentRefinesCompilerArgs: Array<String>
    get() = refinesEdges.get().map { edge ->
        "${edge.fromFragmentName}:${edge.toFragmentName}"
    }.toTypedArray()

//internal abstract class K2MultiplatformStructureDumpTask : DefaultTask() {
//    @get:Nested
//    abstract val structure: Property<K2MultiplatformStructure>
//
//    @get:OutputFile
//    abstract val output: RegularFileProperty
//
//    @TaskAction
//    fun action() {
//        val structure = structure.get()
//        val output = output.get().asFile
//        output.writeText(structure.toJson())
//    }
//}

internal val ConfigureK2MultiplatformStructureDumpTasks = KotlinCompilationSideEffect { compilation ->
    val project = compilation.project
    println("Compilation ${compilation.name} of target ${compilation.target.name} of project ${project.path}")
    println("Has task name: ${compilation.compileTaskProvider.name}")
    compilation.compileTaskProvider.configure { task ->
        println("Configure Task: ${task.name} of project ${project.path}")
        if (task !is K2MultiplatformCompilationTask) return@configure

        val k2StructureOutput = project.layout.buildDirectory.file("kgp-debug-data/compilations/${task.name}/k2-structure.json")
        val classpathOutput = project.layout.buildDirectory.file("kgp-debug-data/compilations/${task.name}/classpath.txt")
        val friendsOutput = project.layout.buildDirectory.file("kgp-debug-data/compilations/${task.name}/friends.txt")

//        task.outputs.file(k2StructureOutput)
//        task.outputs.file(classpathOutput)
//        task.outputs.file(friendsOutput)

        task.doFirst {
            if (task is KotlinCompileTool) {
                classpathOutput.get().asFile.run {
                    println("Writing libraries classpath to $absolutePath")
                    parentFile.mkdirs()
                    writeText(task.libraries.files.joinToString("\n"))
                }
            }

            if (task is BaseKotlinCompile) {
                friendsOutput.get().asFile.run {
                    println("Writing friend modules to $absolutePath")
                    parentFile.mkdirs()
                    writeText(task.friendPaths.files.joinToString("\n"))
                }
            }

            k2StructureOutput.get().asFile.run {
                println("Writing K2MultiplatformStructure to $absolutePath")
                parentFile.mkdirs()
                writeText(task.multiplatformStructure.toJson())
            }
        }
    }
}

private fun K2MultiplatformStructure.toJson(): String {
    val root = JsonObject()

    // Materialize defaultFragmentName if present
    this.defaultFragmentName.orNull?.let { root.addProperty("defaultFragmentName", it) }

    // Materialize fragments with sources and dependencies as lists of files
    val fragmentsArray = com.google.gson.JsonArray()
    for (fragment in this.fragments.get()) {
        val fragmentObj = JsonObject()
        fragmentObj.addProperty("fragmentName", fragment.fragmentName)

        val sourcesArray = com.google.gson.JsonArray()
        for (file in fragment.sources.files) {
            sourcesArray.add(file.absolutePath)
        }
        fragmentObj.add("sources", sourcesArray)

        val depsArray = com.google.gson.JsonArray()
        for (file in fragment.dependencies.files) {
            depsArray.add(file.absolutePath)
        }
        fragmentObj.add("dependencies", depsArray)

        val friendsArray = com.google.gson.JsonArray()
        for (file in fragment.friends.files) {
            friendsArray.add(file.absolutePath)
        }
        fragmentObj.add("friends", friendsArray)

        fragmentsArray.add(fragmentObj)
    }
    root.add("fragments", fragmentsArray)

    // Materialize refines edges
    val edgesArray = com.google.gson.JsonArray()
    for (edge in this.refinesEdges.get()) {
        val edgeObj = JsonObject()
        edgeObj.addProperty("fromFragmentName", edge.fromFragmentName)
        edgeObj.addProperty("toFragmentName", edge.toFragmentName)
        edgesArray.add(edgeObj)
    }
    root.add("refinesEdges", edgesArray)

    return GsonBuilder().setPrettyPrinting().create().toJson(root)
}