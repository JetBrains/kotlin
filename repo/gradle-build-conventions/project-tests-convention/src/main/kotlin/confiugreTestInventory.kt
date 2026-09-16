/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.withType

internal fun Project.configureTestInventory() {
    val isTeamCityBuild = kotlinBuildProperties.isTeamcityBuild.get()
    val listenerRegistry by lazy { objects.newInstance(BuildEventsListenerRegistryHolder::class.java).listenerRegistry }

    tasks.withType<AbstractTestTask>().configureEach {
        val taskPath = path
        val testInventoryListener = TestInventoryListener(name, project.layout.buildDirectory.asFile)
        addTestListener(testInventoryListener)
        outputs.file(testInventoryListener.inventoryFile)

        val testExecutionsListener = TestExecutionsListener(name, taskPath, project.layout.buildDirectory.asFile)
        addTestListener(testExecutionsListener)
        outputs.file(testExecutionsListener.executionsFile)

        if (isTeamCityBuild) {
            val compatibilityService = project.gradle.sharedServices.registerIfAbsent(
                "${TestBuildCacheTeamCityCompatibilityService::class.qualifiedName}:$taskPath",
                TestBuildCacheTeamCityCompatibilityService::class.java,
            ) {
                parameters.taskPath.set(taskPath)
                parameters.executionsFile.set(testExecutionsListener.executionsFile)
            }

            listenerRegistry.onTaskCompletion(compatibilityService)
        }
    }
}
