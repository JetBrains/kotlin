/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.gradle.api.tasks.CacheableTask
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.fillDefaultValues
import org.jetbrains.kotlin.incremental.ChangedFiles
import java.io.File

@CacheableTask
internal open class KotlinCompileCommon : AbstractKotlinCompile<K2MetadataCompilerArguments>(), KotlinCommonCompile {

    private val kotlinOptionsImpl = KotlinMultiplatformCommonOptionsImpl()
    override val kotlinOptions: KotlinMultiplatformCommonOptions
        get() = kotlinOptionsImpl

    override fun createCompilerArgs(): K2MetadataCompilerArguments =
            K2MetadataCompilerArguments()

    override fun getSourceRoots(): SourceRoots =
            SourceRoots.KotlinOnly.create(getSource(), sourceFilesExtensions)

    override fun findKotlinCompilerClasspath(project: Project): List<File>  =
            findKotlinMetadataCompilerClasspath(project)

    override fun setupCompilerArgs(args: K2MetadataCompilerArguments, defaultsOnly: Boolean) {
        args.apply { fillDefaultValues() }
        super.setupCompilerArgs(args, defaultsOnly)

        args.moduleName = friendTask?.moduleName ?: this@KotlinCompileCommon.moduleName

        if (defaultsOnly) return

        val classpathList = classpath.files.toMutableList()
        friendTask?.let { classpathList.add(it.destinationDir) }

        with(args) {
            classpath = classpathList.joinToString(File.pathSeparator)
            destination = destinationDir.canonicalPath
        }

        kotlinOptionsImpl.updateArguments(args)
    }

    override fun callCompiler(args: K2MetadataCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        val messageCollector = GradleMessageCollector(logger)
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = GradleCompilerRunner(project)
        val environment = GradleCompilerEnvironment(computedCompilerClasspath, messageCollector, outputItemCollector, args)
        val exitCode = compilerRunner.runMetadataCompiler(sourceRoots.kotlinSourceFiles, args, environment)
        throwGradleExceptionIfError(exitCode)
    }
}