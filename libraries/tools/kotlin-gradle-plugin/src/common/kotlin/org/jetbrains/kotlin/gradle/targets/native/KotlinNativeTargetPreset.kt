/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Old package for compatibility
@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinProjectSharedDataProvider
import org.jetbrains.kotlin.gradle.plugin.internal.kotlinSecondaryVariantsDataSharing
import org.jetbrains.kotlin.gradle.targets.android.internal.InternalKotlinTargetPreset
import org.jetbrains.kotlin.gradle.targets.native.internal.getOrRegisterDownloadKotlinNativeDistributionTask
import org.jetbrains.kotlin.gradle.targets.native.internal.setupCInteropCommonizerDependencies
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.lenient
import org.jetbrains.kotlin.gradle.utils.setupNativeCompiler
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost
import org.jetbrains.kotlin.gradle.dsl.konanTarget
import org.jetbrains.kotlin.gradle.dsl.canLinkBinaries
import org.jetbrains.kotlin.konan.target.KonanTarget

internal abstract class AbstractKotlinNativeTargetPreset<T : KotlinNativeTarget>(
    override val name: String,
    val project: Project,
    val konanTarget: KonanTarget,
) : InternalKotlinTargetPreset<T> {

    init {
        // This is required to obtain Kotlin/Native home in the IDE plugin:
        setupNativeHomePrivateProperty()
    }

    private fun setupNativeHomePrivateProperty() = with(project) {
        if (!extensions.extraProperties.has(KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY))
            extensions.extraProperties.set(
                KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY,
                nativeProperties.actualNativeHomeDirectory.get().absolutePath
            )
    }

    protected abstract fun createTargetConfigurator(): AbstractKotlinTargetConfigurator<T>

    protected abstract fun instantiateTarget(name: String): T

    override fun createTargetInternal(name: String): T {
        if (!project.nativeProperties.isToolchainEnabled.get()) {
            @Suppress("DEPRECATION")
            project.setupNativeCompiler(konanTarget)
        }

        val result = instantiateTarget(name).apply {
            targetName = name
            disambiguationClassifier = name
            targetPreset = this@AbstractKotlinNativeTargetPreset

            val compilationFactory = KotlinNativeCompilationFactory(this)
            compilations = project.objects.domainObjectContainer(compilationFactory.itemClass, compilationFactory)
        }

        createTargetConfigurator().configureTarget(result)

        SingleActionPerProject.run(project, "setupCInteropDependencies") {
            project.getOrRegisterDownloadKotlinNativeDistributionTask()
            project.setupCInteropCommonizerDependencies()
        }

        return result
    }

    companion object {
        private const val KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY = "konanHome"
    }

}

internal open class KotlinNativeTargetPreset(name: String, project: Project, konanTarget: KonanTarget) :
    AbstractKotlinNativeTargetPreset<KotlinNativeTarget>(name, project, konanTarget) {

    override fun createTargetConfigurator(): AbstractKotlinTargetConfigurator<KotlinNativeTarget> =
        KotlinNativeTargetConfigurator()

    override fun instantiateTarget(name: String): KotlinNativeTarget {
        return project.objects.newInstance(KotlinNativeTarget::class.java, project, konanTarget)
    }
}

internal open class KotlinNativeTargetWithHostTestsPreset(name: String, project: Project, konanTarget: KonanTarget) :
    AbstractKotlinNativeTargetPreset<KotlinNativeTargetWithHostTests>(name, project, konanTarget) {

    override fun createTargetConfigurator(): AbstractKotlinTargetConfigurator<KotlinNativeTargetWithHostTests> =
        KotlinNativeTargetConfigurator()

    override fun instantiateTarget(name: String): KotlinNativeTargetWithHostTests =
        project.objects.newInstance(KotlinNativeTargetWithHostTests::class.java, project, konanTarget)
}

