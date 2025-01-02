/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.lazy

import samples.*
import kotlin.concurrent.thread

@Suppress("unused")
class LazySamples {

    @Sample
    fun lazySample() {
        val answer: Int by lazy {
            println("Computing the answer to the Ultimate Question of Life, the Universe, and Everything")
            42
        }

        println("What is the answer?")
        // Will print 'Computing...' and then 42
        assertPrints(answer, "42")
        println("Come again?")
        // Will just print 42
        assertPrints(answer, "42")
    }

    @Sample
    fun lazyExplicitSample() {
        val answer: Lazy<Int> = lazy {
            println("Computing the answer to the Ultimate Question of Life, the Universe, and Everything")
            42
        }

        println("What is the answer?")
        assertPrints(answer, "Lazy value not initialized yet.")
        println("Oh, now compute it please")
        // Will print 'Computing...' and then 42
        assertPrints(answer.value, "42")
        println("Come again?")
        // Will print 42
        assertPrints(answer.value, "42")

    }

    fun lazySynchronizedSample() {
        val answer: Int by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            println("Computing the answer to the Ultimate Question of Life, the Universe, and Everything")
            42
        }

        val t1 = thread {
            println("Thread 1: $answer")
        }

        val t2 = thread {
            println("Thread 2: $answer")
        }

        // It is guaranteed that 'Computing' message will be printed once, but both threads will see 42 as an answer
        t1.join()
        t2.join()
    }

    fun lazySafePublicationSample() {
        class Answer(val value: Int, val computedBy: Thread = Thread.currentThread()) {
            override fun toString() = "Answer: $value, computed by: $computedBy"
        }

        val answer: Answer by lazy(LazyThreadSafetyMode.PUBLICATION) {
            println("Computing the answer to the Ultimate Question of Life, the Universe, and Everything")
            Answer(42)
        }

        val t1 = thread(name = "#1") {
            println("Thread 1: $answer")
        }

        val t2 = thread(name = "#2") {
            println("Thread 2: $answer")
        }

        // It is **not** guaranteed that 'Computing' message will be printed once,
        // but guaranteed that both threads will see 42 computed by *the same* thread as an answer
        t1.join()
        t2.join()
    }

    fun explicitLockLazySample() {
        val lock = Any()
        val answer: Int by lazy(lock) {
            println("Computing the answer to the Ultimate Question of Life, the Universe, and Everything")
            42
        }

        // Lock is acquired first, so thread cannot compute the answer
        val thread: Thread
        synchronized(lock) {
            thread = thread(name = "#1") {
                println("Thread is asking for an answer")
                println("$answer")
            }
            println("Let's hold the thread #1 for a while with a lock")
            Thread.sleep(100) // Let it wait
        }
        // Lock is unlocked, the thread will print an answer
        thread.join()
    }
}
