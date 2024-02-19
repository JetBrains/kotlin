/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.newFileProperty
import org.jetbrains.kotlin.platform.wasm.BinaryenConfig
import javax.inject.Inject

@DisableCachingByDefault
abstract class BinaryenExec
@Inject
constructor() : AbstractExecTask<BinaryenExec>(BinaryenExec::class.java) {
    @get:Inject
    abstract val fs: FileSystemOperations

    init {
        onlyIf {
            !inputFileProperty.isPresent || inputFileProperty.asFile.map { it.exists() }.get()
        }
    }

    @Input
    var binaryenArgs: MutableList<String> = BinaryenConfig.binaryenArgs.toMutableList()

    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFile
    @NormalizeLineEndings
    val inputFileProperty: RegularFileProperty = project.newFileProperty()

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val outputFileName: Property<String>

    @Internal
    val outputFileProperty: Provider<RegularFile> = project.provider {
        outputDirectory.file(outputFileName).get()
    }

    override fun exec() {
        val inputFile = inputFileProperty.asFile.get()
        val newArgs = mutableListOf<String>()
        newArgs.addAll(binaryenArgs)
        newArgs.add(inputFile.canonicalPath)
        newArgs.add("-o")
        newArgs.add(outputDirectory.file(outputFileName).get().asFile.normalize().absolutePath)
        workingDir = inputFile.parentFile
        this.args = newArgs
        super.exec()
    }

    companion object {
        fun create(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: BinaryenExec.() -> Unit = {},
        ): TaskProvider<BinaryenExec> {
            val target = compilation.target
            val project = target.project
            val binaryen = BinaryenRootPlugin.apply(project.rootProject)
            return project.registerTask(
                name,
            ) {
                it.executable = binaryen.requireConfigured().executable
                it.dependsOn(binaryen.setupTaskProvider)
                it.dependsOn(compilation.compileTaskProvider)
                it.configuration()
            }
        }
    }
}