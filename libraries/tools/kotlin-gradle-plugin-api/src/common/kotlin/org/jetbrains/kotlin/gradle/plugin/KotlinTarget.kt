/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl
import org.jetbrains.kotlin.tooling.core.HasMutableExtras

/**
 * Represents a target platform for which Kotlin code is built.
 *
 * This abstraction allows for the configuration of tasks, dependencies, and other settings specific to the platform on which the code is intended to run.
 *
 * By default, a Kotlin target contains two [KotlinCompilations][KotlinCompilation]: one for [production][KotlinCompilation.MAIN_COMPILATION_NAME]
 * and one for [test][KotlinCompilation.TEST_COMPILATION_NAME] source code.
 *
 * Examples of accessing the Kotlin target:
 *
 * - In Kotlin/JVM or Kotlin/Android projects:
 * ```
 * kotlin {
 *     target {
 *         // Configure JVM or Android target specifics here
 *     }
 * }
 * ```
 *
 * - In Kotlin Multiplatform projects:
 * ```
 * kotlin {
 *     jvm { // Creates JVM target
 *         // Configure JVM target specifics here
 *     }
 *
 *     linuxX64 {
 *         // Configure Kotlin native target for Linux X86_64 here
 *     }
 * }
 * ```
 *
 * To learn more about the different targets in Kotlin, see [Targets](https://kotlinlang.org/docs/multiplatform-discover-project.html#targets).
 */
@KotlinGradlePluginDsl
interface KotlinTarget : Named, HasAttributes, HasProject, HasMutableExtras {

    /**
     * The name of the target in the Kotlin build configuration.
     */
    val targetName: String

    /**
     * Retrieves the disambiguation classifier for the Kotlin target.
     *
     * The disambiguation classifier can be used to distinguish between multiple Kotlin targets within the same project.
     * It is often applied as a prefix or suffix to generated names to avoid naming conflicts.
     */
    val disambiguationClassifier: String? get() = targetName

    /**
     * Represents the type of Kotlin platform associated with the target.
     */
    val platformType: KotlinPlatformType

    /**
     * A container for [Kotlin compilations][KotlinCompilation] related to this target.
     *
     * Allows access to the default [main][KotlinCompilation.MAIN_COMPILATION_NAME] or [test][KotlinCompilation.TEST_COMPILATION_NAME]
     * compilations, or the creation of additional compilations.
     */
    val compilations: NamedDomainObjectContainer<out KotlinCompilation<out Any>>

    /**
     * The name of the task responsible for assembling the final artifact for this target.
     */
    val artifactsTaskName: String

    /**
     * The name of the configuration that is used when compiling against the API of this Kotlin target.
     *
     * This configuration is intended to be consumed by other components when they need to compile against it.
     */
    val apiElementsConfigurationName: String

    /**
     * The name of the configuration containing elements that are strictly required at runtime by this Kotlin target.
     *
     * Consumers of this configuration receive all the necessary elements for this component to execute at runtime.
     */
    val runtimeElementsConfigurationName: String

    /**
     * The name of the configuration that represents the variant that carries the original source code in packaged form.
     *
     * This is typically only needed for publishing.
     */
    val sourcesElementsConfigurationName: String

    /**
     * Indicates whether the Kotlin target is publishable.
     *
     * For example, the target could have a value of `false` if it's not possible to compile for the target platform on the current host.
     */
    val publishable: Boolean

    /**
     * Configures the publication of sources.
     *
     * @param publish Indicates whether the sources JAR is to be published. Defaults to `true`.
     */
    fun withSourcesJar(publish: Boolean = true)

    /**
     * Represents a collection of Gradle [software components][SoftwareComponent] associated with this Kotlin target.
     *
     * **Note**: Returned [SoftwareComponent] potentially could be in not fully configured state (for example without some usages).
     * Call this function during the Gradle execution phase to retrieve [SoftwareComponent] in a fully configured state.
     */
    val components: Set<SoftwareComponent>

    /**
     * Configures the [Maven publication][MavenPublication] for this Kotlin target.
     */
    fun mavenPublication(action: MavenPublication.() -> Unit) = mavenPublication(Action { action(it) })

    /**
     * Configures the [Maven publication][MavenPublication] for this Kotlin target.
     */
    fun mavenPublication(action: Action<MavenPublication>)

    /**
     * Configures the attributes associated with this target.
     */
    fun attributes(configure: AttributeContainer.() -> Unit) = attributes.configure()

    /**
     * Configures the attributes associated with this target.
     */
    fun attributes(configure: Action<AttributeContainer>) = attributes { configure.execute(this) }

    // Workaround for https://youtrack.jetbrains.com/issue/CMP-7891/Compose-Gradle-plugin-is-using-deprecated-Presets-API
    /**
     * @suppress
     */
    @Suppress("DEPRECATION_ERROR")
    @get:Deprecated(
        "Not supported",
        level = DeprecationLevel.HIDDEN
    )
    val preset: KotlinTargetPreset<out KotlinTarget>? get() = null

    /**
     * @suppress
     */
    override fun getName(): String = targetName

    /**
     * @suppress
     */
    @Deprecated(
        "Accessing 'sourceSets' container on the Kotlin target level DSL is deprecated. " +
                "Consider configuring 'sourceSets' on the Kotlin extension level. This API is scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR
    )
    val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
}

/**
 * Represents a [KotlinTarget] that includes test runs.
 */
interface KotlinTargetWithTests<E : KotlinExecution.ExecutionSource, T : KotlinTargetTestRun<E>> : KotlinTarget {

    /**
     * The container that holds test run executions.
     *
     * A test run by the name [DEFAULT_TEST_RUN_NAME] is automatically created and configured.
     */
    val testRuns: NamedDomainObjectContainer<T>

    /**
     * [KotlinTargetWithTests] constants.
     */
    companion object {

        /**
         * The name of the default [KotlinTargetTestRun] created by [KotlinTargetWithTests].
         */
        const val DEFAULT_TEST_RUN_NAME = "test"
    }
}

// Workaround for https://youtrack.jetbrains.com/issue/CMP-7891/Compose-Gradle-plugin-is-using-deprecated-Presets-API
@Deprecated(
    "Not supported",
    level = DeprecationLevel.HIDDEN
)
interface KotlinTargetPreset<T : KotlinTarget>
