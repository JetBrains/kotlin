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
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.cli.common.arguments.K2JSDceArguments
import org.jetbrains.kotlin.cli.js.dce.K2JSDce
import org.jetbrains.kotlin.compilerRunner.GradleKotlinLogger
import org.jetbrains.kotlin.compilerRunner.createLoggingMessageCollector
import org.jetbrains.kotlin.compilerRunner.runToolInSeparateProcess
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptionsImpl
import java.io.File

@CacheableTask
open class KotlinJsDce : AbstractKotlinCompileTool<K2JSDceArguments>(), KotlinJsDce {

    init {
        cacheOnlyIfEnabledForKotlin()
    }

    override fun createCompilerArgs(): K2JSDceArguments = K2JSDceArguments()

    override fun setupCompilerArgs(args: K2JSDceArguments, defaultsOnly: Boolean) {
        dceOptionsImpl.updateArguments(args)
        args.declarationsToKeep = keep.toTypedArray()
    }

    private val dceOptionsImpl = KotlinJsDceOptionsImpl()

    @get:Internal
    override val dceOptions: KotlinJsDceOptions
        get() = dceOptionsImpl

    @get:Input
    override val keep: MutableList<String> = mutableListOf()

    override fun findKotlinCompilerClasspath(project: Project): List<File> = findKotlinJsDceClasspath(project)

    override fun compile() {}

    override fun keep(vararg fqn: String) {
        keep += fqn
    }

    @TaskAction
    fun performDce() {
        val inputFiles = (listOf(getSource()) + classpath.map { project.fileTree(it) })
            .reduce(FileTree::plus)
            .files.map { it.path }

        val outputDirArgs = arrayOf("-output-dir", destinationDir.path)

        val argsArray = serializedCompilerArguments.toTypedArray()

        val log = GradleKotlinLogger(project.logger)
        val allArgs = argsArray + outputDirArgs + inputFiles
        val exitCode = runToolInSeparateProcess(
            allArgs, K2JSDce::class.java.name, computedCompilerClasspath,
            log, createLoggingMessageCollector(log)
        )
        throwGradleExceptionIfError(exitCode)
    }
}