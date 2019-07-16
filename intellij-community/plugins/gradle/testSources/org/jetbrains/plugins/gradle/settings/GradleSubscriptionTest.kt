// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings

import com.intellij.openapi.util.Disposer
import org.junit.Test

class GradleSubscriptionTest : GradleSubscriptionTestCase() {

  @Test
  fun `test external system subscription`() {
    var linkingCounter = 0
    onProjectLinked {
      linkingCounter += 1
    }
    linkProject()
    unlinkProject()
    linkProject()
    assertEquals(2, linkingCounter)
  }

  @Test
  fun `test external system unsubscription`() {
    var linkingCounter = 0
    val subscription = Disposer.newDisposable()
    onProjectLinked(subscription) {
      linkingCounter += 1
      Disposer.dispose(subscription)
    }
    linkProject()
    unlinkProject()
    linkProject()
    assertEquals(1, linkingCounter)
  }
}