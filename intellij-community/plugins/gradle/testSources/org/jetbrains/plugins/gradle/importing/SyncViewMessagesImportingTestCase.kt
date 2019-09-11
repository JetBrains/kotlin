// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.build.*
import com.intellij.openapi.project.Project
import com.intellij.testFramework.replaceService

abstract class SyncViewMessagesImportingTestCase : GradleImportingTestCase() {

  protected lateinit var syncViewManager: TestSyncViewManager

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myProject.replaceService(BuildContentManager::class.java, BuildContentManagerImpl(myProject), testRootDisposable)
    syncViewManager = TestSyncViewManager(myProject)
    myProject.replaceService(SyncViewManager::class.java, syncViewManager, testRootDisposable)
  }

  protected class TestSyncViewManager(project: Project) :
    SyncViewManager(project) {
    public override fun getBuildsMap(): MutableMap<BuildDescriptor, BuildView> {
      return super.getBuildsMap()
    }
  }

  override fun handleImportFailure(errorMessage: String, errorDetails: String?) {
    // do not fail tests with failed builds
  }
}
