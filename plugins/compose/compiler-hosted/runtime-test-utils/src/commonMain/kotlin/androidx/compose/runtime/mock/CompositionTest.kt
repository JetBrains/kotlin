/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.runtime.mock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeRuntimeFlags
import androidx.compose.runtime.Composition
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.CompositionObserverHandle
import androidx.compose.runtime.tooling.setObserver
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

enum class ComposerToUse {
    Gap,
    Link,
    Both,
}

@OptIn(InternalComposeApi::class, ExperimentalCoroutinesApi::class, ExperimentalComposeApi::class)
fun compositionTest(
    composerToUse: ComposerToUse = ComposerToUse.Both,
    clock: MonotonicFrameClock? = null,
    block: suspend CompositionTestScope.() -> Unit,
) = runTest {
    suspend fun TestScope.runComposerTest() {
        withContext(clock ?: TestMonotonicFrameClock(this)) {
            // Start the recomposer
            val recomposer = Recomposer(coroutineContext)
            launch { recomposer.runRecomposeAndApplyChanges() }
            testScheduler.runCurrent()

            // Create a test scope for the test using the test scope passed in by runTest
            val scope =
                object : CompositionTestScope, CoroutineScope by this@runTest {
                    var composed = false
                    override var composition: Composition? = null

                    override lateinit var root: View

                    override val testCoroutineScheduler: TestCoroutineScheduler
                        get() = this@runTest.testScheduler

                    override fun compose(block: @Composable () -> Unit) {
                        check(!composed) { "Compose should only be called once" }
                        composed = true
                        root = View().apply { name = "root" }
                        val composition = Composition(ViewApplier(root), recomposer)
                        this.composition = composition
                        composition.setContent(block)
                    }

                    override fun hasPendingWork(): Boolean {
                        return recomposer.hasPendingWork
                    }

                    @OptIn(ExperimentalComposeRuntimeApi::class)
                    override fun compose(
                        observer: CompositionObserver,
                        block: @Composable () -> Unit,
                    ): CompositionObserverHandle? {
                        check(!composed) { "Compose should only be called once" }
                        composed = true
                        root = View().apply { name = "root" }
                        val composition = Composition(ViewApplier(root), recomposer)
                        val result = composition.setObserver(observer)
                        this.composition = composition
                        composition.setContent(block)
                        return result
                    }

                    override fun advanceCount(ignorePendingWork: Boolean): Long {
                        val changeCount = recomposer.changeCount
                        Snapshot.sendApplyNotifications()
                        if (recomposer.hasPendingWork) {
                            advanceTimeBy(5_000)
                            check(ignorePendingWork || !recomposer.hasPendingWork) {
                                "Potentially infinite recomposition, still recomposing after advancing"
                            }
                        }
                        return recomposer.changeCount - changeCount
                    }

                    override fun advanceTimeBy(amount: Long) = testScheduler.advanceTimeBy(amount)

                    override fun advance(ignorePendingWork: Boolean) =
                        advanceCount(ignorePendingWork) != 0L

                    override fun verifyConsistent() {
                        (composition as? ControlledComposition)?.verifyConsistent()
                    }

                    override var validator: (MockViewValidator.() -> Unit)? = null
                }
            scope.block()

            try {
                scope.composition?.dispose()
            } catch (_: Throwable) {
                // suppress
            } finally {
                scope.composition = null
                recomposer.cancel()
                recomposer.join()
            }
        }
    }

    suspend fun TestScope.runTestWithComposer(composerToUse: ComposerToUse) {
        val previousValue = ComposeRuntimeFlags.isLinkBufferComposerEnabled
        ComposeRuntimeFlags.isLinkBufferComposerEnabled = composerToUse == ComposerToUse.Link
        try {
            runComposerTest()
        } finally {
            ComposeRuntimeFlags.isLinkBufferComposerEnabled = previousValue
        }
    }

    // Wrappers so that the composer tested shows up in the stack trace if it fails

    suspend fun TestScope.runGapComposerTest() {
        runTestWithComposer(ComposerToUse.Gap)
    }

    suspend fun TestScope.runLinkComposerTest() {
        runTestWithComposer(ComposerToUse.Link)
    }

    when (composerToUse) {
        ComposerToUse.Gap -> runGapComposerTest()
        ComposerToUse.Link -> runLinkComposerTest()
        ComposerToUse.Both -> {
            val gapError = runCatching { runGapComposerTest() }.exceptionOrNull()
            val linkError = runCatching { runLinkComposerTest() }.exceptionOrNull()

            if (gapError != null && linkError != null) {
                throw AssertionError(
                    """
                    |Test failed under both composers.
                    |GapComposer error: ${gapError.stackTraceToString()}
                    |
                    |LinkComposer error: ${linkError.stackTraceToString()}
                """
                        .trimMargin()
                )
            } else if (gapError != null) {
                throw AssertionError(
                    "Test failed under the GapComposer: ${gapError.message}",
                    gapError,
                )
            } else if (linkError != null) {
                throw AssertionError(
                    "Test failed under the LinkComposer: ${linkError.message}",
                    linkError,
                )
            }
        }
    }
}

/** A test scope used in tests that allows controlling and testing composition. */
@OptIn(ExperimentalCoroutinesApi::class)
interface CompositionTestScope : CoroutineScope {

    /** A scheduler used by [CoroutineScope] */
    val testCoroutineScheduler: TestCoroutineScheduler

    /** Compose a block using the mock view composer. */
    fun compose(block: @Composable () -> Unit)

    /** Compose a block observed using the mock view composer. */
    @OptIn(ExperimentalComposeRuntimeApi::class)
    fun compose(
        observer: CompositionObserver,
        block: @Composable () -> Unit,
    ): CompositionObserverHandle?

    fun hasPendingWork(): Boolean

    /**
     * Advance the state which executes any pending compositions, if any. Returns true if advancing
     * resulted in changes being applied.
     */
    fun advance(ignorePendingWork: Boolean = false): Boolean

    /** Advance counting the number of time the recomposer ran. */
    fun advanceCount(ignorePendingWork: Boolean = false): Long

    /** Advance the clock by [amount] ms */
    fun advanceTimeBy(amount: Long)

    /** Verify the composition is well-formed. */
    fun verifyConsistent()

    /** The root mock view of the mock views being composed. */
    val root: View

    /** The last validator used. */
    var validator: (MockViewValidator.() -> Unit)?

    /** Access to the composition created for the call to [compose] */
    var composition: Composition?
}

/** Create a mock view validator and validate the view. */
fun CompositionTestScope.validate(block: MockViewValidator.() -> Unit) =
    MockViewListValidator(root.children).validate(block).also { validator = block }

/** Revalidate using the last validator */
fun CompositionTestScope.revalidate() = validate(validator ?: error("validate was not called"))

/** Advance and expect changes */
fun CompositionTestScope.expectChanges() {
    val changes = advance()
    assertTrue(actual = changes, message = "Expected changes but none were found")
}

/** Advance and expect no changes */
fun CompositionTestScope.expectNoChanges() {
    val changes = advance()
    assertFalse(actual = changes, message = "Expected no changes but changes occurred")
}
