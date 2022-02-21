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

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.InputChanges
import org.gradle.work.NormalizeLineEndings
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class KotlinCompileCommon @Inject constructor(
    override val kotlinOptions: KotlinMultiplatformCommonOptions,
    workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory
) : AbstractKotlinCompile<K2MetadataCompilerArguments>(objectFactory),
    KotlinCommonCompile {

    override val compilerRunner: Provider<GradleCompilerRunner> =
        objectFactory.propertyWithConvention(
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

    override fun setupCompilerArgs(args: K2MetadataCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        args.apply { fillDefaultValues() }
        super.setupCompilerArgs(args, defaultsOnly = defaultsOnly, ignoreClasspathResolutionErrors = ignoreClasspathResolutionErrors)

        args.moduleName = this@KotlinCompileCommon.moduleName.get()

        if (expectActualLinker.get()) {
            args.expectActualLinker = true
        }

        if (defaultsOnly) return

        val classpathList = libraries.files.filter { it.exists() }.toMutableList()

        with(args) {
            classpath = classpathList.joinToString(File.pathSeparator)
            destination = destinationDirectory.get().asFile.canonicalPath

            friendPaths = this@KotlinCompileCommon.friendPaths.files.map { it.absolutePath }.toTypedArray()
            refinesPaths = refinesMetadataPaths.map { it.absolutePath }.toTypedArray()
        }

        (kotlinOptions as KotlinMultiplatformCommonOptionsImpl).updateArguments(args)
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:NormalizeLineEndings
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    internal val refinesMetadataPaths: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Internal
    internal val expectActualLinker = objectFactory.property(Boolean::class.java)

    override fun callCompilerAsync(
        args: K2MetadataCompilerArguments,
        kotlinSources: Set<File>,
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
        compilerRunner.runMetadataCompilerAsync(
            kotlinSources.toList(),
            commonSourceSet.files.toList(),
            args,
            environment
        )
    }
}
