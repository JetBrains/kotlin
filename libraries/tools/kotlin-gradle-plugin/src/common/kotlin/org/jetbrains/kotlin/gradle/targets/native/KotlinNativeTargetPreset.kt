/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Old package for compatibility
@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.targets.android.internal.InternalKotlinTargetPreset
import org.jetbrains.kotlin.gradle.targets.native.internal.getOrRegisterDownloadKotlinNativeDistributionTask
import org.jetbrains.kotlin.gradle.targets.native.internal.setupCInteropCommonizerDependencies
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.setupNativeCompiler
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.lenient
import org.jetbrains.kotlin.konan.target.HostManager
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
            compilations = project.container(compilationFactory.itemClass, compilationFactory)
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
    get() = this == HostManager.host

/**
 * Returns whether klib compilation is allowed for [this]-target on the current host.
 * [enabledOnCurrentHostForBinariesCompilation] returns 'true' only if [enabledOnCurrentHostForKlibCompilation]
 * returns 'true'
 *
 * [enabledOnCurrentHostForKlibCompilation] might return 'true' in some cases where [enabledOnCurrentHostForBinariesCompilation]
 * returns 'false' (e.g.: compile a klib for iOS target on Linux when the code depends only on Kotlin Stdlib)
 *
 * Ideally, these APIs should be in [HostManager] instead of KGP-side wrappers. Refer to KT-64512 for that
 */
@Deprecated("Use crossCompilationOnCurrentHostSupported instead")
internal fun KonanTarget.enabledOnCurrentHostForKlibCompilation(
    provider: PropertiesProvider,
) = if (provider.enableKlibsCrossCompilation) {
    // If cross-compilation is enabled, allow compilation for all targets
    true
} else {
    // If cross-compilation is disabled use standard HostManager enablement check
    HostManager().isEnabled(this)
}

internal val AbstractKotlinNativeCompilation.crossCompilationOnCurrentHostSupported: Future<Boolean>
    get() = when (this) {
        is KotlinNativeCompilation -> target.crossCompilationOnCurrentHostSupported
        else -> project.future { true }
    }

// KT-81134 with a fallback to `enabledOnCurrentHostForKlibCompilation`
@Suppress("DEPRECATION")
internal val KotlinNativeTarget.publishableWithFallback: Boolean
    get() = crossCompilationOnCurrentHostSupported.lenient.getOrNull()
        ?: konanTarget.enabledOnCurrentHostForKlibCompilation(project.kotlinPropertiesProvider)

internal val KonanTarget.enabledOnCurrentHostForBinariesCompilation
    get() = HostManager().isEnabled(this)