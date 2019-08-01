// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.ui

import com.intellij.openapi.Disposable
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors


class MockPropertyFactory : Disposable {

  private lateinit var thread: Thread

  private val coroutineDispatcher: ExecutorCoroutineDispatcher

  private val jobs = ConcurrentLinkedQueue<Job>()

  fun <T> property(initial: () -> T): Property<T> = MockProperty(initial)

  fun assertIsExecutionThread() {
    val currentThread = Thread.currentThread()
    if (currentThread != thread) {
      println(currentThread.name)
      println(thread.name)
      throw AssertionError()
    }
  }

  fun invokeLater(action: () -> Unit) {
    val job = GlobalScope.launch(coroutineDispatcher) {
      action()
    }
    jobs.add(job)
  }

  fun waitAll() {
    runBlocking {
      while (jobs.isNotEmpty()) {
        jobs.remove().join()
      }
    }
  }

  override fun dispose() {
    assert(jobs.isEmpty())
    coroutineDispatcher.close()
  }

  init {
    val executionService = Executors.newSingleThreadExecutor { runnable ->
      Thread(runnable).also {
        thread = it
      }
    }
    coroutineDispatcher = executionService.asCoroutineDispatcher()
  }

  private inner class MockProperty<T>(initial: () -> T) : Property<T>(initial) {
    override fun assertIsExecutionThread() = this@MockPropertyFactory.assertIsExecutionThread()

    override fun invokeLater(action: () -> Unit) = this@MockPropertyFactory.invokeLater(action)
  }
}