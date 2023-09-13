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

/**
 * Represents a Kotlin task participating in some stage of the build by compiling sources or running additional Kotlin tools.
 */
interface KotlinCompileTool : PatternFilterable, Task {

    /**
     * The configured task inputs (for example, Kotlin sources) which are used to produce a task artifact.
     */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sources: FileCollection

    /**
     * Adds input sources for this task.
     *
     * @param sources object is evaluated as per [org.gradle.api.Project.files].
     */
    fun source(vararg sources: Any)

    /**
     * Sets input sources for this task.
     *
     * **Note**: due to [a bug](https://youtrack.jetbrains.com/issue/KT-59632/KotlinCompileTool.setSource-should-replace-existing-sources),
     * the `setSource()` function does not update already added sources.
     *
     * @param sources object is evaluated as per [org.gradle.api.Project.files].
     */
    fun setSource(vararg sources: Any)

    /**
     * Collection of external artifacts participating in the output artifact generation.
     *
     * For example, a Kotlin/JVM compilation task has external JAR files or an
     * external location with already compiled class files.
     */
    @get:Classpath
    @get:Incremental
    val libraries: ConfigurableFileCollection

    /**
     * The destination directory where the task artifact can be found.
     */
    @get:OutputDirectory
    val destinationDirectory: DirectoryProperty

    /**
     * Returns the set of exclude patterns.
     *
     * @return The exclude patterns. Returns an empty set when there are no exclude patterns.
     */
    @Internal
    override fun getExcludes(): MutableSet<String>

    /**
     * Returns the set of include patterns.
     *
     * @return The include patterns. Returns an empty set when there are no include patterns.
     */
    @Internal
    override fun getIncludes(): MutableSet<String>
}

/**
 * Represents any Kotlin compilation task including common task inputs.
 */
interface BaseKotlinCompile : KotlinCompileTool {

    /**
     * Paths to the output directories of the friend modules whose internal declarations should be visible.
     */
    @get:Internal
    val friendPaths: ConfigurableFileCollection

    /**
     * Kotlin compiler plugins artifacts
     * , such as JAR or class files, that participate in the compilation process. All files that are permitted in the [JVM classpath](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/classpath.html) are permitted here.
     */
    @get:Classpath
    val pluginClasspath: ConfigurableFileCollection

    /**
     * The configuration for the Kotlin compiler plugin added in [pluginClasspath] using [CompilerPluginConfig].
     */
    @get:Nested
    val pluginOptions: ListProperty<CompilerPluginConfig>

    // Exists only to be used in 'KotlinCompileCommon' task.
    // Should be removed once 'moduleName' will be moved into CommonCompilerArguments
    /**
     * @suppress
     */
    @get:Input
    val moduleName: Property<String>

    /**
     * Specifies the name of [org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet] that is compiled.
     */
    @get:Internal
    val sourceSetName: Property<String>

    /**
     * Enables the [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html) flag for compilation.
     */
    @get:Input
    val multiPlatformEnabled: Property<Boolean>

    /**
     * Enable more granular tracking of inter-modules as part of incremental compilation. Useful in Android projects.
     */
    @get:Input
    val useModuleDetection: Property<Boolean>
}

/**
 * Represents a Kotlin task compiling given Kotlin sources into JVM class files.
 */
