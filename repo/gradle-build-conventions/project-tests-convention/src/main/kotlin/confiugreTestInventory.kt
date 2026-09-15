/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.withType

internal fun Project.configureTestInventory() {
    val listenerRegistry = objects.newInstance(BuildEventsListenerRegistryHolder::class.java).listenerRegistry

    tasks.withType<AbstractTestTask>().configureEach {
        val taskPath = path

        val testInventoryListener = TestInventoryListener(name, project.layout.buildDirectory.asFile)
        addTestListener(testInventoryListener)
        outputs.file(testInventoryListener.inventoryFile)

        // Declared as an output like the inventory above: that is what makes Gradle restore the file
        // when the task is served from the build cache, which is the whole point of recording it.
        val testExecutionsListener = TestExecutionsListener(name, taskPath, project.layout.buildDirectory.asFile)
        addTestListener(testExecutionsListener)
        outputs.file(testExecutionsListener.executionsFile)

        // Registered per *task*, not per project: the parameters below are plain values that are
        // already known at this point, so they do not depend on when Gradle isolates them. A map
        // filled in by this very 'configureEach' would still be empty at isolation time, because a
        // service is instantiated as soon as its listener is registered - which, for a registration
        // done while the plugin is applied, is before the test tasks even exist.
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
