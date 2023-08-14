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
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.cli.common.arguments.DevModeOverwritingStrategies
import org.jetbrains.kotlin.cli.common.arguments.K2JSDceArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.compilerRunner.runToolInSeparateProcess
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.create
import org.jetbrains.kotlin.gradle.utils.absolutePathWithoutExtension
import org.jetbrains.kotlin.gradle.utils.fileExtensionCasePermutations
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class KotlinJsDce @Inject constructor(
    objectFactory: ObjectFactory
) : AbstractKotlinCompileTool<K2JSDceArguments>(objectFactory),
    KotlinToolTask<KotlinJsDceCompilerToolOptions>,
    KotlinJsDce {

    init {
        cacheOnlyIfEnabledForKotlin()
        outputs.cacheIf { !isDevMode }

        include("js".fileExtensionCasePermutations().map { "**/*.$it" })
    }

    override val toolOptions: KotlinJsDceCompilerToolOptions = objectFactory.newInstance<KotlinJsDceCompilerToolOptionsDefault>()

    override fun createCompilerArguments(context: CreateCompilerArgumentsContext) = context.create<K2JSDceArguments> {
        primitive { args ->
            KotlinJsDceCompilerToolOptionsHelper.fillCompilerArguments(toolOptions, args)
            args.declarationsToKeep = keep.toTypedArray()
            args.outputDirectory = destinationDirectory.get().asFile.path
            args.devModeOverwritingStrategy = DevModeOverwritingStrategies.ALL
        }
    }

    // DCE can be broken in case of non-kotlin js files or modules
    @Internal
    var kotlinFilesOnly: Boolean = false

    @get:Internal
    override val dceOptions: KotlinJsDceOptions = object : KotlinJsDceOptions {
        override val options: KotlinJsDceCompilerToolOptions
            get() = toolOptions
    }

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
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    override val sources: FileCollection = super.sources

    @get:Incremental
    @get:InputFiles
    @get:NormalizeLineEndings
    abstract override val libraries: ConfigurableFileCollection

    private val buildDir = project.layout.buildDirectory

    private val isDevMode
        get() = toolOptions.devMode.get() || toolOptions.freeCompilerArgs.get().contains("-dev-mode")

    private val isExplicitDevModeAllStrategy
        get() = strategyAllArg in toolOptions.freeCompilerArgs.get() ||
                strategyOlderArg !in toolOptions.freeCompilerArgs.get() &&
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

        val arguments = createCompilerArguments()
        arguments.freeArgs += inputFiles

        val exitCode = runToolInSeparateProcess(
            ArgumentUtils.convertArgumentsToStringList(arguments).toTypedArray(),
            dceToolMainClassName,
            defaultCompilerClasspath,
            GradleKotlinLogger(logger),
            buildDir.get().asFile,
            jvmArgs
        )
        throwExceptionIfCompilationFailed(exitCode, KotlinCompilerExecutionStrategy.OUT_OF_PROCESS)
    }

    private fun isDceCandidate(file: File): Boolean {
        if (file.extension == "jar") {
            return true
        }

        if (file.extension != "js" || file.name.endsWith(".meta.js")) {
            return false
        }

        return File("${file.absolutePathWithoutExtension()}.meta.js").exists()
    }

    companion object {
        const val strategyAllArg = "-Xdev-mode-overwriting-strategy=${DevModeOverwritingStrategies.ALL}"
        const val strategyOlderArg = "-Xdev-mode-overwriting-strategy=${DevModeOverwritingStrategies.OLDER}"
        const val dceToolMainClassName = "org.jetbrains.kotlin.cli.js.dce.K2JSDce"
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("KTIJ-25227: Necessary override for IDEs < 2023.2", level = DeprecationLevel.ERROR)
    override fun setupCompilerArgs(args: K2JSDceArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        @Suppress("DEPRECATION_ERROR")
        super.setupCompilerArgs(args, defaultsOnly, ignoreClasspathResolutionErrors)
    }
}
