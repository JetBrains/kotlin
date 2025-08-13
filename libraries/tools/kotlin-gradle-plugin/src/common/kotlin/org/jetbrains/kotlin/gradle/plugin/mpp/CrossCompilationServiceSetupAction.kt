/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.tasks.withType

internal val CrossCompilationServiceSetupAction = KotlinProjectSetupAction {

    val serviceProvider = crossCompilationServiceProvider

    tasks.withType<UsesCrossCompilationService>().configureEach { task ->
        task.crossCompilationService.value(serviceProvider).disallowChanges()
        task.usesService(serviceProvider)
    }

    project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations) {
        serviceProvider.get().collectCinteropUsagesFrom(project)
    }
}