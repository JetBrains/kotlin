/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.work.InputChanges
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptIncrementalChanges
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.report.ReportingSettings
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.toPathsArray
import java.io.File
import javax.inject.Inject

abstract class KaptWithKotlincTask @Inject constructor(
    objectFactory: ObjectFactory
) : KaptTask(objectFactory),
    CompilerArgumentAwareWithInput<K2JVMCompilerArguments>,
    CompileUsingKotlinDaemonWithNormalization {

    class Configurator(kotlinCompileTask: KotlinCompile): KaptTask.Configurator<KaptWithKotlincTask>(kotlinCompileTask) {
        override fun configure(task: KaptWithKotlincTask) {
            super.configure(task)
            task.pluginClasspath.from(kotlinCompileTask.pluginClasspath)
            task.compileKotlinArgumentsContributor.set(
                task.project.provider { kotlinCompileTask.compilerArgumentsContributor }
            )
            task.javaPackagePrefix.set(task.project.provider { kotlinCompileTask.javaPackagePrefix })
            task.reportingSettings.set(task.project.provider { kotlinCompileTask.reportingSettings() })
        }
    }

    @get:Internal
    internal val pluginOptions = CompilerPluginOptions()

    @get:Classpath
    abstract val pluginClasspath: ConfigurableFileCollection

    @get:Internal
    val taskProvider: Provider<GradleCompileTaskProvider> = objectFactory.property(
        objectFactory.newInstance<GradleCompileTaskProvider>(project.gradle, this, project)
    )

    override fun createCompilerArgs(): K2JVMCompilerArguments = K2JVMCompilerArguments()

    abstract override val kotlinDaemonJvmArguments: ListProperty<String>

    @get:Internal
    internal abstract val compileKotlinArgumentsContributor: Property<CompilerArgumentsContributor<K2JVMCompilerArguments>>

    override fun setupCompilerArgs(args: K2JVMCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        compileKotlinArgumentsContributor.get().contributeArguments(
            args, compilerArgumentsConfigurationFlags(
                defaultsOnly,
            ignoreClasspathResolutionErrors
        ))

        args.pluginClasspaths = pluginClasspath.toPathsArray()

        val pluginOptionsWithKapt: CompilerPluginOptions = pluginOptions.withWrappedKaptOptions(
            withApClasspath = kaptClasspath,
            changedFiles = changedFiles,
            classpathChanges = classpathChanges,
            compiledSourcesDir = compiledSources.toList(),
            processIncrementally = processIncrementally
        )

        args.pluginOptions = (pluginOptionsWithKapt.arguments + args.pluginOptions!!).toTypedArray()
        args.verbose = verbose.get()
    }

    /**
     * This will be part of the subplugin options that is not part of the input snapshotting, so just initialize it. Actual value is set
     * in the task action.
     */
    private var changedFiles: List<File> = emptyList()
    private var classpathChanges: List<String> = emptyList()
    private var processIncrementally = false

    private val javaPackagePrefix = objectFactory.property(String::class.java)
    private val reportingSettings = objectFactory.property(ReportingSettings::class.java)

    @TaskAction
    fun compile(inputChanges: InputChanges) {
        logger.debug("Running kapt annotation processing using the Kotlin compiler")
        checkAnnotationProcessorClasspath()

        val incrementalChanges = getIncrementalChanges(inputChanges)
        if (incrementalChanges is KaptIncrementalChanges.Known) {
            changedFiles = incrementalChanges.changedSources.toList()
            classpathChanges = incrementalChanges.changedClasspathJvmNames.toList()
            processIncrementally = true
        }

        val args = prepareCompilerArguments()

        val messageCollector = GradlePrintingMessageCollector(GradleKotlinLogger(logger), args.allWarningsAsErrors)
        val outputItemCollector = OutputItemsCollectorImpl()
        val environment = GradleCompilerEnvironment(
            compilerClasspath.files.toList(), messageCollector, outputItemCollector,
            reportingSettings = reportingSettings.get(),
            outputFiles = allOutputFiles()
        )

        val compilerRunner = GradleCompilerRunner(
            taskProvider.get(),
            defaultKotlinJavaToolchain.get().currentJvmJdkToolsJar.orNull,
            normalizedKotlinDaemonJvmArguments.orNull
        )
        compilerRunner.runJvmCompilerAsync(
            sourcesToCompile = emptyList(),
            commonSources = emptyList(),
            javaSourceRoots = source.files,
            javaPackagePrefix = javaPackagePrefix.orNull,
            args = args,
            environment = environment,
            jdkHome = defaultKotlinJavaToolchain.get().providedJvm.get().javaHome,
            null
        )
    }
}
