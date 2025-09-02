/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KOTLIN_OPTIONS_DEPRECATION_MESSAGE
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.tooling.core.HasMutableExtras
import java.util.Locale.getDefault

/**
 * # Kotlin compilation
 * Represents the configuration of a Kotlin compiler invocation.
 * The [KotlinCompilation] API is designed to ensure the correct and consistent propagation of any compilation changes
 * to all underlying tasks and configurations.
 * Use the [KotlinCompilation] API instead of getting tasks, configurations,
 * and other related domain objects directly through the Gradle API.
 * For Native targets, [KotlinCompilation] also provides an API to configure cinterop.
 *
 * A [KotlinTarget] contains multiple [KotlinCompilations](KotlinCompilation).
 * By default, a [KotlinTarget] contains two [KotlinCompilations](KotlinCompilation) with the names "main" and "test".
 *
 * Here's an example of how to use a [KotlinCompilation] to configure compilation tasks for the JVM target:
 * ```kotlin
 * // build.gradle.kts
 * kotlin {
 *   jvm {
 *     compilations.all {
 *       compileTaskProvider {
 *         // Configure the compile task here
 *       }
 *     }
 *   }
 * }
 * ```
 *
 * ## Main compilation
 * The [KotlinCompilation] with the name "main" represents a Kotlin compiler invocation for the main sources of the [KotlinTarget].
 * The results of the main compilation are publishable
 * and exposed via the [KotlinTarget.apiElementsConfigurationName] consumable configuration.
 *
 * Here's an example of how to use the outputs of the main JVM Compilation for custom processing:
 *
 * ```kotlin
 * // build.gradle.kts
 * val jvmMainClasses = kotlin.jvm().compilations.getByName("main").output.classesDirs
 * tasks.register<Jar>("customJar") {
 *     from(jvmMainClasses)
 * }
 * ```
 *
 * ## Test compilation
 * The [KotlinCompilation] with the name "test" represents a Kotlin Compiler invocation for the test source sets.
 * The test compilation is implicitly [associated with](KotlinCompilation.associateWith) the main compilation.
 * See [KotlinCompilation.associatedCompilations] for more information.
 * This means that the test compilation sees all dependencies, internal and public declarations of the main compilation.
 *
 * ## Custom compilation
 * It is possible to create additional custom compilations for a [KotlinTarget]:
 *
 * ```kotlin
 * kotlin {
 *   jvm {
 *     compilations.create("customCompilation")
 *   }
 * }
 * ```
 *
 * Use a separate Gradle project instead of creating a custom compilation for an easier and safer setup.
 *
 * ## Metadata target compilation
 *
 * The Kotlin metadata target is a special [KotlinTarget] that manages compiler invocations that compile the code of shared source sets.
 * There are no "main" or "test" Kotlin Compilations for the Metadata Target.
 * Instead, there is a dedicated compilation for each shared source set.
 *
 * ## Future changes
 *
 * In future releases, the [KotlinCompilation] interface will no longer have a parameter of generic type.
 * To avoid compile-time errors after updating to the latest version of the Kotlin Gradle plugin,
 * use the [KotlinCompilationCompat] typealias.
 */
