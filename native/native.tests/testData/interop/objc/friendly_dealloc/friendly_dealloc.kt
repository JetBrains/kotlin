/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import friendly_dealloc.*
import platform.objc.*
import platform.Foundation.*

import kotlin.concurrent.*
import kotlin.native.concurrent.*
import kotlin.native.internal.test.testLauncherEntryPoint
import kotlin.system.exitProcess
import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.*

val timeout = 10.seconds

fun <T> allocCollectable(ctor: () -> T): T = autoreleasepool {
    ctor()
}

class Event() : NSObject() {
    @Volatile
    private var triggered = false

    fun isTriggered() = triggered

    @ObjCAction
    fun trigger() {
        assertFalse(isTriggered())
        triggered = true;
    }
}

val trigerSelector = sel_registerName("trigger")

fun waitTriggered(event: Event) {
    val timeoutMark = TimeSource.Monotonic.markNow() + timeout

    kotlin.native.internal.GC.collect()
    while (true) {
        if (event.isTriggered()) {
            return
        }
        assertFalse(timeoutMark.hasPassedNow(), "Timed out")
    }
}

// All the tests are run on a secondary (non main) thread in order to prevent `objcDisposeOnMain` hacks from messing things up
@Test
fun testAutorelease() {
    val event = withWorker {
        execute(TransferMode.SAFE, {}) {
            val event = Event()
            assertFalse(event.isTriggered())

            val victimId = allocCollectable {
                val v = OnDestroyHook {
                    event.trigger()
                }
                retain(v.identity())
                v.identity()
            }

            allocCollectable {
                OnDestroyHook {
                    autorelease(victimId)
                }.identity()
            }

            event
        }.result
    }
    waitTriggered(event)
}

@Test
fun testTimer() {
    val event = Event()

    withWorker {
        execute(TransferMode.SAFE, { event }) { event ->
            allocCollectable {
                OnDestroyHook {
                    assertFalse(event.isTriggered())
                    NSTimer.scheduledTimerWithTimeInterval(0.0, target=event, selector=trigerSelector, userInfo=null, repeats=false)
                }.identity()
            }
        }
    }

    waitTriggered(event)
}

@Test
fun testSelector() {
    val event = Event()

    withWorker {
        execute(TransferMode.SAFE, { event }) { event ->
            allocCollectable {
                OnDestroyHook {
                    assertFalse(event.isTriggered())
                    NSRunLoop.currentRunLoop().performSelector(trigerSelector, target=event, argument=null, order=0, modes=listOf(NSDefaultRunLoopMode))
                }.identity()
            }
        }
    }

    waitTriggered(event)
}

@Test
fun testPerformBlock() {
    val event = Event()

    withWorker {
        execute(TransferMode.SAFE, { event }) { event ->
            allocCollectable {
                OnDestroyHook {
                    NSRunLoop.currentRunLoop().performBlock({
                        assertFalse(event.isTriggered())
                        event.trigger()
                    });
                }.identity()
            }
        }
    }

    waitTriggered(event)
}