internal open class KotlinNativeTargetWithSimulatorTestsPreset(name: String, project: Project, konanTarget: KonanTarget) :
    AbstractKotlinNativeTargetPreset<KotlinNativeTargetWithSimulatorTests>(name, project, konanTarget) {

    override fun createTargetConfigurator(): AbstractKotlinTargetConfigurator<KotlinNativeTargetWithSimulatorTests> =
        KotlinNativeTargetConfigurator()

    override fun instantiateTarget(name: String): KotlinNativeTargetWithSimulatorTests =
        project.objects.newInstance(KotlinNativeTargetWithSimulatorTests::class.java, project, konanTarget)
}

internal val KonanTarget.isCurrentHost: Boolean
    get() = this == KotlinNativeHost.current().konanTarget

/**
 * Returns whether klib compilation is allowed for [this]-target on the current host.
 * [enabledOnCurrentHostForBinariesCompilation] returns 'true' only if [enabledOnCurrentHostForKlibCompilation]
 * returns 'true'
 *
 * [enabledOnCurrentHostForKlibCompilation] might return 'true' in some cases where [enabledOnCurrentHostForBinariesCompilation]
 * returns 'false' (e.g.: compile a klib for iOS target on Linux when the code depends only on Kotlin Stdlib)
 *
 * See KT-64512 for a finer-grained model on the Kotlin/Native side.
 */
@Deprecated("Use crossCompilationOnCurrentHostSupported instead")
internal fun KonanTarget.enabledOnCurrentHostForKlibCompilation(
    provider: PropertiesProvider,
): Boolean {
    val host = KotlinNativeHost.current()
    // With cross-compilation enabled every target compiles on a supported host. Otherwise only linkable targets do.
    return host.isSupported && (provider.enableKlibsCrossCompilation || host.canLinkBinaries(this))
}

internal val AbstractKotlinNativeCompilation.crossCompilationSharedData: KotlinProjectSharedDataProvider<CrossCompilationData>
    get() = project.kotlinSecondaryVariantsDataSharing.consumeCrossCompilationMetadata(
        compilation.configurations.compileDependencyConfiguration
    )

internal val AbstractKotlinNativeCompilation.crossCompilationOnCurrentHostSupported: Boolean
    get() = when (this) {
        is KotlinNativeCompilation -> target.crossCompilationOnCurrentHostSupported.getOrThrow()
        else -> KotlinNativeHost.current().isSupported
    }

// KT-81134 with a fallback to `enabledOnCurrentHostForKlibCompilation`
@Suppress("DEPRECATION")
internal val KotlinNativeTarget.publishableWithFallback: Boolean
    get() = crossCompilationOnCurrentHostSupported.lenient.getOrNull()
        ?: konanTarget.enabledOnCurrentHostForKlibCompilation(project.kotlinPropertiesProvider)

internal val KonanTarget.enabledOnCurrentHostForBinariesCompilation
    get() = KotlinNativeHost.current().canLinkBinaries(this)

/** Human-readable names of the supported hosts for diagnostics, e.g. `macOS (arm64)`. */
internal val supportedNativeHosts: List<String> = KotlinNativeHost.supportedHosts().map { it.displayName }

internal val KotlinNativeHost.displayName: String
    get() {
        val osName = when (os) {
            KotlinNativeHostOs.MACOS -> "macOS"
            KotlinNativeHostOs.LINUX -> "Linux"
            KotlinNativeHostOs.WINDOWS -> "Windows"
            KotlinNativeHostOs.UNKNOWN -> "unknown OS"
        }
        val archName = when (architecture) {
            KotlinNativeHostArchitecture.X64 -> "x86_64"
            KotlinNativeHostArchitecture.ARM64 -> "arm64"
            KotlinNativeHostArchitecture.UNKNOWN -> "unknown architecture"
        }
        return "$osName ($archName)"
    }

/**
 * `true` on a macOS host with a Kotlin/Native distribution.
 *
 * KGP's Apple tooling needs linked Kotlin/Native binaries, so it is gated on this instead of [KotlinNativeHost.isMacOs].
 * An Intel Mac is macOS but unsupported, and its tasks stay skipped as they were with `HostManager.hostIsMac`.
 */
internal val KotlinNativeHost.isSupportedMacOs: Boolean
    get() = isMacOs && isSupported
