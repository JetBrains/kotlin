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

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.FilteringSourceRootsContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.SourceRoots
import org.jetbrains.kotlin.gradle.utils.isParentOf
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile
import java.io.File
import java.util.concurrent.Callable

@CacheableTask
abstract class KaptGenerateStubsTask : KotlinCompile() {

    class Configurator(private val kotlinCompileTask: KotlinCompile, kotlinCompilation: KotlinCompilationData<*>) :
        AbstractKotlinCompile.Configurator<KaptGenerateStubsTask>(kotlinCompilation) {

        override fun configure(task: KaptGenerateStubsTask) {
            super.configure(task)
            val providerFactory = kotlinCompileTask.project.providers
            task.useModuleDetection.value(kotlinCompileTask.useModuleDetection).disallowChanges()
            task.moduleName.value(kotlinCompileTask.moduleName).disallowChanges()
            task.classpath = task.project.files(Callable { kotlinCompileTask.classpath })
            task.kotlinTaskPluginClasspath.from(
                providerFactory.provider { kotlinCompileTask.pluginClasspath }
            )
            task.compileKotlinArgumentsContributor.set(
                providerFactory.provider {
                    kotlinCompileTask.compilerArgumentsContributor
                }
            )
            task.jvmSourceRoots.set(
                providerFactory.provider {
                    kotlinCompileTask.getSourceRoots().let { compileTaskSourceRoots ->
                        SourceRoots.ForJvm(
                            compileTaskSourceRoots.kotlinSourceFiles.filterTo(mutableListOf()) { task.isSourceRootAllowed(it) },
                            javaSourceRootsProvider = { compileTaskSourceRoots.javaSourceRoots.filterTo(mutableSetOf()) { task.isSourceRootAllowed(it) } }
                        )
                    }
                }
            )
            task.kotlinOptionsProperty.value(KotlinJvmOptionsImpl()).disallowChanges()
        }
    }


    @field:Transient
    override val sourceRootsContainer = FilteringSourceRootsContainer(objects, { isSourceRootAllowed(it) })

    @get:OutputDirectory
    abstract val stubsDir: DirectoryProperty

    @get:Internal
    lateinit var generatedSourcesDirs: List<File>

    @get:Classpath
    @get:InputFiles
    abstract val kaptClasspath: ConfigurableFileCollection

    @get:Classpath
    @get:InputFiles
    @Suppress("unused")
    internal abstract val kotlinTaskPluginClasspath: ConfigurableFileCollection

    @get:Input
    val verbose = (project.hasProperty("kapt.verbose") && project.property("kapt.verbose").toString().toBoolean() == true)

    override fun source(vararg sources: Any): SourceTask {
        return super.source(sourceRootsContainer.add(sources))
    }

    override fun setSource(sources: Any) {
        super.setSource(sourceRootsContainer.set(sources))
    }

    private fun isSourceRootAllowed(source: File): Boolean =
        !destinationDir.isParentOf(source) &&
                !stubsDir.asFile.get().isParentOf(source) &&
                generatedSourcesDirs.none { it.isParentOf(source) }

    @get:Internal
    internal abstract val compileKotlinArgumentsContributor: Property<CompilerArgumentsContributor<K2JVMCompilerArguments>>

    override fun setupCompilerArgs(args: K2JVMCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        compileKotlinArgumentsContributor.get().contributeArguments(args, compilerArgumentsConfigurationFlags(
            defaultsOnly,
            ignoreClasspathResolutionErrors
        ))

        val pluginOptionsWithKapt = pluginOptions.withWrappedKaptOptions(withApClasspath = kaptClasspath)
        args.pluginOptions = (pluginOptionsWithKapt.arguments + args.pluginOptions!!).toTypedArray()

        args.verbose = verbose
        args.classpathAsList = this.classpath.filter { it.exists() }.toList()
        args.destinationAsFile = this.destinationDir
    }

    @get:Internal
    internal abstract val jvmSourceRoots: Property<SourceRoots.ForJvm>

    override fun getSourceRoots(): SourceRoots.ForJvm = jvmSourceRoots.get()
}