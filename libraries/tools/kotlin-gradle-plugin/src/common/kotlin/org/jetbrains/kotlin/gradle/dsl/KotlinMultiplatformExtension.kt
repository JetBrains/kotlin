/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy.KotlinTargetHierarchyDslImpl
import javax.inject.Inject

abstract class KotlinMultiplatformExtension(project: Project) :
    KotlinProjectExtension(project),
    KotlinTargetContainerWithPresetFunctions,
    KotlinTargetContainerWithJsPresetFunctions,
    KotlinTargetContainerWithWasmPresetFunctions,
    KotlinTargetContainerWithNativeShortcuts {
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

    internal val internalKotlinTargetHierarchy by lazy {
        KotlinTargetHierarchyDslImpl(targets, sourceSets)
    }

    @ExperimentalKotlinGradlePluginApi
    val targetHierarchy: KotlinTargetHierarchyDsl get() = internalKotlinTargetHierarchy

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