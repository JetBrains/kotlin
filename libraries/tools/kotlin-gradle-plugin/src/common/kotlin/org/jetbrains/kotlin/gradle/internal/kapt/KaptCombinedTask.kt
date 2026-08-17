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

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.work.Incremental
import org.gradle.work.NormalizeLineEndings
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KaptStubGenerationScheme
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig
import org.jetbrains.kotlin.gradle.plugin.FilesSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.create
import org.jetbrains.kotlin.gradle.report.BuildReportMode
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.*
import javax.inject.Inject
import kotlin.collections.map

@CacheableTask
abstract class KaptCombinedTask @Inject constructor(
    project: Project,
    workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory,
) : KotlinCompile(
    objectFactory.KotlinJvmCompilerOptionsDefault(project),
    workerExecutor,
    objectFactory
), KaptGenerateStubs, Kapt {

    @get:Input
    abstract val verbose: Property<Boolean>

    /**
     * The directory where the generated related [KaptGenerateStubs] task stub can be found.
     */
    @get:OutputDirectory
    abstract override val stubsDir: DirectoryProperty

    /* Kapt stubs properties */

    // Bug in Gradle - without this override Gradle complains @Internal is not
    // compatible with @Classpath and @Incremental annotations
    @get:Internal
    abstract override val libraries: ConfigurableFileCollection

    /**
     * [K2MultiplatformStructure] is not required for Kapt stubs
     */
    @InternalKotlinGradlePluginApi
    @get:Internal
    override val multiplatformStructure: K2MultiplatformStructure get() = super.multiplatformStructure

    /* Used as input as empty kapt classpath should not trigger stub generation, but a non-empty one should. */
    @Input
    fun getIfKaptClasspathIsPresent() = !kaptClasspath.isEmpty

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
    abstract val kotlinCompileDestinationDirectory: DirectoryProperty

    override val incrementalProps: List<FileCollection>
        get() = listOf(
            sources,
            javaSources,
            commonSourceSet,
            classpathSnapshotProperties.classpathSnapshot
        )

    /* Kapt apt properties */

    @Deprecated(
        "Use annotationProcessorOptionsProviders instead. Scheduled for removal in Kotlin 2.4.",
        replaceWith = ReplaceWith("annotationProcessorOptionsProviders")
    )
    override val annotationProcessorOptionProviders: MutableList<Any> = mutableListOf()

    @get:Input
    internal var isIncremental = true

    @get:Internal
    var useBuildCache: Boolean = false

    @get:PathSensitive(PathSensitivity.NONE)
    @get:Incremental
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:Optional
    @get:InputFiles
    abstract val classpathStructure: ConfigurableFileCollection

    /**
     * Contains all Java source code used in this compilation
     * and generated by related [KaptGenerateStubs] task stubs.
     */
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val javaSourcesForApt: ConfigurableFileCollection

    @get:Input
    var mapDiagnosticLocations: Boolean = false

    @get:Input
    val stubGenerationScheme: Property<KaptStubGenerationScheme> =
        objectFactory.propertyWithConvention(KaptStubGenerationScheme.JTREE)

    @get:Input
    abstract val annotationProcessorFqNames: ListProperty<String>

    @get:Input
    abstract val javacOptions: MapProperty<String, String>

    @get:Internal
    internal val projectDir = project.projectDir

    @get:Input
    var disableClassloaderCacheForProcessors: Set<String> = emptySet()

    @get:Nested
    abstract val kaptPluginOptions: ListProperty<CompilerPluginConfig>

    override fun createCompilerArguments(context: CreateCompilerArgumentsContext) = context.create<K2JVMCompilerArguments> {
        primitive { args ->
            args.allowNoSourceFiles = true

            KotlinJvmCompilerOptionsHelper.fillCompilerArguments(compilerOptions, args)

            requireNotNull(args.moduleName)

            // Copied from KotlinCompile
            if (reportingSettings().buildReportMode == BuildReportMode.VERBOSE) {
                args.reportPerf = true
            }

            val pluginOptionsWithKapt: CompilerPluginOptions = pluginOptions.toSingleCompilerPluginOptions()
//                .withWrappedKaptOptions(withApClasspath = kaptClasspath)
            kaptClasspath.map { FilesSubpluginOption("apclasspath", listOf(it)) }.forEach { it: FilesSubpluginOption ->
                pluginOptionsWithKapt.addPluginArgument(Kapt3GradleSubplugin.KAPT_SUBPLUGIN_ID, it)
            }

            args.pluginOptions = (pluginOptionsWithKapt.arguments).toTypedArray() + getAnnotationProcessorOptions().map { (key, value) ->
                "plugin:${Kapt3GradleSubplugin.KAPT_SUBPLUGIN_ID}:apOption=$key=$value"
            }

            args.verbose = verbose.get()
            args.destinationAsFile = destinationDirectory.get().asFile
        }

        pluginClasspath { args ->
            args.pluginClasspaths = runSafe {
                listOfNotNull(
                    pluginClasspath, kotlinPluginData?.orNull?.classpath
                ).reduce(FileCollection::plus).toPathsArray()
            } ?: emptyArray()
        }

        dependencyClasspath { args ->
            args.classpathAsList = runSafe { libraries.toList().filter { it.exists() } }.orEmpty()
            args.friendPaths = friendPaths.toPathsArray()
        }

        sources { args ->
            args.freeArgs += (scriptSources.asFileTree.files + javaSources.files + sources.asFileTree.files).map { it.absolutePath }
        }
    }

    private fun getAnnotationProcessorOptions(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        kaptPluginOptions.toSingleCompilerPluginOptions().subpluginOptionsByPluginId[Kapt3GradleSubplugin.KAPT_SUBPLUGIN_ID]?.forEach {
            result[it.key] = it.value
        }
        fun addArgumentsFromProvider(provider: CommandLineArgumentProvider) {
            for (argument in provider.asArguments()) {
                result[argument.removePrefix("-A")] = ""
            }
        }

        @Suppress("DEPRECATION")
        val deprecatedProviders = annotationProcessorOptionProviders
        for (providers in deprecatedProviders) {
            for (provider in (providers as List<*>)) {
                addArgumentsFromProvider(provider as CommandLineArgumentProvider)
            }
        }
        for (provider in annotationProcessorOptionsProviders.get()) {
            addArgumentsFromProvider(provider)
        }

        return result
    }
}
