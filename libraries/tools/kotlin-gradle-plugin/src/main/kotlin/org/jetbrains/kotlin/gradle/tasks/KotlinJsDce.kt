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
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.cli.common.arguments.K2JSDceArguments
import org.jetbrains.kotlin.cli.js.dce.K2JSDce
import org.jetbrains.kotlin.compilerRunner.runToolInSeparateProcess
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptionsImpl
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.utils.canonicalPathWithoutExtension
import java.io.File

@CacheableTask
open class KotlinJsDce : AbstractKotlinCompileTool<K2JSDceArguments>(), KotlinJsDce {

    init {
        cacheOnlyIfEnabledForKotlin()
    }

    @get:Internal
    internal val objects = project.objects

    override fun localStateDirectories(): FileCollection = project.files()

    override fun createCompilerArgs(): K2JSDceArguments = K2JSDceArguments()

    override fun setupCompilerArgs(args: K2JSDceArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        dceOptionsImpl.updateArguments(args)
        args.declarationsToKeep = keep.toTypedArray()
    }

    private val dceOptionsImpl = KotlinJsDceOptionsImpl()

    // DCE can be broken in case of non-kotlin js files or modules
    @Internal
    var kotlinFilesOnly: Boolean = false

    @get:Internal
    override val dceOptions: KotlinJsDceOptions
        get() = dceOptionsImpl

    @get:Input
    override val keep: MutableList<String> = mutableListOf()

    override fun findKotlinCompilerClasspath(project: Project): List<File> = findKotlinJsDceClasspath(project)

    override fun keep(vararg fqn: String) {
        keep += fqn
    }

    @Input
    var jvmArgs = mutableListOf<String>()

    private val buildDir by lazy {
        project.buildDir
    }

    @TaskAction
    fun performDce() {
        val inputFiles = (listOf(source) + classpath
            .filter { !kotlinFilesOnly || isDceCandidate(it) }
            .map { objects.fileCollection().from(it).asFileTree })
            .reduce(FileTree::plus)
            .files.map { it.path }

        val outputDirArgs = arrayOf("-output-dir", destinationDir.path)

        val argsArray = serializedCompilerArguments.toTypedArray()

        val log = GradleKotlinLogger(logger)
        val allArgs = argsArray + outputDirArgs + inputFiles

        val exitCode = runToolInSeparateProcess(
            allArgs,
            K2JSDce::class.java.name,
            computedCompilerClasspath,
            log,
            buildDir,
            jvmArgs
        )
        throwGradleExceptionIfError(exitCode)

    }

    private fun isDceCandidate(file: File): Boolean {
        if (file.extension == "jar") {
            return true
        }

        if (file.extension != "js" || file.name.endsWith(".meta.js")) {
            return false
        }

        return File("${file.canonicalPathWithoutExtension()}.meta.js").exists()
    }
}