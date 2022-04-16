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

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.jetbrains.kotlin.cli.common.arguments.DevModeOverwritingStrategies
import org.jetbrains.kotlin.cli.common.arguments.K2JSDceArguments
import org.jetbrains.kotlin.cli.js.dce.K2JSDce
import org.jetbrains.kotlin.compilerRunner.runToolInSeparateProcess
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptionsImpl
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.utils.canonicalPathWithoutExtension
import org.jetbrains.kotlin.gradle.utils.fileExtensionCasePermutations
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class KotlinJsDce @Inject constructor(
    objectFactory: ObjectFactory
) : AbstractKotlinCompileTool<K2JSDceArguments>(objectFactory),
    KotlinJsDce {

    init {
        cacheOnlyIfEnabledForKotlin()

        include("js".fileExtensionCasePermutations().map { "**/*.$it" })
    }

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

    override fun keep(vararg fqn: String) {
        keep += fqn
    }

    @Input
    var jvmArgs = mutableListOf<String>()

    // Source could be empty, while classpath not
    @get:Incremental
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    override val sources: FileCollection = super.sources

    @get:Incremental
    @get:InputFiles
    abstract override val libraries: ConfigurableFileCollection

    private val buildDir = project.layout.buildDirectory

    private val isDevMode
        get() = dceOptions.devMode || "-dev-mode" in dceOptions.freeCompilerArgs

    private val isExplicitDevModeAllStrategy
        get() = strategyAllArg in dceOptions.freeCompilerArgs ||
                strategyOlderArg !in dceOptions.freeCompilerArgs &&
                System.getProperty("kotlin.js.dce.devmode.overwriting.strategy") == DevModeOverwritingStrategies.ALL

    @TaskAction
    fun performDce(inputChanges: InputChanges) {
        validateCompilerClasspath()
        // in case of explicit `all` strategy do not perform incremental copy
        val shouldPerformIncrementalCopy = isDevMode && !isExplicitDevModeAllStrategy

        val classpathFiles = if (shouldPerformIncrementalCopy) {
            inputChanges.getFileChanges(libraries)
                .filter { it.changeType == ChangeType.MODIFIED || it.changeType == ChangeType.ADDED }
                .map { it.file }
        } else {
            libraries.asFileTree.files
        }

        val inputFiles = sources.asFileTree.files.plus(classpathFiles)
            .filter { !kotlinFilesOnly || isDceCandidate(it) }
            .map { it.path }

        val outputDirArgs = arrayOf("-output-dir", destinationDirectory.get().asFile.path)

        val processedSerializedArgs = if (shouldPerformIncrementalCopy) {
            var shouldAddStrategyAllArgument = true
            val processedArgs = serializedCompilerArguments
                .map { if (it == strategyOlderArg) strategyAllArg.also { shouldAddStrategyAllArgument = false } else it }
            if (shouldAddStrategyAllArgument) processedArgs + strategyAllArg else processedArgs
        } else {
            serializedCompilerArguments
        }
        val argsArray = processedSerializedArgs.toTypedArray()

        val log = GradleKotlinLogger(logger)
        val allArgs = argsArray + outputDirArgs + inputFiles

        val exitCode = runToolInSeparateProcess(
            allArgs,
            K2JSDce::class.java.name,
            defaultCompilerClasspath,
            log,
            buildDir.get().asFile,
            jvmArgs
        )
        throwGradleExceptionIfError(exitCode, KotlinCompilerExecutionStrategy.OUT_OF_PROCESS)
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

    companion object {
        const val strategyAllArg = "-Xdev-mode-overwriting-strategy=${DevModeOverwritingStrategies.ALL}"
        const val strategyOlderArg = "-Xdev-mode-overwriting-strategy=${DevModeOverwritingStrategies.OLDER}"
    }
}