interface KotlinJvmCompile : BaseKotlinCompile,
    KotlinCompileDeprecated<KotlinJvmOptionsDeprecated>,
    KotlinCompilationTask<KotlinJvmCompilerOptions>,
    UsesKotlinJavaToolchain {

    /**
     * @suppress
     */
    @get:Deprecated(
        message = "Please migrate to compilerOptions.moduleName",
        replaceWith = ReplaceWith("compilerOptions.moduleName")
    )
    @get:Optional
    @get:Input
    override val moduleName: Property<String>

    /**
     * @suppress
     */
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
     * Using the same JVM targets ensures that the produced JAR file contains class files of the same JVM bytecode version,
     * which is important to avoid compatibility issues for users of your code.
     *
     * The Gradle Java compilation task [org.gradle.api.tasks.compile.JavaCompile.targetCompatibility] controls the value
     * of the `org.gradle.jvm.version` [attribute](https://docs.gradle.org/current/javadoc/org/gradle/api/attributes/java/TargetJvmVersion.html)
     * which itself controls the produced artifact's minimum supported JVM version via
     * [Gradle Module Metadata](https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html).
     * This allows Gradle to check the compatibility of dependencies at dependency resolution time.
     *
     * To avoid problems with different targets, we advise using the [JVM Toolchain](https://kotl.in/gradle/jvm/toolchain) feature.
     *
     * The default value for builds with Gradle <8.0 is [JvmTargetValidationMode.WARNING],
     * while for builds with Gradle 8.0+ it is [JvmTargetValidationMode.ERROR].
     *
     * @since 1.9.0
     */
    @get:Input
    val jvmTargetValidationMode: Property<JvmTargetValidationMode>
}

/**
 * Represents a Kotlin task that generates stubs from Java annotation processing results.
 *
 * This task generates annotation processing output stubs (without the actual method implementations)
 * using Java source code.
 * These generated stubs can be referenced in Kotlin source code compilation before completing
 * annotation processing.
 *
 * This task is a part of [Kotlin/Kapt](https://kotlinlang.org/docs/kapt.html).
 */
interface KaptGenerateStubs : KotlinJvmCompile {
    /**
     * The directory where generated stubs can be found.
     */
    @get:OutputDirectory
    val stubsDir: DirectoryProperty

    /**
     * Allows adding artifacts (accepted by [JVM classpath](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/classpath.html))
     * containing implementation of Java [annotation processor](https://jcp.org/en/jsr/detail?id=269).
     *
     * Configure this property with the same artifacts as its related [Kapt] task.
     */
    @get:Internal("Not an input, just passed as kapt args. ")
    val kaptClasspath: ConfigurableFileCollection

    /**
     * @suppress
     */
    @get:Deprecated(
        message = "Please migrate to compilerOptions.moduleName",
        replaceWith = ReplaceWith("compilerOptions.moduleName")
    )
    @get:Optional
    @get:Input
    override val moduleName: Property<String>
}

/**
 * Represents a Kotlin task that runs annotation processing using [Kotlin/Kapt](https://kotlinlang.org/docs/kapt.html).
 *
 * **Note:** Always run this task after its related [KaptGenerateStubs] and [KotlinJvmCompile] tasks.
 */
interface BaseKapt : Task,
    UsesKotlinJavaToolchain {

    // part of kaptClasspath consisting from external artifacts only
    // basically kaptClasspath = kaptExternalClasspath + artifacts built locally
    // TODO (Yahor): should not be a part of public api
    /**
     * @suppress
     */
    @get:Classpath
    val kaptExternalClasspath: ConfigurableFileCollection

    /**
     * The names of Gradle's [org.gradle.api.artifacts.Configuration] that contains all the annotation processor artifacts
     * used to configure [kaptClasspath].
     */
    @get:Internal
    val kaptClasspathConfigurationNames: ListProperty<String>

    /**
     * The output directory containing the caches necessary to support incremental annotation processing.
     */
    @get:LocalState
    val incAptCache: DirectoryProperty

    /**
     * The directory where class files generated by annotation processing can be found.
     */
    @get:OutputDirectory
    val classesDir: DirectoryProperty

    /**
     * The directory where Java source files generated by annotation processing can be found.
     */
    @get:OutputDirectory
    val destinationDir: DirectoryProperty

    // Used in the model builder only
    /**
     * The directory where Java source files generated by annotation processing can be found.
     */
    @get:OutputDirectory
    val kotlinSourcesDestinationDir: DirectoryProperty

    /**
     * Represents a list of annotation processor option providers.
     *
     * Accepts a [List] of [org.gradle.process.CommandLineArgumentProvider]s.
     */
    @get:Nested
    val annotationProcessorOptionProviders: MutableList<Any>

    /**
     * The directory where the generated related [KaptGenerateStubs] task stub can be found.
     */
    @get:Internal
    val stubsDir: DirectoryProperty

    /**
     * Allows adding artifacts (usually JAR files)
     * that contain the implementation of the Java [annotation processor](https://jcp.org/en/jsr/detail?id=269).
     *
     * Should be configured with the same artifacts as in the related [KaptGenerateStubs] task.
     */
    @get:Classpath
    val kaptClasspath: ConfigurableFileCollection

    /**
     * The directory that contains the compiled related [KotlinJvmCompile] task classes.
     */
    @get:Internal
    val compiledSources: ConfigurableFileCollection

    /**
     * Contains all artifacts from the related [KotlinJvmCompile.libraries] task input.
     */
    @get:Internal("Task implementation adds correct input annotation.")
    val classpath: ConfigurableFileCollection

    // Needed for the model builder
    /**
     * Specifies the name of [org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet] for which task is
     * doing annotation processing.
     */
    @get:Internal
    val sourceSetName: Property<String>

    /**
     * Contains all Java source code used in this compilation
     * and generated by related [KaptGenerateStubs] task stubs.
     */
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val source: ConfigurableFileCollection

    /**
     * Enable searching for annotation processors in the [classpath].
     */
    @get:Input
    val includeCompileClasspath: Property<Boolean>

    /**
     * Java source compatibility in the form of the Java language level for the produced class files and Java source code.
     *
     * Check the `javac` `-source` command line option
     * [description](https://docs.oracle.com/javase/7/docs/technotes/tools/windows/javac.html#options)
     * for the possible values for the Java language level.
     *
     * @see [org.gradle.api.tasks.compile.AbstractCompile.setSourceCompatibility]
     */
    @get:Internal("Used to compute javac option.")
    val defaultJavaSourceCompatibility: Property<String>
}

/**
 * Represents a [BaseKapt] task whose implementation is running [Kotlin/Kapt](https://kotlinlang.org/docs/kapt.html)
 * directly (without using the Kotlin compiler).
 */
interface Kapt : BaseKapt {

    /**
     * Add JDK classes to the [BaseKapt.classpath].
     *
     * For example, in Android projects this should be disabled.
     */
    @get:Input
    val addJdkClassesToClasspath: Property<Boolean>

    /**
     * The file collection that contains `org.jetbrains.kotlin:kotlin-annotation-processing-gradle` and `kotlin-stdlib`
     * artifacts that are used to run annotation processing.
     *
     * The artifacts' versions must be the same as the version of the Kotlin compiler used to compile the related Kotlin sources.
     */
    @get:Classpath
    val kaptJars: ConfigurableFileCollection
}