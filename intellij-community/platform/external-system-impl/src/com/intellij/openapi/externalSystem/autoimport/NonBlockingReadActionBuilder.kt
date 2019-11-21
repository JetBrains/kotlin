// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.util.concurrency.AppExecutorUtil

internal class NonBlockingReadActionBuilder<R>(private val action: () -> R) {

  private lateinit var finishAction: (R) -> Unit

  fun finishOnUiThread(action: (R) -> Unit): NonBlockingReadActionBuilder<R> {
    finishAction = action
    return this
  }

  fun submit(parentDisposable: Disposable) {
    if (ApplicationManager.getApplication().isHeadlessEnvironment) {
      finishAction(action())
      return
    }
    ReadAction.nonBlocking(action)
      .expireWith(parentDisposable)
      .finishOnUiThread(ModalityState.defaultModalityState(), finishAction)
      .submit(AppExecutorUtil.getAppExecutorService())
  }

  companion object {
    fun <R> nonBlockingReadAction(action: () -> R) = NonBlockingReadActionBuilder(action)
  }
}