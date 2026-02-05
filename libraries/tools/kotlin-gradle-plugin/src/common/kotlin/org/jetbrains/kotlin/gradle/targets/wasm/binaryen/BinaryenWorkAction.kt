/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.binaryen

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.gradle.utils.getFile
import javax.inject.Inject

internal abstract class BinaryenWorkAction : WorkAction<BinaryenWorkAction.BinaryenWorkParameters> {
    internal interface BinaryenWorkParameters : WorkParameters {
        val executable: Property<String>
        val workingDir: DirectoryProperty
        val args: ListProperty<String>
        val inputFile: RegularFileProperty
        val outputFile: RegularFileProperty
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun execute() {
        execOperations.exec {
            it.executable = parameters.executable.get()
            it.workingDir = parameters.workingDir.getFile()
            it.args = parameters.args.get() +
                    parameters.inputFile.getFile().absolutePath +
                    "-o" +
                    parameters.outputFile.getFile().absolutePath
        }
    }
}