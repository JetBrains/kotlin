/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics

@DisableCachingByDefault(because = "Validation task, no outputs")
internal abstract class ValidateXcodeArchitecturesTask : DefaultTask(), UsesKotlinToolingDiagnostics {

    @get:Input
    abstract val requestedTargets: ListProperty<String>

    @get:Input
    abstract val configuredTargets: SetProperty<String>

    @get:Input
    abstract val frameworkName: Property<String>

    @TaskAction
    fun validate() {
        val missingTargets = requestedTargets.get().filterNot(configuredTargets.get()::contains)
        if (missingTargets.isNotEmpty()) {
            reportDiagnostic(
                KotlinToolingDiagnostics.XcodeArchitectureNotConfiguredInGradle(
                    missingTargets = missingTargets,
                    frameworkName = frameworkName.get(),
                )
            )
        }
    }
}
