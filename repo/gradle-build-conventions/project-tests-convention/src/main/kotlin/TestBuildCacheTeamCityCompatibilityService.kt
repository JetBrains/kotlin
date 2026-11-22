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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Reports the tests of the test tasks served from the Gradle Build Cache to TeamCity.
 *
 * One service for the whole build, holding the `test-executions.jsonl` of every test task in the
 * graph; when it is subscribed to task completions is load bearing, see [configureTestInventory].
 * What is reported about a recording is [TestExecutionsReplay]'s; this is when, and where to.
 *
 * After a task's messages comes a settle: the build, and so the process its step runs, is kept
 * alive a little longer so that the reader behind the step's output can drain what the replay
 * left in the buffers between the processes - which the TeamCity Docker wrapper otherwise
 * discards when the process exits. The settle is in proportion to what was written and bounded
 * per build, so that the thread it blocks still drains well inside the time the build gives it,
 * see [settleMillisFor].
 */
abstract class TestBuildCacheTeamCityCompatibilityService :
    BuildService<TestBuildCacheTeamCityCompatibilityService.Parameters>,
    OperationCompletionListener {

    interface Parameters : BuildServiceParameters {
        /** Path of a test task to the `test-executions.jsonl` recorded for it. */
        val executionsFiles: MapProperty<String, File>
    }

    private val log = Logging.getLogger(javaClass)

    /** Read once: every event wants the same map, and reading it finalizes the parameters. */
    private val executionsFiles: Map<String, File> by lazy { parameters.executionsFiles.get() }

    /**
     * One flow number per replayed task, so that the flow ids stay short: a flow id is a label
     * nothing outside its build refers to, and it rides on every message of a replay that runs
     * six figures of them.
     */
    private val flowNumbers = AtomicInteger()

    /** What this build's settles have taken between them, against the budget in [settleMillisFor]. */
    private val settleSpent = AtomicLong()

    override fun onFinish(event: FinishEvent) {
        if (event !is TaskFinishEvent) return
        val taskPath = event.descriptor.taskPath
        val executionsFile = executionsFiles[taskPath] ?: return
        val result = event.result as? TaskSuccessResult ?: return
        if (!result.isFromCache && !result.isUpToDate) return

        val flowId = "R${flowNumbers.incrementAndGet()}"
        val replay = TestExecutionsReplay(taskPath, flowId) { message, failure -> log.warn(message, failure) }
        var count = 0
        var emitted = 0L
        for (message in replay.messagesFor(executionsFile)) {
            println("##teamcity[$message]")
            emitted += message.length + 13 // the '##teamcity[' and ']' wrapper and the newline
            count++
        }

        // Through the same 'println' the messages go through, and last, so that a log missing it is
        // a log whose replay was cut short - which TeamCity does to the tail of a large one.
        // The flow is named so a short id stays traceable to its task from the log alone.
        println("TestReplay: $count messages of $taskPath emitted in flow $flowId")

        // Sleeping here was tried on 2026-10-03 and failed builds: a flat 5 s per task outlasted
        // the sixty seconds the build gives this thread to drain, and the builds that failed
        // replayed the most tasks. That is what the budget in settleMillisFor is for - what
        // sleeps is what was written, capped per task and per build - and what sleeps for
        // nothing, a replay that reported nothing, is nothing.
        val settleMillis = settleMillisFor(emitted, settleSpent.get())
        if (settleMillis > 0) {
            println("TestReplay: settled $settleMillis ms after $taskPath")
            Thread.sleep(settleMillis)
            settleSpent.addAndGet(settleMillis)
        }
    }
}

/** The rate the TeamCity agent ingests a step's output, characters per millisecond (~382 KiB/s, measured on CI). */
private const val INGESTION_CHARS_PER_MS = 391L

/** The longest settle that can deliver anything: the buffers between the processes hold about a megabyte. */
private const val MAX_SETTLE_MILLIS = 3_000L

/** What the settles of one build may take between them: half of the drain time the build gives its event thread. */
private const val SETTLE_BUDGET_MILLIS = 30_000L

/**
 * How long to keep the build alive after a replay that wrote [emittedChars] characters.
 *
 * TeamCity's Docker wrapper discards whatever still sits in the buffers between the build's
 * process and the agent when the process exits - moby closes an exec's streams two seconds
 * after the exec'd process exits - and a replay can leave about a megabyte in them. The agent
 * ingests a step's output at ~382 KiB/s, so the sleep is the time that output needs to drain:
 * proportional to what there is of it, and a replay that wrote nothing sleeps nothing.
 *
 * No single settle runs longer than the buffers can hold, about three seconds' worth at that
 * rate - a bigger one cannot deliver more than they contain. And no build settles for more
 * than half of the sixty seconds it gives the one thread its events run on, the rest being
 * the printing itself, which happens on the same thread: [alreadySpentMillis] is what this
 * build's earlier settles took, so the budget is shared over the tasks that need it.
 */
internal fun settleMillisFor(emittedChars: Long, alreadySpentMillis: Long): Long =
    (emittedChars / INGESTION_CHARS_PER_MS)
        .coerceAtMost(MAX_SETTLE_MILLIS)
        .coerceAtMost(SETTLE_BUDGET_MILLIS - alreadySpentMillis)
        .coerceAtLeast(0)
