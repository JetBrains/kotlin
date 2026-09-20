/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.provider.MapProperty
import org.gradle.api.services.BuildServiceRegistration
import org.gradle.api.services.BuildServiceRegistry
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.withType
import java.io.File

internal fun Project.configureTestInventory() {
    val isTeamCityBuild = kotlinBuildProperties.isTeamcityBuild.get()

    // Lazy: reaching for it registers the build's service, and most projects have no test task.
    val recordedExecutions by lazy { testBuildCacheTeamCityCompatibilityExecutions() }

    tasks.withType<AbstractTestTask>().configureEach {
        val taskPath = path
        val testInventoryListener = TestInventoryListener(name, project.layout.buildDirectory.asFile)
        addTestListener(testInventoryListener)
        outputs.file(testInventoryListener.inventoryFile)

        val testExecutionsListener = TestExecutionsListener(name, taskPath, project.layout.buildDirectory.asFile)
        addTestListener(testExecutionsListener)
        outputs.file(testExecutionsListener.executionsFile)

        if (isTeamCityBuild) {
            recordedExecutions.put(taskPath, testExecutionsListener.executionsFile)
        }
    }
}

private const val COMPATIBILITY_SERVICE_NAME = "TestBuildCacheTeamCityCompatibilityService"

/**
 * The map the build's single [TestBuildCacheTeamCityCompatibilityService] replays from, registering
 * and subscribing the service on first use.
 *
 * The registration holds the map: one service per build under its name, so every project fills the
 * same parameters, which are what the configuration cache stores.
 */
private fun Project.testBuildCacheTeamCityCompatibilityExecutions(): MapProperty<String, File> {
    val sharedServices = gradle.sharedServices

    // Gradle runs this action for the call that creates the registration and for no other, so the
    // build subscribes once however many projects ask for the service.
    sharedServices.registerIfAbsent(
        COMPATIBILITY_SERVICE_NAME,
        TestBuildCacheTeamCityCompatibilityService::class.java,
    ) {
        val listenerRegistry = objects.newInstance(BuildEventsListenerRegistryHolder::class.java).listenerRegistry

        // Impossible to use lambda because of whenReady(Closure closure) overload
        @Suppress("ObjectLiteralToLambda")
        val action = object : Action<TaskExecutionGraph> {
            override fun execute(graph: TaskExecutionGraph) {
                // Subscribing isolates the parameters, freezing the map, so it has to wait until
                // every project has added its test tasks. On a configuration cache hit this never
                // runs: the subscription stored with the entry, isolated map and all, replays there.
                val compatibility = sharedServices.testBuildCacheTeamCityCompatibilityRegistration

                // Test tasks configured without being scheduled are dropped: the map the build
                // carries, into its configuration cache entry among other places, is this run's own.
                val scheduled = graph.allTasks.mapTo(HashSet()) { it.path }
                val executionsFiles = compatibility.parameters.executionsFiles
                executionsFiles.set(executionsFiles.get().filterKeys { it in scheduled })

                listenerRegistry.onTaskCompletion(compatibility.service)
            }
        }

        gradle.taskGraph.whenReady(action)
    }

    return sharedServices.testBuildCacheTeamCityCompatibilityRegistration.parameters.executionsFiles
}

/**
 * The registration of the build's [TestBuildCacheTeamCityCompatibilityService], typed: it carries
 * both halves needed here, the parameters to fill and the service to subscribe.
 */
private val BuildServiceRegistry.testBuildCacheTeamCityCompatibilityRegistration:
        BuildServiceRegistration<
                TestBuildCacheTeamCityCompatibilityService,
                TestBuildCacheTeamCityCompatibilityService.Parameters>
    get() {
        @Suppress("UNCHECKED_CAST")
        return registrations.getByName(COMPATIBILITY_SERVICE_NAME)
                as BuildServiceRegistration<
                    TestBuildCacheTeamCityCompatibilityService,
                    TestBuildCacheTeamCityCompatibilityService.Parameters>
    }
