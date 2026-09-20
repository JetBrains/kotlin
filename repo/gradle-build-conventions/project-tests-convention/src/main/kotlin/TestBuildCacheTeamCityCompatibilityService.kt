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
 * Reports the tests of the test tasks served from the Gradle Build Cache to TeamCity.
 *
 * One service serves the whole build, holding the `test-executions.json` of every test task in the
 * task graph. When to subscribe it to task completions is load bearing, see [configureTestInventory].
 *
 * What is reported about a recording is [TestExecutionsReplay]'s; this is when, and where to.
 */
abstract class TestBuildCacheTeamCityCompatibilityService :
    BuildService<TestBuildCacheTeamCityCompatibilityService.Parameters>,
    OperationCompletionListener {

    interface Parameters : BuildServiceParameters {
        /** Path of a test task to the `test-executions.json` recorded for it. */
        val executionsFiles: MapProperty<String, File>
    }

    private val log = Logging.getLogger(javaClass)

    /** Held rather than read per event: the map is the same for all of them, and reading it finalizes it. */
    private val executionsFiles: Map<String, File> by lazy { parameters.executionsFiles.get() }

    override fun onFinish(event: FinishEvent) {
        if (event !is TaskFinishEvent) return
        val taskPath = event.descriptor.taskPath
        val executionsFile = executionsFiles[taskPath] ?: return
        val result = event.result as? TaskSuccessResult ?: return
        if (!result.isFromCache && !result.isUpToDate) return

        val replay = TestExecutionsReplay(taskPath) { message, failure -> log.warn(message, failure) }
        for (message in replay.messagesFor(executionsFile)) {
            println("##teamcity[$message]")
        }
    }
}
