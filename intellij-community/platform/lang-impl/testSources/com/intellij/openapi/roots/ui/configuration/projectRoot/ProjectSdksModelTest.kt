// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration.projectRoot

import com.intellij.idea.TestFor
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SimpleJavaSdkType
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.util.Consumer
import com.intellij.util.ui.UIUtil
import org.junit.Assert
import org.junit.Test

class ProjectSdksModelTest : LightPlatformTestCase() {
  private val model = ProjectSdksModel()
  private val sdkType = SimpleJavaSdkType()

  @Test
  fun testAddedSdkIsClonedModifiable() {
    val sdk = model.createSdk(sdkType, "testJdskd123", "mock")
    model.addSdk(sdk)

    //we fix current behaviour of the code - the added Sdk is cloned
    //to be edited

    Assert.assertNotSame(sdk, model.findSdk(sdk.name))
    Assert.assertFalse(model.projectSdks.values.contains(sdk))
    Assert.assertTrue(model.projectSdks.keys.contains(sdk))
  }

  @Test
  fun testEditableSdkIsAddedToJdkTable() {
    val sdk = model.createSdk(sdkType, "testJdskd123", "mock")
    model.addSdk(sdk)
    model.apply()

    disposeOnTearDown(Disposable { runWriteAction { ProjectJdkTable.getInstance().removeJdk(sdk) } })

    val foundSdk = ProjectJdkTable.getInstance().allJdks.find { it.name == sdk.name }

    //we assume that the added Sdk is added to the ProjectJdkTable
    Assert.assertNotNull(foundSdk)
    Assert.assertSame(foundSdk, sdk)

    sdk.sdkModificator.apply {
      name = "newName"
    }.commitChanges()

    model.apply()

    val foundSdk2 = ProjectJdkTable.getInstance().allJdks.find { it.name == sdk.name }
    Assert.assertNotNull(foundSdk2)

    //the ProjectJdkTable should keep the same element as it was
    Assert.assertSame(foundSdk2, sdk)
  }

  private fun doDownloadTest(action: DownloadSdkTest.() -> Unit) {
    val test = DownloadSdkTest()
    test.action()
  }

  private inner class DownloadSdkTest {
    val type = SimpleJavaSdkType.getInstance()
    val plannedDir = createTempDir("planned-dir-")
    val sdkName = "test-name"

    fun task(action: () -> Unit) {
      runCatching(action).exceptionOrNull()?.let { addSuppressedException(it) }
    }

    init {
      disposeOnTearDown(Disposable {
        runWriteAction {
          val jdkTable = ProjectJdkTable.getInstance()
          jdkTable.allJdks.filter { it.name == sdkName }.forEach { jdkTable.removeJdk(it) }
        }
      })
    }

    fun doDownload(onSdk: (Sdk) -> Unit = {},
                   actualDownload: (ProgressIndicator) -> Unit) {
      var isSameCallStack = true

      try {
        model.setupInstallableSdk(type, object : SdkDownloadTask {
          override fun getSuggestedSdkName() = sdkName
          override fun getPlannedHomeDir() = plannedDir.absolutePath
          override fun getPlannedVersion() = "1.2.3"
          override fun doDownload(indicator: ProgressIndicator) {
            assertThat(ApplicationManager.getApplication().isDispatchThread).isFalse()

            //ProgressManager works in the same thread in tests
            assertThat(isSameCallStack).withFailMessage("Is should be in the same call stack!").isTrue()

            actualDownload(indicator)
          }
        }, Consumer(onSdk))
      }
      finally {
        isSameCallStack = false
        UIUtil.dispatchAllInvocationEvents()
      }
    }
  }

  @TestFor(issues = ["IDEA-229737"])
  fun testCancelDownloadingTaskBeforeCommit() = doDownloadTest {
    doDownload { indicator ->
      task {
        //the callback is expected to be called here
        assertThat(model.sdks).withFailMessage("SDK should be added to the model").anyMatch { it.name == sdkName }
      }

      //cancel it
      indicator.cancel()
      indicator.checkCanceled()
    }
    assertThat(model.sdks).withFailMessage("SDK should NOT added to the model after cancellation").noneMatch { it.name == sdkName }
  }

  @TestFor(issues = ["IDEA-229737"])
  fun testCrashDownloadingTaskBeforeCommit() = doDownloadTest {
    try {
      doDownload {
        task {
          //the callback is expected to be called here
          assertThat(model.sdks).withFailMessage("SDK should be added to the model").anyMatch { it.name == sdkName }
        }

        error("Download task has to fail")
      }
    }
    catch (t: Throwable) {
      //we do not care if an exception is thrown here
    }
    UIUtil.dispatchAllInvocationEvents()
    assertThat(model.sdks).withFailMessage("SDK should NOT added to the model after cancellation").noneMatch { it.name == sdkName }
  }

  @TestFor(issues = ["IDEA-229737"])
  fun testCancelDownloadingTaskAfterCommitInJdkTable() = doDownloadTest {
    doDownload { indicator ->
      //the callback is expected to be called here
      assertThat(model.sdks).withFailMessage("SDK should be added to the model").anyMatch { it.name == sdkName }

      task {
        invokeAndWaitIfNeeded {
          model.apply()
        }

        runReadAction {
          assertThat(ProjectJdkTable.getInstance().allJdks).withFailMessage(
            "Downloading SDK should be visible").anyMatch { it.name == sdkName }
        }
      }

      //cancel it
      indicator.cancel()
      indicator.checkCanceled()
    }

    assertThat(model.sdks).withFailMessage("SDK should NOT added to the model after cancellation").noneMatch { it.name == sdkName }
    assertThat(ProjectJdkTable.getInstance().allJdks).withFailMessage(
      "Downloading SDK should NOT be visible").noneMatch { it.name == sdkName }
  }
}
