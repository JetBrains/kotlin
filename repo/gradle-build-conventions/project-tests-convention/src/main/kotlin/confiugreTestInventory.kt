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

    // Lazily, because reaching for it registers the build's service, and most projects that apply
    // this convention have no test task to report.
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
 * the service and subscribing it to task completions the first time a project asks for it.
 *
 * The registration doubles as the build scoped holder of the map: a shared service exists once per
 * build under its name, so every project fills the same parameters, and those parameters are what
 * the configuration cache stores for the service and hands back to it on a build that skipped
 * configuring.
 */
private fun Project.testBuildCacheTeamCityCompatibilityExecutions(): MapProperty<String, File> {
    val sharedServices = gradle.sharedServices

    // Gradle runs the configuration action for the call that creates the registration and for no
    // other, so the build subscribes once however many projects ask for the service.
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
                // every project has added its test tasks - which is what makes one service for the
                // whole build possible at all. Subscribing while the plugin is applied would freeze
                // a map that 'configureEach' has not filled yet, and that is why the service used to
                // be registered once per test task.
                //
                // On a build restored from the configuration cache this never runs. It does not have
                // to: the subscription made when the entry was stored, isolated map and all, is part
                // of what was stored.
                val compatibility = sharedServices.testBuildCacheTeamCityCompatibilityRegistration

                // Test tasks that were configured without being scheduled are dropped, so that the
                // map the build carries - into its configuration cache entry among other places - is
                // this run's own.
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
 * The registration of the build's [TestBuildCacheTeamCityCompatibilityService], typed.
 *
 * Both halves of the registration are needed - the parameters to fill and the service itself to
 * subscribe - and this is where the one cast that types them lives.
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
