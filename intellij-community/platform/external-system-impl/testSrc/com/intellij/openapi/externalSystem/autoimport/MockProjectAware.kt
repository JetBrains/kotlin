// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class MockProjectAware(override val projectId: ExternalSystemProjectId) : ExternalSystemProjectAware {

  val subscribeCounter = AtomicInteger(0)
  val unsubscribeCounter = AtomicInteger(0)
  val refreshCounter = AtomicInteger(0)

  var refreshStatus = ExternalSystemRefreshStatus.SUCCESS

  private val listeners = CopyOnWriteArrayList<ExternalSystemProjectRefreshListener>()
  private val inRefreshActions = CopyOnWriteArrayList<() -> Unit>()

  override val settingsFiles = LinkedHashSet<String>()

  override fun subscribe(listener: ExternalSystemProjectRefreshListener, parentDisposable: Disposable) {
    if (listeners.add(listener)) {
      subscribeCounter.incrementAndGet()
    }
    Disposer.register(parentDisposable, Disposable {
      if (listeners.remove(listener)) {
        unsubscribeCounter.incrementAndGet()
      }
    })
  }

  override fun refreshProject() {
    listeners.forEach { it.beforeProjectRefresh() }
    refreshCounter.incrementAndGet()
    inRefreshActions.forEach { it() }
    listeners.forEach { it.afterProjectRefresh(refreshStatus) }
  }

  fun onceDuringRefresh(action: () -> Unit) {
    val inRefreshAction = object : (() -> Unit) {
      override fun invoke() {
        action()
        inRefreshActions.remove(this)
      }
    }
    inRefreshActions.add(inRefreshAction)
  }
}