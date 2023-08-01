/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.PRESETS_API_IS_DEPRECATED_MESSAGE
import org.jetbrains.kotlin.gradle.DeprecatedTargetPresetApi
import org.jetbrains.kotlin.gradle.internal.syncCommonOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.hierarchy.KotlinHierarchyDslImpl
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.android.internal.InternalKotlinTargetPreset
import org.jetbrains.kotlin.gradle.targets.android.internal.internal
import org.jetbrains.kotlin.gradle.utils.configureExperimentalTryK2
import org.jetbrains.kotlin.gradle.utils.newInstance
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
        PRESETS_API_IS_DEPRECATED_MESSAGE,
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

    /**
     * @see applyDefaultHierarchyTemplate
     * @see applyHierarchyTemplate
     * @param extension: Additionally extend the default hierarchy with additional groups
     *
     */
    @ExperimentalKotlinGradlePluginApi
    fun applyDefaultHierarchyTemplate(extension: KotlinHierarchyBuilder.Root.() -> Unit) {
        hierarchy.applyHierarchyTemplate(KotlinHierarchyTemplate.default, extension)
    }

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

    @ExperimentalKotlinGradlePluginApi
    val targetHierarchy: DeprecatedKotlinTargetHierarchyDsl get() = DeprecatedKotlinTargetHierarchyDsl(this)

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

    private fun warnAboutTargetFromPresetDeprecation() {
        project.reportDiagnostic(KotlinToolingDiagnostics.TargetFromPreset())
    }

    @DeprecatedTargetPresetApi
    @Deprecated(
        KotlinToolingDiagnostics.TargetFromPreset.DEPRECATION_MESSAGE,
        level = DeprecationLevel.WARNING
    )
    fun <T : KotlinTarget> targetFromPreset(
        preset: KotlinTargetPreset<T>,
        name: String = preset.name,
        configure: T.() -> Unit = { },
    ): T {
        warnAboutTargetFromPresetDeprecation()
        return targetFromPresetInternal(
            preset = preset,
            name = name,
            configure = configure,
        )
    }

    @DeprecatedTargetPresetApi
    @Deprecated(
        KotlinToolingDiagnostics.TargetFromPreset.DEPRECATION_MESSAGE,
        level = DeprecationLevel.WARNING
    )
    fun <T : KotlinTarget> targetFromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configure: Action<T>,
    ): T {
        warnAboutTargetFromPresetDeprecation()
        return targetFromPresetInternal(
            preset = preset,
            name = name,
            configure = configure,
        )
    }

    @DeprecatedTargetPresetApi
    @Deprecated(
        KotlinToolingDiagnostics.TargetFromPreset.DEPRECATION_MESSAGE,
        level = DeprecationLevel.WARNING
    )
    fun <T : KotlinTarget> targetFromPreset(preset: KotlinTargetPreset<T>): T {
        warnAboutTargetFromPresetDeprecation()
        return targetFromPresetInternal(
            preset = preset
        )
    }

    @DeprecatedTargetPresetApi
    @Deprecated(
        KotlinToolingDiagnostics.TargetFromPreset.DEPRECATION_MESSAGE,
        level = DeprecationLevel.WARNING
    )
    fun <T : KotlinTarget> targetFromPreset(preset: KotlinTargetPreset<T>, name: String): T {
        warnAboutTargetFromPresetDeprecation()
        return targetFromPresetInternal(
            preset = preset,
            name = name
        )
    }

    @DeprecatedTargetPresetApi
    @Deprecated(
        KotlinToolingDiagnostics.TargetFromPreset.DEPRECATION_MESSAGE,
        level = DeprecationLevel.WARNING
    )
    fun <T : KotlinTarget> targetFromPreset(preset: KotlinTargetPreset<T>, configure: Action<T>): T {
        warnAboutTargetFromPresetDeprecation()
        return targetFromPresetInternal(
            preset = preset,
            configure = configure
        )
    }

    internal val rootSoftwareComponent: KotlinSoftwareComponent by lazy {
        KotlinSoftwareComponentWithCoordinatesAndPublication(project, "kotlin", targets)
    }

    @ExperimentalKotlinGradlePluginApi
    val compilerOptions: KotlinCommonCompilerOptions = project.objects
        .newInstance<KotlinCommonCompilerOptionsDefault>()
        .configureExperimentalTryK2(project)
        .also {
            syncCommonOptions(it)
        }

    @ExperimentalKotlinGradlePluginApi
    fun compilerOptions(configure: KotlinCommonCompilerOptions.() -> Unit) {
        configure(compilerOptions)
    }

    @ExperimentalKotlinGradlePluginApi
    fun compilerOptions(configure: Action<KotlinCommonCompilerOptions>) {
        configure.execute(compilerOptions)
    }
}

@DeprecatedTargetPresetApi
@KotlinGradlePluginDsl
interface TargetsFromPresetExtension : NamedDomainObjectCollection<KotlinTarget> {

    @Deprecated(
        KotlinToolingDiagnostics.FromPreset.DEPRECATION_MESSAGE,
        level = DeprecationLevel.WARNING,
    )
    fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configureAction: T.() -> Unit = {},
    ): T

    @Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")
    @Deprecated(
        KotlinToolingDiagnostics.FromPreset.DEPRECATION_MESSAGE,
        level = DeprecationLevel.WARNING,
    )
    fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
    ): T = fromPreset(preset, name) {}

    @Deprecated(
        KotlinToolingDiagnostics.FromPreset.DEPRECATION_MESSAGE,
        level = DeprecationLevel.WARNING,
    )
    fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configureAction: Action<T>,
    ): T
}

@Suppress("OVERRIDE_DEPRECATION")
internal abstract class DefaultTargetsFromPresetExtension @Inject constructor(
    private val targetsContainer: () -> KotlinTargetsContainerWithPresets,
    val targets: NamedDomainObjectCollection<KotlinTarget>,
    val project: Project,
) : TargetsFromPresetExtension,
    NamedDomainObjectCollection<KotlinTarget> by targets {

    private fun warnAboutFromPresetDeprecation() {
        project.reportDiagnostic(KotlinToolingDiagnostics.FromPreset())
    }

    override fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configureAction: T.() -> Unit,
    ): T {
        warnAboutFromPresetDeprecation()
        return targetsContainer().configureOrCreate(name, preset.internal, configureAction)
    }

    @Suppress("DEPRECATION")
    override fun <T : KotlinTarget> fromPreset(
        preset: KotlinTargetPreset<T>,
        name: String,
        configureAction: Action<T>,
    ) = fromPreset(preset, name) {
        configureAction.execute(this)
    }
}

@Suppress("DEPRECATION")
private fun KotlinTarget.isProducedFromPreset(kotlinTargetPreset: KotlinTargetPreset<*>): Boolean =
    preset == kotlinTargetPreset

internal fun <T : KotlinTarget> KotlinTargetsContainerWithPresets.configureOrCreate(
    targetName: String,
    targetPreset: InternalKotlinTargetPreset<T>,
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
            @Suppress("DEPRECATION")
            throw InvalidUserCodeException(
                "The target '$targetName' already exists, but it was not created with the '${targetPreset.name}' preset. " +
                        "To configure it, access it by name in `kotlin.targets`" +
                        (" or use the preset function '${existingTarget.preset?.name}'."
                            .takeIf { existingTarget.preset != null } ?: ".")
            )
        }
    }
}