@KotlinGradlePluginDsl
interface KotlinCompilation<UNUSED : KotlinAnyOptionsDeprecated> : Named,
    HasProject,
    HasMutableExtras,
    HasAttributes,
    HasKotlinDependencies {

    /**
     * Represents a [KotlinTarget] which this compilation belongs to.
     */
    val target: KotlinTarget

    /**
     * The name of the compilation.
     */
    val compilationName: String

    /**
     * Directly added [KotlinSourceSets](KotlinSourceSet) that are used by this compilation. I.e., doesn't contain shared source sets.
     */
    val kotlinSourceSets: Set<KotlinSourceSet>

    /**
     * All [KotlinSourceSets](KotlinSourceSet) used by this compilation.
     */
    val allKotlinSourceSets: Set<KotlinSourceSet>

    /**
     * @suppress
     */
    @Deprecated(
        "Use defaultSourceSet.name instead. Scheduled for removal in Kotlin 2.3",
        ReplaceWith("defaultSourceSet.name"),
        level = DeprecationLevel.ERROR
    )
    val defaultSourceSetName: String get() = defaultSourceSet.name

    /**
     * The [KotlinSourceSet] by default associated with this compilation.
     */
    val defaultSourceSet: KotlinSourceSet

    /**
     * Configures the provided [defaultSourceSet].
     */
    fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit)

    /**
     * Configures the provided [defaultSourceSet].
     */
    fun defaultSourceSet(configure: Action<KotlinSourceSet>) = defaultSourceSet { configure.execute(this) }

    /**
     * The name of the Gradle configuration containing all the resolved dependencies required for compilation.
     *
     * A possible name for the configuration is the 'compileClasspath' for the Kotlin JVM target.
     */
    val compileDependencyConfigurationName: String

    /**
     * A collection of file system locations for the artifacts of compilation dependencies.
     */
    var compileDependencyFiles: FileCollection

    /**
     * The name of the Gradle configuration containing all the resolved dependencies required to run compilation output.
     *
     * A possible name for the configuration is the 'runtimeClasspath' for the Kotlin JVM target.
     */
    val runtimeDependencyConfigurationName: String?

    /**
     * A collection of file system locations for the artifacts of runtime dependencies.
     */
    val runtimeDependencyFiles: FileCollection?

    /**
     * Represents the output of a Kotlin compilation.
     */
    val output: KotlinCompilationOutput

    /**
     * Represents the [KotlinPlatformType] to which this compilation belongs.
     *
     * It is always the same as the [target].
     */
    val platformType get() = target.platformType

    /**
     * The Kotlin Gradle plugin task name that is used to run the compilation process for this Kotlin compilation.
     */
    val compileKotlinTaskName: String

    /**
     * Provides access to the compilation task for this compilation.
     */
    val compileTaskProvider: TaskProvider<out KotlinCompilationTask<*>>

    /**
     * @suppress
     */
    @Deprecated(
        "To configure compilation compiler options use 'compileTaskProvider':\ncompilation.compileTaskProvider.configure{\n" +
                "    compilerOptions {}\n}"
    )
    @Suppress("DEPRECATION_ERROR")
    val compilerOptions: HasCompilerOptions<*>

    /**
     * @suppress
     */
    @Suppress("DEPRECATION", "TYPEALIAS_EXPANSION_DEPRECATION_ERROR")
    @Deprecated(
        message = "Accessing task instance directly is deprecated. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("compileTaskProvider"),
        level = DeprecationLevel.ERROR,
    )
    val compileKotlinTask: KotlinCompileDeprecated<KotlinCommonOptionsDeprecated>

    /**
     * @suppress
     */
    @Suppress("DEPRECATION", "TYPEALIAS_EXPANSION_DEPRECATION_ERROR")
    @Deprecated(
        message = "Replaced with compileTaskProvider. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("compileTaskProvider"),
        level = DeprecationLevel.ERROR,
    )
    val compileKotlinTaskProvider: TaskProvider<out KotlinCompileDeprecated<KotlinCommonOptionsDeprecated>>

    /**
     * @suppress
     */
    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION_ERROR")
    @OptIn(InternalKotlinGradlePluginApi::class)
    @Deprecated(
        message = KOTLIN_OPTIONS_DEPRECATION_MESSAGE,
        level = DeprecationLevel.ERROR,
    )
    val kotlinOptions: KotlinCommonOptionsDeprecated

    /**
     * @suppress
     */
    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION_ERROR")
    @OptIn(InternalKotlinGradlePluginApi::class)
    @Deprecated(
        message = KOTLIN_OPTIONS_DEPRECATION_MESSAGE,
        level = DeprecationLevel.ERROR,
    )
    fun kotlinOptions(configure: KotlinCommonOptionsDeprecated.() -> Unit) {
        @Suppress("DEPRECATION_ERROR")
        configure(kotlinOptions)
    }

    /**
     * @suppress
     */
    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION_ERROR")
    @OptIn(InternalKotlinGradlePluginApi::class)
    @Deprecated(
        message = KOTLIN_OPTIONS_DEPRECATION_MESSAGE,
        level = DeprecationLevel.ERROR,
    )
    fun kotlinOptions(configure: Action<KotlinCommonOptionsDeprecated>) {
        @Suppress("DEPRECATION_ERROR")
        configure.execute(kotlinOptions)
    }

    /**
     * Configures the compilation [AttributeContainer] with the provided configuration.
     */
    fun attributes(configure: AttributeContainer.() -> Unit) = attributes.configure()

    /**
     * Configures the compilation [AttributeContainer] with the provided configuration.
     */
    fun attributes(configure: Action<AttributeContainer>) = attributes { configure.execute(this) }

    /**
     * The Gradle task name that is used as a meta task to trigger all the required compilation tasks for this [KotlinCompilation].
     *
     * For example, in a JVM-only project, the "classes" task depends on the "compileKotlin" and "compileJava" tasks.
     */
    val compileAllTaskName: String

    /**
     * Constants for the [KotlinCompilation].
     */
    companion object {
        /**
         * The default main compilation name.
         */
        const val MAIN_COMPILATION_NAME = "main"

        /**
         * The default test compilation name.
         */
        const val TEST_COMPILATION_NAME = "test"
    }

    /**
     * Associates the current KotlinCompilation with another KotlinCompilation.
     *
     * After this compilation will:
     * - use the output of the [other] compilation as compile & runtime dependency
     * - add all 'declared dependencies' present on [other] compilation
     * - see all internal declarations of [other] compilation
     */
    fun associateWith(other: KotlinCompilation<*>)

    /**
     * @suppress
     */
    @Deprecated(
        "Used in IDEA Import",
        level = DeprecationLevel.HIDDEN
    )
    val associateWith: List<KotlinCompilation<*>> get() = associatedCompilations.toList()

    /**
     * A list of all compilations that were previously associated with this compilation using [associateWith].
     *
     * For exmaple, 'test' compilations return 'setOf(main)' by default.
     *
     * @since 1.9.20
     */
    val associatedCompilations: Set<KotlinCompilation<*>>

    /**
     * A full transitive closure of [associatedCompilations].
     *
     * @since 1.9.20
     */
    @ExperimentalKotlinGradlePluginApi
    val allAssociatedCompilations: Set<KotlinCompilation<*>>

    /**
     * The object's name.
     *
     * The name must be constant for the life of the object.
     */
    override fun getName(): String = compilationName

    /**
     * A unique name for this compilation in the project.
     *
     * [KotlinTargets](KotlinTarget) may have [KotlinCompilations](KotlinCompilation) which have the same [compilationName] in different targets. For example, [MAIN_COMPILATION_NAME].
     * This property provides a unique name for the compilation based on the target name, allowing for easy distinction among them throughout the project.
     */
    val disambiguatedName
        get() = target.disambiguationClassifier + name.replaceFirstChar { it.titlecase(getDefault()) }

    @Deprecated(
        "Declaring dependencies on Compilation level is deprecated, please declare on related source set",
        ReplaceWith("defaultSourceSet.dependencies"),
        level = DeprecationLevel.WARNING
    )
    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit)

    @Deprecated(
        "Declaring dependencies on Compilation level is deprecated, please declare on related source set",
        ReplaceWith("defaultSourceSet.dependencies"),
        level = DeprecationLevel.WARNING
    )
    override fun dependencies(configure: Action<KotlinDependencyHandler>)

    @Deprecated(
        "Accessing apiConfigurationName on Compilation level is deprecated, please use default source set instead",
        ReplaceWith("defaultSourceSet.apiConfigurationName"),
        level = DeprecationLevel.WARNING
    )
    override val apiConfigurationName: String

    @Deprecated(
        "Accessing implementationConfigurationName on Compilation level is deprecated, please use default source set instead",
        ReplaceWith("defaultSourceSet.implementationConfigurationName"),
        level = DeprecationLevel.WARNING
    )
    override val implementationConfigurationName: String

    @Deprecated(
        "Accessing compileOnlyConfigurationName on Compilation level is deprecated, please use default source set instead",
        ReplaceWith("defaultSourceSet.compileOnlyConfigurationName"),
        level = DeprecationLevel.WARNING
    )
    override val compileOnlyConfigurationName: String

    @Deprecated(
        "Accessing runtimeOnlyConfigurationName on Compilation level is deprecated, please use default source set instead",
        ReplaceWith("defaultSourceSet.runtimeOnlyConfigurationName"),
        level = DeprecationLevel.WARNING
    )
    override val runtimeOnlyConfigurationName: String
}

