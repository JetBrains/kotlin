/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.work.Incremental
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig

interface KotlinCompileTool : PatternFilterable, Task {
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sources: FileCollection

    /**
     * Sets sources for this task.
     * The given sources object is evaluated as per [org.gradle.api.Project.files].
     */
    fun source(vararg sources: Any)

    /**
     * Sets sources for this task.
     * The given sources object is evaluated as per [org.gradle.api.Project.files].
     */
    fun setSource(vararg sources: Any)

    @get:Classpath
    @get:Incremental
    val libraries: ConfigurableFileCollection

    @get:OutputDirectory
    val destinationDirectory: DirectoryProperty

    @Internal
    override fun getExcludes(): MutableSet<String>

    @Internal
    override fun getIncludes(): MutableSet<String>
}

interface BaseKotlinCompile : KotlinCompileTool {

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

@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
interface KotlinJvmCompile : BaseKotlinCompile,
    KotlinCompileDeprecated<KotlinJvmOptionsDeprecated>,
    KotlinCompilationTask<KotlinJvmCompilerOptions> {

    @get:Deprecated(
        message = "Please migrate to compilerOptions.moduleName",
        replaceWith = ReplaceWith("compilerOptions.moduleName")
    )
    @get:Optional
    @get:Input
    override val moduleName: Property<String>

    // JVM specific
    @get:Internal("Takes part in compiler args.")
    @Deprecated(
        message = "Configure compilerOptions directly",
        replaceWith = ReplaceWith("compilerOptions")
    )
    val parentKotlinOptions: Property<KotlinJvmOptionsDeprecated>

    /**
     * Controls JVM target validation mode between this task and the Java compilation task from Gradle for the same source set.
     *
     * The same JVM targets ensure that the produced jar file contains class files of the same JVM bytecode version,
     * which is important to avoid compatibility issues for the code consumers.
     *
     * Also, Gradle Java compilation task [org.gradle.api.tasks.compile.JavaCompile.targetCompatibility] controls value
     * of "org.gradle.jvm.version" [attribute](https://docs.gradle.org/current/javadoc/org/gradle/api/attributes/java/TargetJvmVersion.html)
     * which itself controls the produced artifact minimal supported JVM version via
     * [Gradle Module Metadata](https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html).
     * This allows Gradle to check compatibility of dependencies at dependency resolution time.
     *
     * To avoid problems with different targets we advise to use [JDK Toolchain](https://kotl.in/gradle/jvm/toolchain) feature.
     *
     * Default value for builds with Gradle <8.0 is [JvmTargetValidationMode.WARNING],
     * while for builds with Gradle 8.0+ it is [JvmTargetValidationMode.ERROR].
     *
     * @since 1.9.0
     */
    @get:Input
    val jvmTargetValidationMode: Property<JvmTargetValidationMode>
}

interface KaptGenerateStubs : KotlinJvmCompile {
    @get:OutputDirectory
    val stubsDir: DirectoryProperty

    @get:Internal("Not an input, just passed as kapt args. ")
    val kaptClasspath: ConfigurableFileCollection

    @get:Deprecated(
        message = "Please migrate to compilerOptions.moduleName",
        replaceWith = ReplaceWith("compilerOptions.moduleName")
    )
    @get:Optional
    @get:Input
    override val moduleName: Property<String>
}

interface BaseKapt : Task {

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
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val source: ConfigurableFileCollection

    @get:Input
    val includeCompileClasspath: Property<Boolean>

    @get:Internal("Used to compute javac option.")
    val defaultJavaSourceCompatibility: Property<String>
}

interface Kapt : BaseKapt {

    @get:Input
    val addJdkClassesToClasspath: Property<Boolean>

    @get:Classpath
    val kaptJars: ConfigurableFileCollection
}