/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.binaryen

import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.newFileProperty
import org.jetbrains.kotlin.platform.wasm.BinaryenConfig
import javax.inject.Inject

@DisableCachingByDefault
abstract class BinaryenExec
@Inject
constructor() : AbstractExecTask<BinaryenExec>(BinaryenExec::class.java) {
    @get:Inject
    internal abstract val workerExecutor: WorkerExecutor

    @get:Inject
    internal abstract val fs: FileSystemOperations

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:NormalizeLineEndings
    @get:SkipWhenEmpty
    abstract val inputFiles: ConfigurableFileCollection

    @Internal
    @Deprecated("Use binaryenArguments instead. Scheduled for removal in Kotlin 2.5.")
    var binaryenArgs: MutableList<String> = BinaryenConfig.binaryenArgs.toMutableList()

    @Suppress("DEPRECATION")
    @get:Input
    val binaryenArguments: ListProperty<String> = project.objects.listProperty(String::class.java).value(
        inputFiles.elements.map {
            if (it.count() == 1) {
                binaryenArgs
            } else {
                BinaryenConfig.binaryenMultimoduleArgs
            }
        }
    )

    @Deprecated("Use inputFiles instead. Scheduled for removal in Kotlin 2.5.", replaceWith = ReplaceWith("inputFiles"))
    @get:Internal
    val inputFileProperty: RegularFileProperty = project.newFileProperty()

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @Deprecated("BinaryenExec can now work with multiple files, so outputFileName is not used anymore.  Scheduled for removal in Kotlin 2.5.")
    @get:Input
    @get:Optional
    abstract val outputFileName: Property<String>

    @Suppress("DEPRECATION")
    @Deprecated("Use outputDirectory instead. Scheduled for removal in Kotlin 2.5.", replaceWith = ReplaceWith("outputDirectory"))
    @Internal
    val outputFileProperty: Provider<RegularFile> = outputDirectory.zip(outputFileName) { dir: Directory, fileName: String ->
        dir.file(fileName)
    }

    override fun exec() {
        @Suppress("DEPRECATION")
        if (inputFileProperty.isPresent) {
            inputFiles.from(inputFileProperty)
        }

        val workQueue = workerExecutor.noIsolation()

        inputFiles.forEach { inputFile ->
            workQueue.submit(BinaryenWorkAction::class.java) {
                it.executable.set(this@BinaryenExec.executable)
                it.workingDir.set(inputFile.parentFile)
                it.args.set(binaryenArguments.get())
                it.inputFile.set(inputFile)
                it.outputFile.set(outputDirectory.file(inputFile.name).getFile())
            }
        }
    }

    companion object {
        @ExperimentalWasmDsl
        fun register(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: BinaryenExec.() -> Unit = {},
        ): TaskProvider<BinaryenExec> {
            val target = compilation.target
            val project = target.project
            val binaryen = BinaryenPlugin.apply(project)
            return project.registerTask(
                name,
            ) {
                it.executable = binaryen.requireConfigured().executable
                it.dependsOn(binaryen.setupTaskProvider)
                it.dependsOn(compilation.compileTaskProvider)
                it.configuration()
            }
        }

        @ExperimentalWasmDsl
        @Deprecated(
            "Use register instead",
            ReplaceWith("register(compilation, name, configuration)")
        )
        fun create(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: BinaryenExec.() -> Unit = {},
        ): TaskProvider<BinaryenExec> = register(compilation, name, configuration)
    }
}