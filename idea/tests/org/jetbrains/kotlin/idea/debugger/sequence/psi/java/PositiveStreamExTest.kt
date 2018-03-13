// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.psi.java

import org.jetbrains.kotlin.idea.debugger.sequence.KotlinPsiChainBuilderTestCase
import org.jetbrains.kotlin.idea.debugger.sequence.LibraryUtil
import org.jetbrains.kotlin.idea.debugger.sequence.lib.java.StreamExLibrarySupportProvider
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.PsiTestUtil

/**
 * @author Vitaliy.Bibaev
 */
class PositiveStreamExTest : KotlinPsiChainBuilderTestCase.Positive("streams/positive/streamex") {
  override val kotlinChainBuilder: StreamChainBuilder = StreamExLibrarySupportProvider().chainBuilder
  override fun setUp() {
    super.setUp()
    ApplicationManager.getApplication().runWriteAction {
      VfsRootAccess.allowRootAccess(LibraryUtil.LIBRARIES_DIRECTORY)
      PsiTestUtil.addLibrary(testRootDisposable, LightPlatformTestCase.getModule(),
                             "StreamEx", LibraryUtil.LIBRARIES_DIRECTORY, "streamex-0.6.5.jar")
    }
  }

  override fun tearDown() {
    super.tearDown()
    ApplicationManager.getApplication().runWriteAction {
      VfsRootAccess.disallowRootAccess(LibraryUtil.LIBRARIES_DIRECTORY)
    }
  }

  fun testSimple() {
    doTest()
  }
}
