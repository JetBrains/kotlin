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

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.gradle.incremental.ChangedFiles
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.gradle.tasks.FilteringSourceRootsContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.SourceRoots
import org.jetbrains.kotlin.gradle.utils.isParentOf
import org.jetbrains.kotlin.gradle.utils.pathsAsStringRelativeTo
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile
import java.io.File

@CacheableTask
open class KaptGenerateStubsTask : KotlinCompile() {
    override val sourceRootsContainer = FilteringSourceRootsContainer(emptyList(), { isSourceRootAllowed(it) })

    @get:Internal
    internal lateinit var kotlinCompileTask: KotlinCompile

    @get:OutputDirectory
    lateinit var stubsDir: File

    @get:Internal
    lateinit var generatedSourcesDir: File

    @get:Classpath
    @get:InputFiles
    val kaptClasspath: FileCollection
        get() = project.files(*kaptClasspathConfigurations.toTypedArray())

    @get:Internal
    internal lateinit var kaptClasspathConfigurations: List<Configuration>

    @get:Classpath
    @get:InputFiles
    @Suppress("unused")
    internal val kotlinTaskPluginClasspath
        get() = kotlinCompileTask.pluginClasspath

    override fun source(vararg sources: Any?): SourceTask? {
        return super.source(sourceRootsContainer.add(sources))
    }

    override fun setSource(sources: Any?) {
        super.setSource(sourceRootsContainer.set(sources))
    }

    private fun isSourceRootAllowed(source: File): Boolean =
        !destinationDir.isParentOf(source) &&
                !stubsDir.isParentOf(source) &&
                !generatedSourcesDir.isParentOf(source)

    override fun setupCompilerArgs(args: K2JVMCompilerArguments, defaultsOnly: Boolean) {
        kotlinCompileTask.setupCompilerArgs(args)

        val pluginOptionsWithKapt = pluginOptions.withWrappedKaptOptions(withApClasspath = kaptClasspath)
        args.pluginOptions = (pluginOptionsWithKapt.arguments + args.pluginOptions!!).toTypedArray()

        args.verbose = project.hasProperty("kapt.verbose") && project.property("kapt.verbose").toString().toBoolean() == true
        args.classpathAsList = this.compileClasspath.toList()
        args.destinationAsFile = this.destinationDir
    }

    override fun execute(inputs: IncrementalTaskInputs) {
        val sourceRoots = kotlinCompileTask.getSourceRoots().let {
            val javaSourceRoots = it.javaSourceRoots.filterTo(HashSet()) { isSourceRootAllowed(it) }
            val kotlinSourceFiles = it.kotlinSourceFiles
            SourceRoots.ForJvm(kotlinSourceFiles, javaSourceRoots)
        }
        val allKotlinSources = sourceRoots.kotlinSourceFiles

        logger.kotlinDebug { "All kotlin sources: ${allKotlinSources.pathsAsStringRelativeTo(project.rootProject.projectDir)}" }

        if (allKotlinSources.isEmpty()) {
            logger.kotlinDebug { "No Kotlin files found, skipping KaptGenerateStubs task" }
            return
        }

        sourceRoots.log(this.name, logger)
        val args = prepareCompilerArguments()

        callCompiler(args, sourceRoots, ChangedFiles(inputs))
    }
}