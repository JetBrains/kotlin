// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing

import com.intellij.openapi.application.ApplicationManager
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

fun createThread(task: () -> Unit): ThreadState = ThreadState(task)

/**
 * Utility class helping to write concurrency tests.
 * Allows to control threads execution schedule:
 * - [waitForStart] starts the thread in a background thread and waits in the calling thread until the thread executes the first instruction
 * - [waitForBlock] waits in the calling thread until this thread gets blocked on an operation (e.g. on acquiring a lock)
 * - [waitForFinish] waits in the calling thread until this thread executes the last instruction
 */
class ThreadState(private val task: () -> Unit) {

  private val thread = AtomicReference<Thread>()

  private val started = AtomicBoolean()

  private val finished = AtomicBoolean()

  private val failed = AtomicReference<Throwable>()

  private val alreadyStarted = AtomicBoolean()

  fun waitForStart(timeLimit: Long = 1000) {
    check(alreadyStarted.compareAndSet(false, true)) { "Thread must not be started twice" }
    ApplicationManager.getApplication().executeOnPooledThread {
      thread.set(Thread.currentThread())
      started.set(true)
      try {
        task()
      }
      catch (e: Throwable) {
        failed.set(e)
      }
      finally {
        finished.set(true)
      }
    }
    waitWithLimit(timeLimit, "Thread has not started in $timeLimit ms") { started.get() }
  }

  fun waitForBlock(blockTime: Long = 100, timeLimit: Long = 1000) {
    checkStarted()
    checkNotFailed()
    checkNotFinished()

    val thread = thread.get()
    waitWithLimit(timeLimit, "Thread has not blocked in $timeLimit ms") {
      checkNotFailed()
      checkNotFinished()

      val oneStackTrace: Array<StackTraceElement> = thread.stackTrace
      Thread.sleep(blockTime)
      val twoStackTrace = thread.stackTrace

      //Stack trace hasn't changed in [blockTime] => the thread is blocked.
      oneStackTrace.contentEquals(twoStackTrace)
    }
  }

  fun waitForFinish(timeLimit: Long = 1000) {
    checkStarted()
    waitWithLimit(timeLimit, "Thread hasn't finished in $timeLimit ms") {
      checkNotFailed()
      finished.get()
    }
  }

  private fun checkStarted() {
    check(started.get() && thread.get() != null) { "Thread has not been started yet. Call waitForStart." }
  }

  private fun checkNotFailed() {
    if (failed.get() != null) {
      throw failed.get()
    }
  }

  private fun checkNotFinished() {
    check(!finished.get()) { "Thread has finished earlier than expected" }
  }

}

private fun waitWithLimit(timeLimit: Long, message: String, condition: () -> Boolean) {
  val startTime = System.currentTimeMillis()
  while (!condition()) {
    check(System.currentTimeMillis() - startTime <= timeLimit) { message }
    Thread.sleep(10)
  }
}