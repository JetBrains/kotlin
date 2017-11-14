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

import com.intellij.openapi.util.io.FileUtil
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.gradle.tasks.FilteringSourceRootsContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile
import org.jetbrains.kotlin.incremental.pathsAsStringRelativeTo
import java.io.File

open class KaptGenerateStubsTask : KotlinCompile() {
    override val sourceRootsContainer = FilteringSourceRootsContainer(emptyList(), { isSourceRootAllowed(it) })

    @get:Internal
    internal lateinit var kotlinCompileTask: KotlinCompile

    @get:OutputDirectory
    lateinit var stubsDir: File

    @get:OutputDirectory
    lateinit var generatedSourcesDir: File

    override fun source(vararg sources: Any?): SourceTask? {
        return super.source(sourceRootsContainer.add(sources))
    }
    override fun setSource(sources: Any?) {
        super.setSource(sourceRootsContainer.set(sources))
    }

    private fun isSourceRootAllowed(source: File): Boolean {
        fun File.isInside(parent: File) = FileUtil.isAncestor(parent, this, /* strict = */ false)

        return !source.isInside(destinationDir) &&
               !source.isInside(stubsDir) &&
               !source.isInside(generatedSourcesDir)
    }

    override fun execute(inputs: IncrementalTaskInputs) {
        val sourceRoots = kotlinCompileTask.getSourceRoots()
        val allKotlinSources = sourceRoots.kotlinSourceFiles

        logger.kotlinDebug { "All kotlin sources: ${allKotlinSources.pathsAsStringRelativeTo(project.rootProject.projectDir)}" }

        if (allKotlinSources.isEmpty()) {
            logger.kotlinDebug { "No Kotlin files found, skipping KaptGenerateStubs task" }
            return
        }

        sourceRoots.log(this.name, logger)
        // todo handle the args like those of the compile tasks
        val args = createCompilerArgs()

        kotlinCompileTask.setupCompilerArgs(args)
        args.pluginClasspaths = (pluginOptions.classpath + args.pluginClasspaths!!).toSet().toTypedArray()
        args.pluginOptions = (pluginOptions.arguments + args.pluginOptions!!).toTypedArray()
        args.verbose = project.hasProperty("kapt.verbose") && project.property("kapt.verbose").toString().toBoolean() == true
        args.classpathAsList = this.compileClasspath.toList()
        args.destinationAsFile = this.destinationDir

        compilerCalled = true
        callCompiler(args, sourceRoots, ChangedFiles(inputs))
    }
}