// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration.projectRoot

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.SimpleJavaSdkType
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Assert
import org.junit.Test
import java.lang.Exception

class ProjectSdksModelTest : LightPlatformTestCase() {
  val model = ProjectSdksModel()
  val sdkType = SimpleJavaSdkType()

  @Test
  fun testAddedSdkIsModifiable() {
    val sdk = model.createSdk(sdkType, "testJdskd123", "mock")
    model.addSdk(sdk)

    //we assume that added Sdk is now kept in as editable, so to allow
    //external code to update it in the background.

    Assert.assertSame(sdk, model.findSdk(sdk.name))
    Assert.assertTrue(model.projectSdks.values.contains(sdk))
    Assert.assertFalse(model.projectSdks.keys.contains(sdk))
  }

  @Test
  fun testEditableJdkIsAddedToJdkTable() {
    val sdk = model.createSdk(sdkType, "testJdskd123", "mock")
    model.addSdk(sdk)
    model.apply()

    try {
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
    finally {
      WriteAction.runAndWait<Exception> {
        ProjectJdkTable.getInstance().removeJdk(sdk)
      }
    }
  }
}
