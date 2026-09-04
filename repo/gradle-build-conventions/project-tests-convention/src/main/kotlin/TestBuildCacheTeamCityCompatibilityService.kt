/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.logging.Logging
import org.gradle.api.provider.MapProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSuccessResult
import java.io.File

/**
 * Receives a [TaskFinishEvent] for every task executed in the build, and reacts to those that are
 * test tasks of the project this service was registered for.
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
         * Maps a test task path (e.g. `:compiler:test`) to the `test-inventory.tsv` file that task
         * declares as an output.
         *
         * A task finish event carries only a task path, never a task type, so this mapping has to be
         * collected while the project is configured. It is passed as a *parameter* on purpose: build
         * service parameters are stored in the configuration cache, while build service state is not,
         * so anything kept in a field would silently be empty on a configuration cache hit.
         */
        val inventoryFiles: MapProperty<String, File>
    }

    private val log = Logging.getLogger(javaClass)

    // Resolved once: 'parameters' are final by the time the first event arrives.
    private val inventoryFiles: Map<String, File> by lazy { parameters.inventoryFiles.get() }

    // Note: 'onFinish' receives the general 'FinishEvent'. Only task events are of interest here,
    // so everything else (e.g. transform or test events) is filtered out.
    override fun onFinish(event: FinishEvent) {
        if (event !is TaskFinishEvent) return

        val taskPath = event.descriptor.taskPath
        // Events are delivered for the whole build, so tasks of other projects are skipped here,
        // as are non-test tasks, which are never part of the mapping.
        val inventoryFile = inventoryFiles[taskPath] ?: return

        val result = event.result
        val fromCache = result is TaskSuccessResult && result.isFromCache
        val upToDate = result is TaskSuccessResult && result.isUpToDate

        // TODO: perform the actual build cache / TeamCity compatibility check here.
        log.info("Test task $taskPath finished: fromCache=$fromCache, upToDate=$upToDate, inventory=$inventoryFile")
    }
}
