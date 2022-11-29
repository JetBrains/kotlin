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

import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.work.NormalizeLineEndings
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.report.BuildReportMode
import org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.toSingleCompilerPluginOptions
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile
import javax.inject.Inject

@CacheableTask
abstract class KaptGenerateStubsTask @Inject constructor(
    workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory
) : KotlinCompile(
    objectFactory.newInstance(KotlinJvmCompilerOptionsDefault::class.java),
    workerExecutor,
    objectFactory
), KaptGenerateStubs {

    // Bug in Gradle - without this override Gradle complains @Internal is not
    // compatible with @Classpath and @Incremental annotations
    @get:Internal
    abstract override val libraries: ConfigurableFileCollection

    /* Used as input as empty kapt classpath should not trigger stub generation, but a non-empty one should. */
    @Input
    fun getIfKaptClasspathIsPresent() = !kaptClasspath.isEmpty

    @get:Input
    abstract val verbose: Property<Boolean>

    /**
     * Changes in this additional sources will trigger stubs regeneration,
     * but the sources themselves will not be used to find kapt annotations and generate stubs.
     */
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Incremental
    abstract val additionalSources: ConfigurableFileCollection

    override fun skipCondition(): Boolean = sources.isEmpty && javaSources.isEmpty

    // Task need to run even if there is no Kotlin sources, but only Java
    @get:Incremental
    @get:NormalizeLineEndings
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    override val sources: FileCollection = super.sources

    @get:Internal
    override val scriptSources: FileCollection = objectFactory.fileCollection()

    @get:Internal
    override val androidLayoutResources: FileCollection = objectFactory.fileCollection()

    override val incrementalProps: List<FileCollection>
        get() = listOf(
            sources,
            javaSources,
            commonSourceSet,
            classpathSnapshotProperties.classpath,
            classpathSnapshotProperties.classpathSnapshot
        )

    @get:Internal
    internal abstract val compileKotlinArgumentsContributor: Property<CompilerArgumentsContributor<K2JVMCompilerArguments>>

    override fun setupCompilerArgs(
        args: K2JVMCompilerArguments,
        defaultsOnly: Boolean,
        ignoreClasspathResolutionErrors: Boolean
    ) {
        compileKotlinArgumentsContributor.get().contributeArguments(
            args,
            compilerArgumentsConfigurationFlags(
                defaultsOnly,
                ignoreClasspathResolutionErrors
            )
        )

        // Also use KotlinOptions configuration that was directly set to this task
        // as 'compileKotlinArgumentsContributor' has KotlinOptions from linked KotlinCompile task
        (compilerOptions as KotlinJvmCompilerOptionsDefault).fillCompilerArguments(args)

        // Copied from KotlinCompile
        if (reportingSettings().buildReportMode == BuildReportMode.VERBOSE) {
            args.reportPerf = true
        }

        val pluginOptionsWithKapt = pluginOptions.toSingleCompilerPluginOptions().withWrappedKaptOptions(withApClasspath = kaptClasspath)
        args.pluginOptions = (pluginOptionsWithKapt.arguments).toTypedArray()

        args.verbose = verbose.get()
        args.classpathAsList = this.libraries.filter { it.exists() }.toList()
        args.destinationAsFile = this.destinationDirectory.get().asFile
    }
}
