/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Old package for compatibility
@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.kotlinNativeToolchainEnabled
import org.jetbrains.kotlin.gradle.DeprecatedTargetPresetApi
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.android.internal.InternalKotlinTargetPreset
import org.jetbrains.kotlin.gradle.targets.native.internal.*
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.setupNativeCompiler
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

@DeprecatedTargetPresetApi
abstract class AbstractKotlinNativeTargetPreset<T : KotlinNativeTarget>(
    private val name: String,
    val project: Project,
    val konanTarget: KonanTarget
) : InternalKotlinTargetPreset<T> {

    init {
        // This is required to obtain Kotlin/Native home in IDE plugin:
        setupNativeHomePrivateProperty()
    }

    override fun getName(): String = name

    private fun setupNativeHomePrivateProperty() = with(project) {
        if (!hasProperty(KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY))
            extensions.extraProperties.set(KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY, konanHome.absolutePath)
    }

    protected abstract fun createTargetConfigurator(): AbstractKotlinTargetConfigurator<T>

    protected abstract fun instantiateTarget(name: String): T

    override fun createTargetInternal(name: String): T {
        if (!project.kotlinNativeToolchainEnabled) {
            @Suppress("DEPRECATION")
            project.setupNativeCompiler(konanTarget)
        }

        val result = instantiateTarget(name).apply {
            targetName = name
            disambiguationClassifier = name
            @Suppress("DEPRECATION")
            preset = this@AbstractKotlinNativeTargetPreset

            val compilationFactory = KotlinNativeCompilationFactory(this)
            compilations = project.container(compilationFactory.itemClass, compilationFactory)
        }

        createTargetConfigurator().configureTarget(result)

        SingleActionPerProject.run(project, "setupCInteropDependencies") {
            project.setupCInteropCommonizerDependencies()
            project.setupCInteropPropagatedDependencies()
        }

        return result
    }

    companion object {
        private const val KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY = "konanHome"
    }

}

@DeprecatedTargetPresetApi
open class KotlinNativeTargetPreset(name: String, project: Project, konanTarget: KonanTarget) :
    AbstractKotlinNativeTargetPreset<KotlinNativeTarget>(name, project, konanTarget) {

    override fun createTargetConfigurator(): AbstractKotlinTargetConfigurator<KotlinNativeTarget> =
        KotlinNativeTargetConfigurator()

    override fun instantiateTarget(name: String): KotlinNativeTarget {
        return project.objects.newInstance(KotlinNativeTarget::class.java, project, konanTarget)
    }
}

@DeprecatedTargetPresetApi
open class KotlinNativeTargetWithHostTestsPreset(name: String, project: Project, konanTarget: KonanTarget) :
    AbstractKotlinNativeTargetPreset<KotlinNativeTargetWithHostTests>(name, project, konanTarget) {

    override fun createTargetConfigurator(): AbstractKotlinTargetConfigurator<KotlinNativeTargetWithHostTests> =
        KotlinNativeTargetConfigurator()

    override fun instantiateTarget(name: String): KotlinNativeTargetWithHostTests =
        project.objects.newInstance(KotlinNativeTargetWithHostTests::class.java, project, konanTarget)
}

@DeprecatedTargetPresetApi
open class KotlinNativeTargetWithSimulatorTestsPreset(name: String, project: Project, konanTarget: KonanTarget) :
    AbstractKotlinNativeTargetPreset<KotlinNativeTargetWithSimulatorTests>(name, project, konanTarget) {

    override fun createTargetConfigurator(): AbstractKotlinTargetConfigurator<KotlinNativeTargetWithSimulatorTests> =
        KotlinNativeTargetConfigurator()

    override fun instantiateTarget(name: String): KotlinNativeTargetWithSimulatorTests =
        project.objects.newInstance(KotlinNativeTargetWithSimulatorTests::class.java, project, konanTarget)
}

internal val KonanTarget.isCurrentHost: Boolean
    get() = this == HostManager.host

internal val KonanTarget.enabledOnCurrentHost
    get() = HostManager().isEnabled(this)
