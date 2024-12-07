/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.gradle.internal.properties.NativeProperties
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector


/**
 * This class is required for checking that provided custom `kotlin.native.home` contains a Kotlin/Native bundle.
 * For that, we are checking the existence of some main subdirectories inside the installed Kotlin/Native bundle.
 */
internal object OverriddenKotlinNativeHomeChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        if (allKonanMainSubdirectoriesExist().get()) return

        collector.report(
            project, KotlinToolingDiagnostics.BrokenKotlinNativeBundleError(
                project.nativeProperties.userProvidedNativeHome.orNull,
                NativeProperties.NATIVE_HOME.name
            )
        )
    }

    private fun KotlinGradleProjectCheckerContext.allKonanMainSubdirectoriesExist() =
        // we need to wrap this check in ValueSource to prevent Gradle from monitoring the stdlib folder as a build configuration input
        project.providers.of(StdlibExistenceCheckerValueSource::class.java) {
            it.parameters.overriddenKotlinNativeHome.fileProvider(
                project.nativeProperties.userProvidedNativeHome.map { nativeHome -> project.file(nativeHome) }
            )
        }

    internal abstract class StdlibExistenceCheckerValueSource :
        ValueSource<Boolean, StdlibExistenceCheckerValueSource.Params> {

        interface Params : ValueSourceParameters {
            val overriddenKotlinNativeHome: DirectoryProperty
        }

        override fun obtain(): Boolean {
            return !parameters.overriddenKotlinNativeHome.isPresent || // when `kotlin.native.home` was not provided, we don't need to make this diagnostic
                    REQUIRED_SUBDIRECTORIES.all { subdir ->
                        parameters.overriddenKotlinNativeHome.asFile.get().resolve(subdir).exists()
                    }

        }

        companion object {
            private val REQUIRED_SUBDIRECTORIES = listOf("konan", "bin")
        }
    }
}