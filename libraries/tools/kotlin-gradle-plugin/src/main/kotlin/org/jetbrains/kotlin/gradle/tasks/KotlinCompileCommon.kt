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
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.AbstractKotlinFragmentMetadataCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinMetadataCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.refinesClosure
import org.jetbrains.kotlin.gradle.plugin.sources.resolveAllDependsOnSourceSets
import org.jetbrains.kotlin.gradle.report.BuildReportMode
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class KotlinCompileCommon @Inject constructor(
    override val kotlinOptions: KotlinMultiplatformCommonOptions,
    workerExecutor: WorkerExecutor
) : AbstractKotlinCompile<K2MetadataCompilerArguments>(), KotlinCommonCompile {

    class Configurator(compilation: KotlinCompilationData<*>) : AbstractKotlinCompile.Configurator<KotlinCompileCommon>(compilation) {
        override fun configure(task: KotlinCompileCommon) {
            super.configure(task)
            task.refinesMetadataPaths.from(getRefinesMetadataPaths(task.project)).disallowChanges()
            task.expectActualLinker
                .value(task.project.provider { (compilation as? KotlinCommonCompilation)?.isKlibCompilation == true || compilation is KotlinMetadataCompilationData })
                .disallowChanges()
        }

        private fun getRefinesMetadataPaths(project: Project): Provider<Iterable<File>> {
            return project.provider {
                when (compilation) {
                    is KotlinCompilation<*> -> {
                        val defaultKotlinSourceSet: KotlinSourceSet = compilation.defaultSourceSet
                        val metadataTarget = compilation.owner as KotlinTarget
                        defaultKotlinSourceSet.resolveAllDependsOnSourceSets()
                            .mapNotNull { sourceSet -> metadataTarget.compilations.findByName(sourceSet.name)?.output?.classesDirs }
                            .flatten()
                    }
                    is AbstractKotlinFragmentMetadataCompilationData -> {
                        val fragment = compilation.fragment
                        project.files(
                            fragment.refinesClosure.minus(fragment).map {
                                compilation.metadataCompilationRegistry.byFragment(it).output.classesDirs
                            }
                        )
                    }
                    else -> error("unexpected compilation type")
                }
            }
        }
    }

    override val compilerRunner: Provider<GradleCompilerRunner> =
        objects.propertyWithConvention(
            gradleCompileTaskProvider.map {
                GradleCompilerRunnerWithWorkers(
                    it,
                    null,
                    normalizedKotlinDaemonJvmArguments.orNull,
                    metrics.get(),
                    compilerExecutionStrategy.get(),
                    workerExecutor
                )
            }
        )

    override fun createCompilerArgs(): K2MetadataCompilerArguments =
        K2MetadataCompilerArguments()

    override fun getSourceRoots(): SourceRoots =
        SourceRoots.KotlinOnly.create(getSource(), sourceFilesExtensions.get())

    override fun setupCompilerArgs(args: K2MetadataCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        args.apply { fillDefaultValues() }
        super.setupCompilerArgs(args, defaultsOnly = defaultsOnly, ignoreClasspathResolutionErrors = ignoreClasspathResolutionErrors)

        args.moduleName = this@KotlinCompileCommon.moduleName.get()

        if (expectActualLinker.get()) {
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

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    internal val refinesMetadataPaths: ConfigurableFileCollection = objects.fileCollection()

    @get:Internal
    internal val expectActualLinker = objects.property(Boolean::class.java)

    override fun callCompilerAsync(
        args: K2MetadataCompilerArguments,
        sourceRoots: SourceRoots,
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    ) {
        val messageCollector = GradlePrintingMessageCollector(logger, args.allWarningsAsErrors)
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = compilerRunner.get()
        val environment = GradleCompilerEnvironment(
            defaultCompilerClasspath, messageCollector, outputItemCollector,
            reportingSettings = reportingSettings(),
            outputFiles = allOutputFiles()
        )
        compilerRunner.runMetadataCompilerAsync(sourceRoots.kotlinSourceFiles.files.toList(), args, environment)
    }
}
