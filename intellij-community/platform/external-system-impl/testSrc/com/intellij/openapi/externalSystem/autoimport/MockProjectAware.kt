// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus.SUCCESS
import com.intellij.openapi.externalSystem.autoimport.MockProjectAware.RefreshCollisionPassType.*
import com.intellij.openapi.observable.operations.AnonymousParallelOperationTrace
import com.intellij.openapi.observable.operations.AnonymousParallelOperationTrace.Companion.task
import com.intellij.openapi.util.Disposer
import com.intellij.util.ConcurrencyUtil.once
import com.intellij.util.EventDispatcher
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.LinkedHashSet

class MockProjectAware(override val projectId: ExternalSystemProjectId) : ExternalSystemProjectAware {

  val subscribeCounter = AtomicInteger(0)
  val unsubscribeCounter = AtomicInteger(0)
  val refreshCounter = AtomicInteger(0)

  val refreshCollisionPassType = AtomicReference(DUPLICATE)
  val refreshStatus = AtomicReference(SUCCESS)

  private val eventDispatcher = EventDispatcher.create(Listener::class.java)
  private val refresh = AnonymousParallelOperationTrace(debugName = "$projectId MockProjectAware.refreshProject")

  override val settingsFiles = LinkedHashSet<String>()

  override fun subscribe(listener: ExternalSystemProjectRefreshListener, parentDisposable: Disposable) {
    eventDispatcher.addListener(listener.asListener(), parentDisposable)
    subscribeCounter.incrementAndGet()
    Disposer.register(parentDisposable, Disposable { unsubscribeCounter.incrementAndGet() })
  }

  override fun reloadProject(context: ExternalSystemProjectReloadContext) {
    when (refreshCollisionPassType.get()!!) {
      DUPLICATE -> {
        doRefreshProject(context)
      }
      CANCEL -> {
        val task = once { doRefreshProject(context) }
        refresh.afterOperation { task.run() }
        if (refresh.isOperationCompleted()) task.run()
      }
      IGNORE -> {
        if (refresh.isOperationCompleted()) {
          doRefreshProject(context)
        }
      }
    }
  }

  private fun doRefreshProject(context: ExternalSystemProjectReloadContext) {
    val refreshStatus = refreshStatus.get()
    eventDispatcher.multicaster.beforeProjectRefresh()
    refresh.task {
      refreshCounter.incrementAndGet()
      eventDispatcher.multicaster.insideProjectRefresh(context)
    }
    eventDispatcher.multicaster.afterProjectRefresh(refreshStatus)
  }

  fun onceDuringRefresh(action: (ExternalSystemProjectReloadContext) -> Unit) {
    val disposable = Disposer.newDisposable()
    duringRefresh(disposable) {
      Disposer.dispose(disposable)
      action(it)
    }
  }

  fun duringRefresh(parentDisposable: Disposable, action: (ExternalSystemProjectReloadContext) -> Unit) {
    eventDispatcher.addListener(object : Listener {
      override fun insideProjectRefresh(context: ExternalSystemProjectReloadContext) = action(context)
    }, parentDisposable)
  }

  private fun ExternalSystemProjectRefreshListener.asListener(): Listener {
    return object : Listener, ExternalSystemProjectRefreshListener {
      override fun beforeProjectRefresh() {
        this@asListener.beforeProjectRefresh()
      }

      override fun afterProjectRefresh(status: ExternalSystemRefreshStatus) {
        this@asListener.afterProjectRefresh(status)
      }
    }
  }

  interface Listener : ExternalSystemProjectRefreshListener, EventListener {
    @JvmDefault
    fun insideProjectRefresh(context: ExternalSystemProjectReloadContext) {
    }
  }

  enum class RefreshCollisionPassType { DUPLICATE, CANCEL, IGNORE }
}