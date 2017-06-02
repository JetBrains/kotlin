/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.common.arguments.K2JSDceArguments
import org.jetbrains.kotlin.cli.js.dce.K2JSDce
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptionsImpl
import java.io.File

open class KotlinJsDce : AbstractKotlinCompileTool<K2JSDceArguments>(), KotlinJsDce {
    private val dceOptionsImpl = KotlinJsDceOptionsImpl()

    override val dceOptions: KotlinJsDceOptions
        get() = dceOptionsImpl

    override val keep: MutableList<String> = mutableListOf()

    override fun findKotlinCompilerJar(project: Project): File? = findKotlinJsDceJar(project)

    override fun compile() {}

    override fun keep(vararg fqn: String) {
        keep += fqn
    }

    @TaskAction
    fun performDce() {
        val inputFiles = getSource().files.map { it.path }

        val outputDirArgs = arrayOf("-output-dir", destinationDir.path)

        val args = K2JSDceArguments()
        dceOptionsImpl.updateArguments(args)
        args.declarationsToKeep = keep.toTypedArray()
        val argsArray = ArgumentUtils.convertArgumentsToStringList(args).toTypedArray()
        val exitCode = CLITool.doMainNoExit(K2JSDce(), argsArray + outputDirArgs + inputFiles)
        throwGradleExceptionIfError(exitCode)
    }
}