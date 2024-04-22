/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.compilerRunner.kotlinNativeToolchainEnabled
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.internal.configurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.usedAtConfigurationTime
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.utils.`is`
import org.jetbrains.kotlin.gradle.utils.konanDistribution

/**
 * This class is made to check that kotlin native was successfully installed before the build
 */
internal object MissingNativeStdlibChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val targets = multiplatformExtension?.awaitTargets() ?: return
        if (targets.isEmpty() || // misconfigured project
            targets.none { it is KotlinNativeTarget } || // no K/N targets
            checkThatStdlibExists().get()
        ) return

        collector.report(project, KotlinToolingDiagnostics.NativeStdlibIsMissingDiagnostic(
            PropertiesProvider.KOTLIN_NATIVE_HOME.takeIf { kotlinPropertiesProvider.nativeHome != null }
        ))
    }

    private fun KotlinGradleProjectCheckerContext.checkThatStdlibExists() =
        // we need to wrap this check in ValueSource to prevent Gradle from monitoring the stdlib folder as a build configuration input
        project.providers.of(StdlibExistenceCheckerValueSource::class.java) {
            it.parameters.noStdlibEnabled.set(project.hasProperty("kotlin.native.nostdlib"))
            it.parameters.kotlinNativeToolchainEnabled.set(project.kotlinNativeToolchainEnabled)
            it.parameters.stdlib.setFrom(project.konanDistribution.stdlib)
            it.parameters.overriddenKotlinNativeHome.set(project.kotlinPropertiesProvider.nativeHome)
        }.usedAtConfigurationTime(project.configurationTimePropertiesAccessor)

    internal abstract class StdlibExistenceCheckerValueSource :
        ValueSource<Boolean, StdlibExistenceCheckerValueSource.Params> {

        interface Params : ValueSourceParameters {
            val noStdlibEnabled: Property<Boolean>
            val kotlinNativeToolchainEnabled: Property<Boolean>
            val stdlib: ConfigurableFileCollection
            val overriddenKotlinNativeHome: Property<String>
        }

        override fun obtain(): Boolean {
            return parameters.noStdlibEnabled.get() || // suppressed
                    (parameters.kotlinNativeToolchainEnabled.get() && !parameters.overriddenKotlinNativeHome.isPresent) || // with toolchain, we download konan after configuration phase, thus, we shouldn't check existence here
                    parameters.stdlib.singleFile.exists()
        }
    }
}
