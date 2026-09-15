/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSuccessResult
import java.io.File

/**
 * Receives a [TaskFinishEvent] for every task executed in the build, and reacts to the one test task
 * this service instance was registered for.
 *
 * Registered as a build service (rather than a plain object) because [OperationCompletionListener]
 * implementations must be build services to be accepted by
 * [org.gradle.build.event.BuildEventsListenerRegistry.onTaskCompletion].
 */
abstract class TestBuildCacheTeamCityCompatibilityService :
    BuildService<TestBuildCacheTeamCityCompatibilityService.Parameters>,
    OperationCompletionListener {

    interface Parameters : BuildServiceParameters {
        /**
         * Path of the test task this service watches, e.g. `:compiler:test`. A task finish event carries
         * only a task path, never a task type, so the path has to be recorded while the task is configured.
         */
        val taskPath: Property<String>

        /**
         * The `test-inventory.tsv` file the watched task declares as an output.
         *
         * Passed as a *parameter* on purpose: build service parameters are stored in the configuration
         * cache, while build service state is not, so anything kept in a field would silently be empty
         * on a configuration cache hit.
         */
        val inventoryFile: Property<File>
    }

    private val log = Logging.getLogger(javaClass)

    // Resolved once: 'parameters' are final by the time the first event arrives.
    private val taskPath: String by lazy { parameters.taskPath.get() }
    private val inventoryFile: File by lazy { parameters.inventoryFile.get() }

    // Note: 'onFinish' receives the general 'FinishEvent'. Only task events are of interest here,
    // so everything else (e.g. transform or test events) is filtered out.
    override fun onFinish(event: FinishEvent) {
        if (event !is TaskFinishEvent) return

        // Events are delivered for the whole build, so every task other than the watched one is skipped.
        if (event.descriptor.taskPath != taskPath) return

        val result = event.result
        val fromCache = result is TaskSuccessResult && result.isFromCache
        val upToDate = result is TaskSuccessResult && result.isUpToDate

        // TODO: perform the actual build cache / TeamCity compatibility check here. The inventory file
        // is readable at this point even when the task was served from the build cache: it is a declared
        // output of the task, so Gradle restores it along with the rest of the task's outputs.
        log.info(
            "Test task $taskPath finished: fromCache=$fromCache, upToDate=$upToDate, " +
                    "inventory=$inventoryFile (exists=${inventoryFile.isFile})"
        )
    }
}
