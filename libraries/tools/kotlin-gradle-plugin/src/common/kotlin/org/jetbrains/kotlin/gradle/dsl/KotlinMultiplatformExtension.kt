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
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
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
    @Deprecated(
        presetsApiIsDeprecatedMessage,
        level = DeprecationLevel.WARNING,
    )
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
        targets,
        project,
    )

    fun targets(configure: Action<TargetsFromPresetExtension>) {
        configure.execute(presetExtension)
    }

    fun targets(configure: TargetsFromPresetExtension.() -> Unit) {
        configure(presetExtension)
    }

    internal val hierarchy by lazy { KotlinHierarchyDslImpl(targets, sourceSets) }

    /**
     * Sets up a 'natural'/'default' hierarchy withing [KotlinTarget]'s in the project.
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


    @ExperimentalKotlinGradlePluginApi
    override fun applyHierarchyTemplate(template: KotlinHierarchyTemplate) {
        hierarchy.applyHierarchyTemplate(template)
    }

    @ExperimentalKotlinGradlePluginApi
    override fun applyHierarchyTemplate(template: KotlinHierarchyTemplate, extension: KotlinHierarchyBuilder.Root.() -> Unit) {
        hierarchy.applyHierarchyTemplate(template, extension)
    }

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

    internal fun <T : KotlinTarget> targetFromPresetInternal(
        preset: KotlinTargetPreset<T>,
        name: String = preset.name,
        configure: T.() -> Unit = { },
    ): T = configureOrCreate(name, preset, configure)

    internal fun <T : KotlinTarget> targetFromPresetInternal(
        preset: KotlinTargetPreset<T>,
        name: String,
        configure: Action<T>,
    ) = targetFromPresetInternal(preset, name) { configure.execute(this) }

    internal fun <T : KotlinTarget> targetFromPresetInternal(preset: KotlinTargetPreset<T>) =
        targetFromPresetInternal(preset, preset.name) { }

    internal fun <T : KotlinTarget> targetFromPresetInternal(preset: KotlinTargetPreset<T>, name: String) =
        targetFromPresetInternal(preset, name) { }

    internal fun <T : KotlinTarget> targetFromPresetInternal(preset: KotlinTargetPreset<T>, configure: Action<T>) =
        targetFromPresetInternal(preset, preset.name, configure)

    private fun warnAboutTargetFromPresetDeprecation() {
        project.reportDiagnostic(
            KotlinToolingDiagnostics.TargetPresets(
                KotlinToolingDiagnostics.TargetPresets.API.TargetFromPreset
            )
        )
    }

    @TargetPresetsDeprecation
    @Deprecated(
        KotlinToolingDiagnostics.TargetPresets.targetFromPresetDeprecationMessage,
        level = DeprecationLevel.WARNING
    )
    fun <T : KotlinTarget> targetFromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configure: Action<T>,
    ) {
        warnAboutTargetFromPresetDeprecation()
        targetFromPresetInternal(
            preset = preset,
            name = name,
            configure = configure,
        )
    }

    @TargetPresetsDeprecation
    @Deprecated(
        KotlinToolingDiagnostics.TargetPresets.targetFromPresetDeprecationMessage,
        level = DeprecationLevel.WARNING
    )
    fun <T : KotlinTarget> targetFromPreset(preset: KotlinTargetPreset<T>) {
        warnAboutTargetFromPresetDeprecation()
        targetFromPresetInternal(
            preset = preset
        )
    }

    @TargetPresetsDeprecation
    @Deprecated(
        KotlinToolingDiagnostics.TargetPresets.targetFromPresetDeprecationMessage,
        level = DeprecationLevel.WARNING
    )
    fun <T : KotlinTarget> targetFromPreset(preset: KotlinTargetPreset<T>, name: String) {
        warnAboutTargetFromPresetDeprecation()
        targetFromPresetInternal(
            preset = preset,
            name = name
        )
    }

    @TargetPresetsDeprecation
    @Deprecated(
        KotlinToolingDiagnostics.TargetPresets.targetFromPresetDeprecationMessage,
        level = DeprecationLevel.WARNING
    )
    fun <T : KotlinTarget> targetFromPreset(preset: KotlinTargetPreset<T>, configure: Action<T>) {
        warnAboutTargetFromPresetDeprecation()
        targetFromPresetInternal(
            preset = preset,
            configure = configure
        )
    }

    internal val rootSoftwareComponent: KotlinSoftwareComponent by lazy {
        KotlinSoftwareComponentWithCoordinatesAndPublication(project, "kotlin", targets)
    }
}

@TargetPresetsDeprecation
interface TargetsFromPresetExtension : NamedDomainObjectCollection<KotlinTarget> {

    @Deprecated(
        KotlinToolingDiagnostics.TargetPresets.fromPresetDeprecationMessage,
        level = DeprecationLevel.WARNING,
    )
    fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configureAction: T.() -> Unit = {},
    ): T

    @Deprecated(
        KotlinToolingDiagnostics.TargetPresets.fromPresetDeprecationMessage,
        level = DeprecationLevel.WARNING,
    )
    fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
    ): T = fromPreset(preset, name, {})

    @Deprecated(
        KotlinToolingDiagnostics.TargetPresets.fromPresetDeprecationMessage,
        level = DeprecationLevel.WARNING,
    )
    fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configureAction: Action<T>,
    ): T
}

internal abstract class DefaultTargetsFromPresetExtension @Inject constructor(
    private val targetsContainer: () -> KotlinTargetsContainerWithPresets,
    val targets: NamedDomainObjectCollection<KotlinTarget>,
    val project: Project,
) : TargetsFromPresetExtension,
    NamedDomainObjectCollection<KotlinTarget> by targets {

    private fun warnAboutFromPresetDeprecation() {
        project.reportDiagnostic(
            KotlinToolingDiagnostics.TargetPresets(
                KotlinToolingDiagnostics.TargetPresets.API.FromPreset
            )
        )
    }

    override fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configureAction: T.() -> Unit,
    ): T {
        warnAboutFromPresetDeprecation()
        return targetsContainer().configureOrCreate(name, preset, configureAction)
    }

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
            val newTarget = targetPreset.createTargetInternal(targetName)
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