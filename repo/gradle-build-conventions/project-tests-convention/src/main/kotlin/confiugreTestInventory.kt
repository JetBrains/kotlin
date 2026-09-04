/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.withType
import java.io.File

internal fun Project.configureTestInventory() {
    // Collected while the project is configured, as this is the only phase where the task type is
    // known: a task finish event carries just a task path. Handed to the build service below as a
    // parameter so that it survives a configuration cache hit.
    val inventoryFiles = objects.mapProperty(String::class.java, File::class.java)

    tasks.withType<AbstractTestTask>().configureEach {

        val testInventoryListener = TestInventoryListener(name, project.layout.buildDirectory.asFile)
        addTestListener(testInventoryListener)
        outputs.file(testInventoryListener.inventoryFile)

        // 'path' is the task path here, matching what a task finish event reports. The 'Provider'
        // overload of 'put' keeps the build directory unresolved until the value is actually needed.
        inventoryFiles.put(path, testInventoryListener.inventoryFile)
    }

    // Registered per project ('path' is the project path here), so that the mapping only ever
    // contains tasks of this project and no configuration state is shared between projects.
    val compatibilityService = gradle.sharedServices.registerIfAbsent(
        "${TestBuildCacheTeamCityCompatibilityService::class.qualifiedName}:$path",
        TestBuildCacheTeamCityCompatibilityService::class.java,
    ) {
        parameters.inventoryFiles.set(inventoryFiles)
    }

    objects.newInstance(BuildEventsListenerRegistryHolder::class.java)
        .listenerRegistry
        .onTaskCompletion(compatibilityService)
}
