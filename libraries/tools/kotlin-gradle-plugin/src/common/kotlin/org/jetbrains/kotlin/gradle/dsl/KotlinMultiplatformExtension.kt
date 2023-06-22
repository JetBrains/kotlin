/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.hierarchy.KotlinHierarchyDslImpl
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import javax.inject.Inject

@Suppress("DEPRECATION")
@KotlinGradlePluginDsl
abstract class KotlinMultiplatformExtension
@InternalKotlinGradlePluginApi constructor(project: Project) :
    KotlinProjectExtension(project),
    KotlinTargetContainerWithPresetFunctions,
    KotlinTargetContainerWithJsPresetFunctions,
    KotlinTargetContainerWithWasmPresetFunctions,
    KotlinTargetContainerWithNativeShortcuts,
    KotlinHierarchyDsl,
    KotlinMultiplatformSourceSetConventions by KotlinMultiplatformSourceSetConventionsImpl {
    override val presets: NamedDomainObjectCollection<KotlinTargetPreset<*>> = project.container(KotlinTargetPreset::class.java)

    final override val targets: NamedDomainObjectCollection<KotlinTarget> = project.container(KotlinTarget::class.java)

    internal suspend fun awaitTargets(): NamedDomainObjectCollection<KotlinTarget> {
        AfterFinaliseDsl.await()
        return targets
    }

    override val compilerTypeFromProperties: KotlinJsCompilerType? = project.kotlinPropertiesProvider.jsCompiler

    private val presetExtension = project.objects.newInstance(
        DefaultTargetsFromPresetExtension::class.java,
        { this },
        targets
    )

    fun targets(configure: Action<TargetsFromPresetExtension>) {
        configure.execute(presetExtension)
    }

    fun targets(configure: TargetsFromPresetExtension.() -> Unit) {
        configure(presetExtension)
    }

    internal val hierarchy by lazy { KotlinHierarchyDslImpl(targets, sourceSets) }

    /**
     * Set's up a 'natural'/'default' hierarchy withing [KotlinTarget]'s in the project.
     *
     * #### Example
     *
     * ```kotlin
     * kotlin {
     *     applyDefaultHierarchyTemplate() // <- position of this call is not relevant!
     *
     *     iosX64()
     *     iosArm64()
     *     linuxX64()
     *     linuxArm64()
     * }
     * ```
     *
     * Will create the following SourceSets:
     * `[iosMain, iosTest, appleMain, appleTest, linuxMain, linuxTest, nativeMain, nativeTest]
     *
     *
     * Hierarchy:
     * ```
     *                                                                     common
     *                                                                        |
     *                                                      +-----------------+-------------------+
     *                                                      |                                     |
     *
     *                                                    native                                 ...
     *
     *                                                     |
     *                                                     |
     *                                                     |
     *         +----------------------+--------------------+-----------------------+
     *         |                      |                    |                       |
     *
     *       apple                  linux                mingw              androidNative
     *
     *         |
     *  +-----------+------------+------------+
     *  |           |            |            |
     *
     * macos       ios         tvos        watchos
     * ```
     *
     * @see KotlinHierarchyTemplate.extend
     */
    fun applyDefaultHierarchyTemplate() = applyHierarchyTemplate(KotlinHierarchyTemplate.default)

    /**
     * Will apply the given [template] to the project.
     *
     * ### Example: Manually apply the default hierarchy (see [applyDefaultHierarchyTemplate])
     * ```kotlin
     * kotlin {
     *     applyHierarchyTemplate(KotlinHierarchyTemplate.default)
     *     iosX64()
     *     iosArm64()
     *     iosSimulatorArm64()
     *     linuxX64()
     *     // ...
     * }
     * ```
     */
    @ExperimentalKotlinGradlePluginApi
    override fun applyHierarchyTemplate(template: KotlinHierarchyTemplate) {
        hierarchy.applyHierarchyTemplate(template)
    }

    /**
     * Similar to [applyDefaultHierarchyTemplate], but allows to extend the provided template
     *
     * #### Example 2: Adding custom groups (Experimental)
     * Let's imagine we would additionally like to share code between linux and apple (unixLike)
     *
     * ```kotlin
     * kotlin {
     *     applyHierarchyTemplate(KotlinHierarchyTemplate.default) {
     *         group("native") { // <- we can re-declare already existing groups and connect children to it!
     *             group("unixLike") {
     *                 withLinux()
     *                 withApple()
     *             }
     *         }
     *     }
     * }
     * ```
     */
    @ExperimentalKotlinGradlePluginApi
    override fun applyHierarchyTemplate(template: KotlinHierarchyTemplate, extension: KotlinHierarchyBuilder.Root.() -> Unit) {
        hierarchy.applyHierarchyTemplate(template, extension)
    }

    /**
     * Allows to create a fully custom hierarchy (no defaults applied)
     * Note: Using the custom hierarchy will also require to set the edges to 'commonMain' and 'commonTest' SourceSets by
     * using the `common` group.
     *
     * ####  Example 1:
     * Sharing code between iOS and a jvmTarget:
     * ```kotlin
     * applyHierarchyTemplate {
     *     common {
     *         withJvm()
     *         group("ios") {
     *             withIos()
     *         }
     *     }
     * }
     * ```
     *
     * Will create two [KotlinSourceSetTree] using the 'common' and 'ios' groups, applied on the "test" and "main" compilations:
     * When the following targets are specified:
     * - jvm()
     * - iosX64()
     * - iosArm64()
     * ```
     *                    "main"                               "test"
     *                  commonMain                           commonTest
     *                      |                                    |
     *                      |                                    |
     *           +----------+----------+              +----------+----------+
     *           |                     |              |                     |
     *         iosMain               jvmMain        iosTest               jvmTest
     *           |                                    |
     *      +----+-----+                         +----+-----+
     *      |          |                         |          |
     * iosX64Main   iosArm64Main            iosX64Test   iosArm64Test
     * ```
     *
     * #### Example 2: Creating a 'diamond structure'
     * ```kotlin
     * applyHierarchyTemplate {
     *     common {
     *         group("ios") {
     *             withIos()
     *         }
     *
     *         group("frontend") {
     *             withJvm()
     *             group("ios") // <- ! We can again reference the 'ios' group
     *         }
     *
     *         group("apple") {
     *             withMacos()
     *             group("ios") // <- ! We can again reference the 'ios' group
     *         }
     *     }
     * }
     * ```
     *
     * In this case, the _group_ "ios" can be created with 'group("ios")' and later referenced with the same construction to build
     * the tree. Applying the descriptor from the example to the following targets:
     * - iosX64()
     * - iosArm64()
     * - macosX64()
     * - jvm()
     *
     * will create the following 'main' KotlinSourceSetTree:
     *
     * ```
     *                      commonMain
     *                           |
     *              +------------+----------+
     *              |                       |
     *          frontendMain            appleMain
     *              |                        |
     *    +---------+------------+-----------+----------+
     *    |                      |                      |
     * jvmMain                iosMain               macosX64Main
     *                           |
     *                           |
     *                      +----+----+
     *                      |         |
     *                iosX64Main   iosArm64Main
     * ```
     */
    @ExperimentalKotlinGradlePluginApi
    override fun applyHierarchyTemplate(template: KotlinHierarchyBuilder.Root.() -> Unit) {
        hierarchy.applyHierarchyTemplate(template)
    }


    @Suppress("unused") // DSL
    val testableTargets: NamedDomainObjectCollection<KotlinTargetWithTests<*, *>>
        get() = targets.withType(KotlinTargetWithTests::class.java)

    fun metadata(configure: KotlinOnlyTarget<KotlinMetadataCompilation<*>>.() -> Unit = { }): KotlinOnlyTarget<KotlinMetadataCompilation<*>> =
        @Suppress("UNCHECKED_CAST")
        (targets.getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME) as KotlinOnlyTarget<KotlinMetadataCompilation<*>>).also(configure)

    fun metadata(configure: Action<KotlinOnlyTarget<KotlinMetadataCompilation<*>>>) = metadata { configure.execute(this) }

    fun withSourcesJar(publish: Boolean = true) {
        targets.all { it.withSourcesJar(publish) }
    }

    fun <T : KotlinTarget> targetFromPreset(
        preset: KotlinTargetPreset<T>,
        name: String = preset.name,
        configure: T.() -> Unit = { },
    ): T = configureOrCreate(name, preset, configure)

    fun <T : KotlinTarget> targetFromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configure: Action<T>,
    ) = targetFromPreset(preset, name) { configure.execute(this) }

    fun <T : KotlinTarget> targetFromPreset(preset: KotlinTargetPreset<T>) = targetFromPreset(preset, preset.name) { }
    fun <T : KotlinTarget> targetFromPreset(preset: KotlinTargetPreset<T>, name: String) = targetFromPreset(preset, name) { }
    fun <T : KotlinTarget> targetFromPreset(preset: KotlinTargetPreset<T>, configure: Action<T>) =
        targetFromPreset(preset, preset.name, configure)

    internal val rootSoftwareComponent: KotlinSoftwareComponent by lazy {
        KotlinSoftwareComponentWithCoordinatesAndPublication(project, "kotlin", targets)
    }
}