/**
 * @suppress
 */
@Deprecated("Scheduled for removal with Kotlin 2.3.", level = DeprecationLevel.ERROR)
interface KotlinCompilationToRunnableFiles<T : KotlinAnyOptionsDeprecated> : KotlinCompilation<T> {
    override val runtimeDependencyConfigurationName: String

    override var runtimeDependencyFiles: FileCollection
}

/**
 * @suppress
 */
@Suppress("Deprecation_ERROR")
typealias DeprecatedKotlinCompilationToRunnableFiles<T> = KotlinCompilationToRunnableFiles<T>
/**
 * @suppress
 */
@Deprecated("Scheduled for removal after providing a replacement: KT-56644")
interface KotlinCompilationWithResources<T : KotlinAnyOptionsDeprecated> : KotlinCompilation<T> {
    val processResourcesTaskName: String
}

/**
 * @suppress
 */
@Suppress("Deprecation")
typealias DeprecatedKotlinCompilationWithResources<T> = KotlinCompilationWithResources<T>

/**
 * Provides compile-time compatibility with changes in the [KotlinCompilation] interface.
 */
typealias KotlinCompilationCompat = KotlinCompilation<KotlinAnyOptionsDeprecated>

/**
 * Provides compile-time compatibility with the `kotlinOptions` DSL deprecation.
 */
typealias KotlinAnyOptionsDeprecated = Any