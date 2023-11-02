/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.tasks

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.konan.*
import org.jetbrains.kotlin.gradle.plugin.konan.KonanInteropSpec.IncludeDirectoriesSpec
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.gradle.plugin.konan.dumpProperties
import org.jetbrains.kotlin.gradle.plugin.konan.konanDefaultDefFile
import org.jetbrains.kotlin.gradle.plugin.konan.konanExtension
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.run

/**
 * A task executing cinterop tool with the given args and compiling the stubs produced by this tool.
 */

open class KonanInteropTask @Inject constructor(@Internal val workerExecutor: WorkerExecutor) : KonanBuildingTask(), KonanInteropSpec {

    private val interopRunner = KonanCliInteropRunner(project, project.konanExtension.jvmArgs)

    @get:Internal
    override val toolRunner: KonanToolRunner = interopRunner

    override fun init(config: KonanBuildingConfig<*>, destinationDir: File, artifactName: String, target: KonanTarget) {
        super.init(config, destinationDir, artifactName, target)
        this.defFile = project.konanDefaultDefFile(artifactName)
    }

    // Output directories -----------------------------------------------------
    override val artifact: File
        @Internal get() = destinationDir.resolve(artifactFullName)

    val artifactFile: File?
        @Optional @OutputFile get() = if (!noPack) artifact else null

    val artifactDirectory: File?
        @Optional @OutputDirectory get() = if (noPack) artifact else null

    override val artifactSuffix: String
        @Internal get() = ".klib".takeUnless { noPack } ?: ""

    override val artifactPrefix: String
        @Internal get() = ""

    // Interop stub generator parameters -------------------------------------

    @Internal var enableParallel: Boolean = false

    @InputFile lateinit var defFile: File

    @Optional @Input var packageName: String? = null

    @Input val compilerOpts   = mutableListOf<String>()
    @Input val linkerOpts     = mutableListOf<String>()

    @Nested val includeDirs = IncludeDirectoriesSpecImpl()

    @InputFiles val headers   = mutableSetOf<FileCollection>()
    @InputFiles val linkFiles = mutableSetOf<FileCollection>()

    @Input
    var noPack: Boolean = false

    fun buildArgs() = mutableListOf<String>().apply {
        addArg("-o", artifact.canonicalPath)

        addArgIfNotNull("-target", konanTarget.visibleName)
        addArgIfNotNull("-def", defFile.canonicalPath)
        addArgIfNotNull("-pkg", packageName)

        addFileArgs("-header", headers)

        compilerOpts.forEach {
            addArg("-compiler-option", it)
        }

        val linkerOpts = mutableListOf<String>().apply { addAll(linkerOpts) }
        linkFiles.forEach {
            linkerOpts.addAll(it.files.map { it.canonicalPath })
        }
        linkerOpts.forEach {
            addArg("-linker-option", it)
        }

        addArgs("-compiler-option", includeDirs.allHeadersDirs.map { "-I${it.absolutePath}" })
        addArgs("-headerFilterAdditionalSearchPrefix", includeDirs.headerFilterDirs.map { it.absolutePath })

        addFileArgs("-library", libraries.klibFiles)
        addArgs("-library", libraries.artifacts.map { it.artifact.canonicalPath })

        addKey("-no-default-libs", noDefaultLibs)
        addKey("-no-endorsed-libs", noEndorsedLibs)

        addKey("-nopack", noPack)

        addAll(extraOpts)
    }

    // region DSL.

    inner class IncludeDirectoriesSpecImpl: IncludeDirectoriesSpec {
        @Input val allHeadersDirs = mutableSetOf<File>()
        @Input val headerFilterDirs = mutableSetOf<File>()

        override fun allHeaders(vararg includeDirs: Any) = allHeaders(includeDirs.toList())
        override fun allHeaders(includeDirs: Collection<Any>) {
            allHeadersDirs.addAll(includeDirs.map { project.file(it) })
        }

        override fun headerFilterOnly(vararg includeDirs: Any) = headerFilterOnly(includeDirs.toList())
        override fun headerFilterOnly(includeDirs: Collection<Any>) {
            headerFilterDirs.addAll(includeDirs.map { project.file(it) })
        }
    }

    override fun defFile(file: Any) {
        defFile = project.file(file)
    }

    override fun packageName(value: String) {
        packageName = value
    }

    override fun compilerOpts(vararg values: String) {
        compilerOpts.addAll(values)
    }

    override fun header(file: Any) = headers(file)
    override fun headers(vararg files: Any) {
        headers.add(project.files(files))
    }
    override fun headers(files: FileCollection) {
        headers.add(files)
    }

    override fun includeDirs(vararg values: Any) = includeDirs.allHeaders(values.toList())

    override fun includeDirs(closure: Closure<Unit>) = includeDirs { project.configure(this, closure) }
    override fun includeDirs(action: Action<IncludeDirectoriesSpec>) = includeDirs { action.execute(this) }
    override fun includeDirs(configure: IncludeDirectoriesSpec.() -> Unit) = includeDirs.configure()

    override fun linkerOpts(vararg values: String) = linkerOpts(values.toList())
    override fun linkerOpts(values: List<String>) {
        linkerOpts.addAll(values)
    }

    override fun link(vararg files: Any) {
        linkFiles.add(project.files(files))
    }

    override fun link(files: FileCollection) {
        linkFiles.add(files)
    }

    override fun noPack(flag: Boolean) {
        noPack = flag
    }

    // endregion

    internal interface RunToolParameters: WorkParameters {
        var taskName: String
        var args: List<String>
    }

    internal abstract class RunTool @Inject constructor() : WorkAction<RunToolParameters> {
        override fun execute() {
            val toolRunner = interchangeBox.remove(parameters.taskName) ?: error(":(")
            toolRunner.run(parameters.args)
        }
    }

    override fun run() {
        interopRunner.init(target)

        destinationDir.mkdirs()
        if (dumpParameters) {
            dumpProperties(this)
        }
        val args = buildArgs()
        if (enableParallel) {
            val workQueue = workerExecutor.noIsolation()
            interchangeBox[this.path] = toolRunner
            workQueue.submit(RunTool::class.java) {
                taskName = path
                this.args = args
            }
        } else {
            toolRunner.run(args)
        }
    }
}

internal val interchangeBox = ConcurrentHashMap<String, KonanToolRunner>()
