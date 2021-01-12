// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import java.util.concurrent.atomic.AtomicInteger

class ProjectAwareWrapper(val delegate: ExternalSystemProjectAware) : ExternalSystemProjectAware by delegate {
  val subscribeCounter = AtomicInteger(0)
  val unsubscribeCounter = AtomicInteger(0)
  val refreshCounter = AtomicInteger(0)
  val beforeRefreshCounter = AtomicInteger(0)
  val afterRefreshCounter = AtomicInteger(0)
  override val settingsFiles = delegate.settingsFiles

  override fun subscribe(listener: ExternalSystemProjectRefreshListener, parentDisposable: Disposable) {
    val listenerWrapper = object : ExternalSystemProjectRefreshListener {
      override fun beforeProjectRefresh() {
        beforeRefreshCounter.incrementAndGet()
        listener.beforeProjectRefresh()
      }

      override fun afterProjectRefresh(status: ExternalSystemRefreshStatus) {
        afterRefreshCounter.incrementAndGet()
        listener.afterProjectRefresh(status)
      }
    }
    delegate.subscribe(listenerWrapper, parentDisposable)
    subscribeCounter.incrementAndGet()
    Disposer.register(parentDisposable, Disposable { unsubscribeCounter.incrementAndGet() })
  }

  override fun reloadProject(context: ExternalSystemProjectReloadContext) {
    refreshCounter.incrementAndGet()
    delegate.reloadProject(context)
  }
}