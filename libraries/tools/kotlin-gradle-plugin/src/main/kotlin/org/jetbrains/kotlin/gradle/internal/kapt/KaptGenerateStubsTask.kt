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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.tasks.FilteringSourceRootsContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.SourceRoots
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.isParentOf
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile
import java.io.File
import javax.inject.Inject

@CacheableTask
open class KaptGenerateStubsTask @Inject constructor(
    objectFactory: ObjectFactory
) : KotlinCompile() {
    override val sourceRootsContainer = FilteringSourceRootsContainer(emptyList(), { isSourceRootAllowed(it) })

    override val kotlinOptions: KotlinJvmOptions = KotlinJvmOptionsImpl()

    @get:Internal
    @field:Transient // can't serialize task references in Gradle instant execution state
    internal lateinit var kotlinCompileTask: KotlinCompile

    @get:OutputDirectory
    val stubsDir: DirectoryProperty = objectFactory.directoryProperty()

    @get:Internal
    lateinit var generatedSourcesDirs: List<File>

    @get:Classpath
    @get:InputFiles
    val kaptClasspath: FileCollection
        get() = objects.fileCollection().from(kaptClasspathConfigurations)

    @get:Internal
    internal lateinit var kaptClasspathConfigurations: List<Configuration>

    @get:Classpath
    @get:InputFiles
    @Suppress("unused")
    internal val kotlinTaskPluginClasspath by project.provider {
        kotlinCompileTask.pluginClasspath
    }

    @get:Input
    override var useModuleDetection: Boolean
        get() = super.useModuleDetection
        set(_) {
            error("KaptGenerateStubsTask.useModuleDetection setter should not be called!")
        }

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

    private val compileKotlinArgumentsContributor by project.provider {
        kotlinCompileTask.compilerArgumentsContributor
    }

    override fun setupCompilerArgs(args: K2JVMCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        compileKotlinArgumentsContributor.contributeArguments(args, compilerArgumentsConfigurationFlags(
            defaultsOnly,
            ignoreClasspathResolutionErrors
        ))

        val pluginOptionsWithKapt = pluginOptions.withWrappedKaptOptions(withApClasspath = kaptClasspath)
        args.pluginOptions = (pluginOptionsWithKapt.arguments + args.pluginOptions!!).toTypedArray()

        args.verbose = verbose
        args.classpathAsList = this.compileClasspath.filter { it.exists() }.toList()
        args.destinationAsFile = this.destinationDir
    }

    private val sourceRoots by project.provider {
        kotlinCompileTask.getSourceRoots().let {
            val javaSourceRoots = it.javaSourceRoots.filterTo(HashSet()) { isSourceRootAllowed(it) }
            val kotlinSourceFiles = it.kotlinSourceFiles.filterTo(ArrayList()) { isSourceRootAllowed(it) }
            SourceRoots.ForJvm(kotlinSourceFiles, javaSourceRoots)
        }
    }

    override fun getSourceRoots(): SourceRoots.ForJvm = sourceRoots
}