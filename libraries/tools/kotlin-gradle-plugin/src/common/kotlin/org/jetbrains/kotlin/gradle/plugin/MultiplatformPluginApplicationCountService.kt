/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.util.concurrent.atomic.AtomicInteger

internal abstract class MultiplatformPluginApplicationCountService : BuildService<BuildServiceParameters.None> {
    private val applications = AtomicInteger()

    val count: Int
        get() = applications.get()

    fun onPluginApplied() {
        applications.incrementAndGet()
    }

    companion object {
        fun getInstance(project: Project): MultiplatformPluginApplicationCountService =
            project.gradle.registerClassLoaderScopedBuildService(MultiplatformPluginApplicationCountService::class).get()
    }
}

internal val CountKotlinMultiplatformPluginApplicationsSetupAction = KotlinProjectSetupAction {
    MultiplatformPluginApplicationCountService.getInstance(project).onPluginApplied()
}
