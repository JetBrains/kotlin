/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl
import org.jetbrains.kotlin.gradle.internal.syncCommonMultiplatformOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.hierarchy.KotlinHierarchyDslImpl
import org.jetbrains.kotlin.gradle.plugin.hierarchy.redundantDependsOnEdgesTracker
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.android.internal.InternalKotlinTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmWasiTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWasmTargetPreset
import org.jetbrains.kotlin.gradle.utils.KotlinCommonCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.utils.addConfigurationMetrics
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import javax.inject.Inject

internal fun ExtensionContainer.KotlinMultiplatformExtension(
    objectFactory: ObjectFactory,
): KotlinMultiplatformExtension {
    val targetsContainer = objectFactory.newInstance<DefaultKotlinTargetsContainer>()
    val presetsContainer = objectFactory.DefaultKotlinTargetContainerWithPresetFunctions(targetsContainer.targets)
    return create(
        KOTLIN_PROJECT_EXTENSION_NAME,
        KotlinMultiplatformExtension::class.java,
        targetsContainer,
        presetsContainer,
    )
}

@KotlinGradlePluginPublicDsl
abstract class KotlinMultiplatformExtension
@Inject
internal constructor(
    project: Project,
    private val targetsContainer: KotlinTargetsContainer,
    internal val presetFunctions: DefaultKotlinTargetContainerWithPresetFunctions = project.objects
        .DefaultKotlinTargetContainerWithPresetFunctions(targetsContainer.targets),
) : KotlinTargetsContainer by targetsContainer,
    KotlinProjectExtension(project),
    KotlinTargetContainerWithPresetFunctions by presetFunctions,
    KotlinTargetContainerWithJsPresetFunctions,
    KotlinTargetContainerWithWasmPresetFunctions,
    KotlinHierarchyDsl,
    KotlinPublishingDsl,
    HasConfigurableKotlinCompilerOptions<KotlinCommonCompilerOptions>,
    KotlinMultiplatformSourceSetConventions by KotlinMultiplatformSourceSetConventionsImpl {

    @InternalKotlinGradlePluginApi
    constructor(
        project: Project,
    ) : this(
        project = project,
        targetsContainer = project.objects.newInstance<KotlinTargetsContainer>(),
    )

    final override val targets: NamedDomainObjectCollection<KotlinTarget>
        get() = targetsContainer.targets

    override fun js(
        name: String,
        compiler: KotlinJsCompilerType,
        configure: KotlinJsTargetDsl.() -> Unit,
    ): KotlinJsTargetDsl {
        @Suppress("UNCHECKED_CAST")
        return presetFunctions.configureOrCreate(
            name,
            presetFunctions.presets.getByName(
                "js"
            ) as InternalKotlinTargetPreset<KotlinJsTargetDsl>,
            configure
        )
    }

    @ExperimentalWasmDsl
    override fun wasmJs(
        name: String,
        configure: KotlinWasmJsTargetDsl.() -> Unit,
    ): KotlinWasmJsTargetDsl =
        presetFunctions.configureOrCreate(
            name,
            presetFunctions.presets.getByName("wasmJs") as KotlinWasmTargetPreset,
            configure
        )

    @ExperimentalWasmDsl
    override fun wasmWasi(
        name: String,
        configure: KotlinWasmWasiTargetDsl.() -> Unit,
    ): KotlinWasmWasiTargetDsl =
        presetFunctions.configureOrCreate(
            name,
            presetFunctions.presets.getByName("wasmWasi") as KotlinWasmTargetPreset,
            configure
        )

    fun dependencies(configure: Action<KotlinDependencies>) = dependencies { configure.execute(this) }

    fun dependencies(configure: KotlinDependencies.() -> Unit) {
        project.addConfigurationMetrics {
            it.put(BooleanMetrics.KMP_TOP_LEVEL_DEPENDENCIES_BLOCK, true)
        }
        when (val dependencies = dependencies) {
            KotlinTopLevelDependenciesBlock.UnavailableInCurrentGradleVersion -> {
                project.reportDiagnostic(
                    KotlinToolingDiagnostics.KotlinTopLevelDependenciesUsedInIncompatibleGradleVersion(
                        currentGradleVersion = GradleVersion.current(),
                        minimumSupportedGradleVersion = MinSupportedGradleVersionWithDependencyCollectors,
                    )
                )
            }
            is KotlinTopLevelDependenciesBlock.Dependencies -> {
                dependencies.block.configure()
            }
        }
    }

    @Deprecated(
        "Because only the IR compiler is left, it's no longer necessary to know about the compiler type in properties. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR
    )
    override val compilerTypeFromProperties: KotlinJsCompilerType? = null

    internal suspend fun awaitTargets(): NamedDomainObjectCollection<KotlinTarget> {
        AfterFinaliseDsl.await()
        return targets
    }

    private val presetExtension = project.objects.DefaultTargetsFromPresetExtension(
        targets,
        project,
    )

    @Suppress("DEPRECATION")
    @Deprecated(
        message = targetsExtensionDeprecationMessage,
        level = DeprecationLevel.WARNING,
    )
    fun targets(configure: Action<TargetsFromPresetExtension>) {
        configure.execute(presetExtension)
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        message = targetsExtensionDeprecationMessage,
        level = DeprecationLevel.WARNING,
    )
    fun targets(configure: TargetsFromPresetExtension.() -> Unit) {
        configure(presetExtension)
    }

    internal val hierarchy by lazy { KotlinHierarchyDslImpl(targets, sourceSets, redundantDependsOnEdgesTracker) }

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
     * Will create the following shared [SourceSet][KotlinSourceSet]s:
     *
     * - `iosMain`, `iosTest`
     * - `appleMain`, `appleTest`
     * - `linuxMain`, `linuxTest`
     * - `nativeMain`, `nativeTest`
     *
     * Hierarchy:
     * ```text
     *                                    common
     *                                      │
     *                         ┌────────────┴──────────────┐
     *                         │                           │
     *                       native                       ...
     *                         │
     *             ┌───────┬───┴───┬───────────┐
     *             │       │       │           │
     *           apple   linux   mingw   androidNative
     *             │
     *   ┌──────┬──┴──┬────────┐
     *   │      │     │        │
     * macos   ios   tvos   watchos
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
    val targetHierarchy: @Suppress("DEPRECATION_ERROR") DeprecatedKotlinTargetHierarchyDsl
        get() = @Suppress("DEPRECATION_ERROR") DeprecatedKotlinTargetHierarchyDsl(this)

    @Suppress("unused") // DSL
    val testableTargets: NamedDomainObjectCollection<KotlinTargetWithTests<*, *>>
        get() = targets.withType(KotlinTargetWithTests::class.java)

    fun metadata(configure: KotlinOnlyTarget<KotlinMetadataCompilation<Any>>.() -> Unit = { }): KotlinOnlyTarget<KotlinMetadataCompilation<Any>> =
        @Suppress("UNCHECKED_CAST")
        (targets.getByName(KotlinMetadataTarget.METADATA_TARGET_NAME) as KotlinOnlyTarget<KotlinMetadataCompilation<Any>>).also(configure)

    fun metadata(configure: Action<KotlinOnlyTarget<KotlinMetadataCompilation<Any>>>) = metadata { configure.execute(this) }

    fun withSourcesJar(publish: Boolean = true) {
        targets.all { it.withSourcesJar(publish) }
    }

    internal val rootSoftwareComponent: KotlinSoftwareComponent by lazy {
        KotlinSoftwareComponentWithCoordinatesAndPublication(
            project,
            "kotlin",
            targets,
            publishing.adhocSoftwareComponent
        )
    }

    override val publishing: KotlinPublishing = project.objects.newInstance<KotlinMultiplatformPublishing>()

    override val compilerOptions: KotlinCommonCompilerOptions =
        project.objects.KotlinCommonCompilerOptionsDefault(project)
            .also {
                syncCommonMultiplatformOptions(it)
            }
}

private const val targetsExtensionDeprecationMessage =
    "Usages of this DSL are deprecated, please migrate to top-level 'kotlin {}' extension."

@Deprecated(
    message = targetsExtensionDeprecationMessage,
    level = DeprecationLevel.WARNING,
)
@KotlinGradlePluginPublicDsl
interface TargetsFromPresetExtension : NamedDomainObjectCollection<KotlinTarget>


internal fun ObjectFactory.DefaultTargetsFromPresetExtension(
    targets: NamedDomainObjectCollection<KotlinTarget>,
    project: Project,
): DefaultTargetsFromPresetExtension = newInstance(
    targets,
    project,
)

@Suppress("DEPRECATION")
internal abstract class DefaultTargetsFromPresetExtension @Inject constructor(
    val targets: NamedDomainObjectCollection<KotlinTarget>,
    val project: Project,
) : TargetsFromPresetExtension,
    NamedDomainObjectCollection<KotlinTarget> by targets

private fun KotlinTarget.isProducedFromPreset(kotlinTargetPreset: InternalKotlinTargetPreset<*>): Boolean =
    internal.targetPreset == kotlinTargetPreset

internal fun <T : KotlinTarget> DefaultKotlinTargetContainerWithPresetFunctions.configureOrCreate(
    targetName: String,
    targetPreset: InternalKotlinTargetPreset<T>,
    configure: T.() -> Unit = {},
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
            // FIXME: KT-71529 - check if this diagnostic is actually reachable and cover with tests or remove
            throw InvalidUserCodeException(
                "The target '$targetName' already exists, but it was not created with the '${targetPreset.name}' preset. " +
                        "To configure it, access it using targets API: https://kotl.in/target-configuration"
            )
        }
    }
}

internal val KotlinMultiplatformExtension.metadataTarget get() = metadata() as KotlinMetadataTarget

internal val Iterable<KotlinTarget>.platformTargets: List<KotlinTarget> get() = filter { it !is KotlinMetadataTarget }

internal suspend fun KotlinMultiplatformExtension.awaitMetadataTarget(): KotlinMetadataTarget {
    awaitTargets()
    return metadataTarget
}

internal fun KotlinMultiplatformExtension.supportedAppleTargets() = targets
    .withType(KotlinNativeTarget::class.java)
    .matching { it.konanTarget.family.isAppleFamily }

private abstract class DefaultKotlinTargetsContainer @Inject constructor(
    objectFactory: ObjectFactory,
) : KotlinTargetsContainer {
    override val targets: NamedDomainObjectCollection<KotlinTarget> =
        objectFactory.domainObjectContainer(KotlinTarget::class.java)
}
