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
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.fillDefaultValues
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.incremental.ChangedFiles
import java.io.File

@CacheableTask
open class KotlinCompileCommon : AbstractKotlinCompile<K2MetadataCompilerArguments>(), KotlinCommonCompile {

    override val kotlinOptions: KotlinMultiplatformCommonOptions
        get() = taskData.compilation.kotlinOptions as KotlinMultiplatformCommonOptionsImpl

    override fun createCompilerArgs(): K2MetadataCompilerArguments =
        K2MetadataCompilerArguments()

    override fun getSourceRoots(): SourceRoots =
        SourceRoots.KotlinOnly.create(getSource(), sourceFilesExtensions)

    override fun findKotlinCompilerClasspath(project: Project): List<File> =
        findKotlinMetadataCompilerClasspath(project)

    override fun setupCompilerArgs(args: K2MetadataCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        args.apply { fillDefaultValues() }
        super.setupCompilerArgs(args, defaultsOnly = defaultsOnly, ignoreClasspathResolutionErrors = ignoreClasspathResolutionErrors)

        args.moduleName = this@KotlinCompileCommon.moduleName

        if ((taskData.compilation as? KotlinCommonCompilation)?.isKlibCompilation == true) {
            args.expectActualLinker = true
        }

        if (defaultsOnly) return

        val classpathList = classpath.files.filter { it.exists() }.toMutableList()

        with(args) {
            classpath = classpathList.joinToString(File.pathSeparator)
            destination = destinationDir.canonicalPath

            friendPaths = this@KotlinCompileCommon.friendPaths.files.map { it.absolutePath }.toTypedArray()
            refinesPaths = refinesMetadataPaths.map { it.absolutePath }.toTypedArray()
        }

        (kotlinOptions as KotlinMultiplatformCommonOptionsImpl).updateArguments(args)
    }

    private fun outputPathsFromMetadataCompilationsOf(sourceSets: Iterable<KotlinSourceSet>): List<File> {
        val target = taskData.compilation.target
        return sourceSets
            .mapNotNull { sourceSet -> target.compilations.findByName(sourceSet.name)?.output?.classesDirs }
            .flatten()
    }

    private val defaultKotlinSourceSet: KotlinSourceSet
        get() = taskData.compilation.defaultSourceSet

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal val refinesMetadataPaths by project.provider {
        outputPathsFromMetadataCompilationsOf(defaultKotlinSourceSet.getSourceSetHierarchy().minus(defaultKotlinSourceSet))
    }

    override fun callCompilerAsync(args: K2MetadataCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        val messageCollector = GradlePrintingMessageCollector(logger, args.allWarningsAsErrors)
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = compilerRunner
        val environment = GradleCompilerEnvironment(
            computedCompilerClasspath, messageCollector, outputItemCollector,
            buildReportMode = buildReportMode,
            outputFiles = allOutputFiles()
        )
        compilerRunner.runMetadataCompilerAsync(sourceRoots.kotlinSourceFiles, args, environment)
    }
}