interface TargetsFromPresetExtension : NamedDomainObjectCollection<KotlinTarget> {

    fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configureAction: T.() -> Unit = {},
    ): T

    fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
    ): T = fromPreset(preset, name, {})

    fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configureAction: Action<T>,
    ): T
}

internal abstract class DefaultTargetsFromPresetExtension @Inject constructor(
    private val targetsContainer: () -> KotlinTargetsContainerWithPresets,
    val targets: NamedDomainObjectCollection<KotlinTarget>,
) : TargetsFromPresetExtension,
    NamedDomainObjectCollection<KotlinTarget> by targets {

    override fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configureAction: T.() -> Unit,
    ): T = targetsContainer().configureOrCreate(name, preset, configureAction)

    override fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configureAction: Action<T>,
    ) = fromPreset(preset, name) {
        configureAction.execute(this)
    }
}

internal fun KotlinTarget.isProducedFromPreset(kotlinTargetPreset: KotlinTargetPreset<*>): Boolean =
    preset == kotlinTargetPreset

internal fun <T : KotlinTarget> KotlinTargetsContainerWithPresets.configureOrCreate(
    targetName: String,
    targetPreset: KotlinTargetPreset<T>,
    configure: T.() -> Unit,
): T {
    val existingTarget = targets.findByName(targetName)
    when {
        existingTarget?.isProducedFromPreset(targetPreset) ?: false -> {
            @Suppress("UNCHECKED_CAST")
            configure(existingTarget as T)
            return existingTarget
        }

        existingTarget == null -> {
            val newTarget = targetPreset.createTarget(targetName)
            targets.add(newTarget)
            configure(newTarget)
            return newTarget
        }

        else -> {
            throw InvalidUserCodeException(
                "The target '$targetName' already exists, but it was not created with the '${targetPreset.name}' preset. " +
                        "To configure it, access it by name in `kotlin.targets`" +
                        (" or use the preset function '${existingTarget.preset?.name}'."
                            .takeIf { existingTarget.preset != null } ?: ".")
            )
        }
    }
}