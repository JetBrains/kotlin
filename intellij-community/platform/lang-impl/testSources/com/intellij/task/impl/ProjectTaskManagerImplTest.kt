// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.task.impl

import com.intellij.task.ProjectTaskManagerTest.Companion.doTestDeprecatedApi
import com.intellij.task.ProjectTaskManagerTest.Companion.doTestNewApi
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.ui.UIUtil
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.isPending
import org.junit.Test
import java.util.concurrent.TimeUnit

class ProjectTaskManagerImplTest : LightPlatformTestCase() {

  @Test
  fun `test new api calls`() {
    doTestNewApi(ProjectTaskManagerImpl.getInstance(project), promiseHandler())
  }

  @Test
  fun `test deprecated api calls`() {
    doTestDeprecatedApi(ProjectTaskManagerImpl.getInstance(project), promiseHandler())
  }
}

private fun <T> promiseHandler(): (Promise<T?>) -> T? = {
  val startTime = System.currentTimeMillis()
  while (it.isPending) {
    val durationInSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime)
    check(durationInSeconds < 1) { "Project task result was not received after $durationInSeconds sec" }
    UIUtil.dispatchAllInvocationEvents()
  }
  it.blockingGet(10)
}