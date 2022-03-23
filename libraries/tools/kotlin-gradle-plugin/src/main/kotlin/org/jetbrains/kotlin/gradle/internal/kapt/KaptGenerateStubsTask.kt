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
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.isParentOf
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class KaptGenerateStubsTask @Inject constructor(
    workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory
) : KotlinCompile(
    KotlinJvmOptionsImpl(),
    workerExecutor,
    objectFactory
) {

    internal class Configurator(
        private val kotlinCompileTaskProvider: TaskProvider<KotlinCompile>,
        kotlinCompilation: KotlinCompilationData<*>,
        properties: PropertiesProvider,
        private val classpathSnapshotDir: File
    ) : KotlinCompile.Configurator<KaptGenerateStubsTask>(kotlinCompilation, properties) {

        override fun getClasspathSnapshotDir(task: KaptGenerateStubsTask): Provider<Directory> =
            task.project.objects.directoryProperty().fileValue(classpathSnapshotDir)

        override fun configure(task: KaptGenerateStubsTask) {
            super.configure(task)

            val kotlinCompileTask = kotlinCompileTaskProvider.get()
            val providerFactory = kotlinCompileTask.project.providers
            task.useModuleDetection.value(kotlinCompileTask.useModuleDetection).disallowChanges()
            task.moduleName.value(kotlinCompileTask.moduleName).disallowChanges()
            task.classpath.from(kotlinCompileTask.classpath)
            task.kotlinTaskPluginClasspath.from(
                providerFactory.provider { kotlinCompileTask.pluginClasspath }
            )
            task.compileKotlinArgumentsContributor.set(
                providerFactory.provider {
                    kotlinCompileTask.compilerArgumentsContributor
                }
            )
            task.verbose.set(KaptTask.queryKaptVerboseProperty(task.project))
        }
    }

    @get:OutputDirectory
    abstract val stubsDir: DirectoryProperty

    @get:Internal
    lateinit var generatedSourcesDirs: List<File>

    @get:Internal("Not an input, just passed as kapt args. ")
    abstract val kaptClasspath: ConfigurableFileCollection

    /* Used as input as empty kapt classpath should not trigger stub generation, but a non-empty one should. */
    @Input
    fun getIfKaptClasspathIsPresent() = !kaptClasspath.isEmpty

    @get:Classpath
    @Suppress("unused")
    internal abstract val kotlinTaskPluginClasspath: ConfigurableFileCollection

    @get:Input
    abstract val verbose: Property<Boolean>

    /**
     * Changes in this additional sources will trigger stubs regeneration,
     * but the sources themselves will not be used to find kapt annotations and generate stubs.
     */
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Incremental
    abstract val additionalSources: ConfigurableFileCollection

    private fun File.isSourceRootAllowed(): Boolean =
        !destinationDirectory.get().asFile.isParentOf(this) &&
                !stubsDir.asFile.get().isParentOf(this) &&
                generatedSourcesDirs.none { it.isParentOf(this) }

    override fun skipCondition(): Boolean = sources.isEmpty && javaSources.isEmpty

    // Task need to run even if there is no Kotlin sources, but only Java
    @get:Incremental
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    override val sources: FileCollection = super.sources
        .asFileTree
        .matching { patternFilterable ->
            patternFilterable.include { it.isDirectory || it.file.isSourceRootAllowed() }
        }

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

    override val javaSources: FileCollection = super.javaSources
        .asFileTree
        .matching { patternFilterable ->
            patternFilterable.include { it.isDirectory || it.file.isSourceRootAllowed() }
        }

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

        val pluginOptionsWithKapt = pluginOptions.withWrappedKaptOptions(withApClasspath = kaptClasspath)
        args.pluginOptions = (pluginOptionsWithKapt.arguments + args.pluginOptions!!).toTypedArray()

        args.verbose = verbose.get()
        args.classpathAsList = this.classpath.filter { it.exists() }.toList()
        args.destinationAsFile = this.destinationDirectory.get().asFile
    }
}