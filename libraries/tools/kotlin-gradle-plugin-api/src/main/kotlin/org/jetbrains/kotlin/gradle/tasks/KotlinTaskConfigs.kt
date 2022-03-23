/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig

interface BaseKotlinCompile : Task {
    @get:OutputDirectory
    val destinationDirectory: DirectoryProperty

    val libraries: ConfigurableFileCollection

    @get:Internal
    val friendPaths: ConfigurableFileCollection

    @get:Classpath
    val pluginClasspath: ConfigurableFileCollection

    @get:Input
    val moduleName: Property<String>

    @get:Internal
    val sourceSetName: Property<String>

    @get:Input
    val multiPlatformEnabled: Property<Boolean>

    @get:Input
    val useModuleDetection: Property<Boolean>

    @get:Nested
    val pluginOptions: ListProperty<CompilerPluginConfig>
}

interface KotlinJvmCompile : BaseKotlinCompile, KotlinCompile<KotlinJvmOptions> {
//    fun setSource(sources: Any)
//
//    fun source(vararg sources: Any): SourceTask

    // JVM specific
    @get:Internal("Takes part in compiler args.")
    val parentKotlinOptionsImpl: Property<KotlinJvmOptions>
}

interface KaptGenerateStubsApi : KotlinJvmCompile {
    @get:OutputDirectory
    val stubsDir: DirectoryProperty

    @get:Internal("Not an input, just passed as kapt args. ")
    val kaptClasspath: ConfigurableFileCollection
}

interface BaseKaptTaskApi : Task {

    //part of kaptClasspath consisting from external artifacts only
    //basically kaptClasspath = kaptExternalClasspath + artifacts built locally
    @get:Classpath
    val kaptExternalClasspath: ConfigurableFileCollection

    @get:Internal
    val kaptClasspathConfigurationNames: ListProperty<String>

    /**
     * Output directory that contains caches necessary to support incremental annotation processing.
     */
    @get:LocalState
    val incAptCache: DirectoryProperty

    @get:OutputDirectory
    val classesDir: DirectoryProperty

    @get:OutputDirectory
    val destinationDir: DirectoryProperty

    /** Used in the model builder only. */
    @get:OutputDirectory
    val kotlinSourcesDestinationDir: DirectoryProperty

    @get:Nested
    val annotationProcessorOptionProviders: MutableList<Any>

    @get:Internal
    val stubsDir: DirectoryProperty

    @get:Classpath
    val kaptClasspath: ConfigurableFileCollection

    @get:Internal
    val compiledSources: ConfigurableFileCollection

    @get:Internal("Task implementation adds correct input annotation.")
    val classpath: ConfigurableFileCollection

    /** Needed for the model builder. */
    @get:Internal
    val sourceSetName: Property<String>

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val source: ConfigurableFileCollection

    @get:Input
    val includeCompileClasspath: Property<Boolean>

    @get:Internal("Used to compute javac option.")
    val defaultJavaSourceCompatibility: Property<String>
}

interface KaptTaskApi : BaseKaptTaskApi {

    @get:Input
    val addJdkClassesToClasspath: Property<Boolean>

    @get:Classpath
    val kaptJars: ConfigurableFileCollection
}