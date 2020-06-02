// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.util.environment.Environment
import com.intellij.openapi.externalSystem.util.environment.TestEnvironment
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkTableImpl
import com.intellij.openapi.roots.ui.configuration.SdkTestCase
import com.intellij.openapi.roots.ui.configuration.UnknownSdkResolver
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.use
import com.intellij.testFramework.replaceService

abstract class ExternalSystemJdkUtilTestCase : SdkTestCase() {

  val environment get() = Environment.getInstance() as TestEnvironment
  val jdkProvider get() = ExternalSystemJdkProvider.getInstance() as TestJdkProvider

  override fun setUp() {
    super.setUp()

    val application = ApplicationManager.getApplication()
    application.replaceService(Environment::class.java, TestEnvironment(), testRootDisposable)
    application.replaceService(ExternalSystemJdkProvider::class.java, TestJdkProvider(), testRootDisposable)

    UnknownSdkResolver.EP_NAME.point.registerExtension(TestUnknownSdkResolver, testRootDisposable)

    environment.variables(ExternalSystemJdkUtil.JAVA_HOME to null)

    TestUnknownSdkResolver.unknownSdkFixMode = TestUnknownSdkResolver.TestUnknownSdkFixMode.TEST_LOCAL_FIX
  }

  class TestJdkProvider : ExternalSystemJdkProvider, Disposable {
    private val internalJdk by lazy { TestSdkGenerator.createNextSdk() }

    override fun getJavaSdkType() = TestSdkType

    override fun getInternalJdk(): Sdk = internalJdk

    override fun createJdk(jdkName: String?, homePath: String): Sdk {
      val sdk = TestSdkGenerator.findTestSdk(homePath)!!
      Disposer.register(this, Disposable { removeSdk(sdk) })
      return sdk
    }

    override fun dispose() {}
  }

  companion object {
    fun assertUnexpectedSdksRegistration(action: () -> Unit) {
      assertNewlyRegisteredSdks({ null }, action)
    }

    fun assertNewlyRegisteredSdks(expectedNewSdk: () -> TestSdk?, action: () -> Unit) {
      val projectSdkTable = ProjectJdkTable.getInstance()
      val beforeSdks = projectSdkTable.allJdks.toSet()

      var throwable = runCatching(action).exceptionOrNull()

      val afterSdks = projectSdkTable.allJdks.toSet()
      val newSdks = afterSdks - beforeSdks

      throwable = throwable ?: runCatching { assertNewlyRegisteredSdks(expectedNewSdk(), newSdks) }.exceptionOrNull()

      removeSdks(*newSdks.toTypedArray())

      if (throwable != null) throw throwable
    }

    private fun assertNewlyRegisteredSdks(expectedNewSdk: TestSdk?, newSdks: Set<Sdk>) {
      if (expectedNewSdk != null) {
        assertTrue("Expected registration of $expectedNewSdk but found $newSdks", newSdks.size == 1)
        val newSdk = newSdks.first()
        assertSdk(expectedNewSdk, newSdk)
      }
      else {
        assertTrue("Unexpected sdk registration $newSdks", newSdks.isEmpty())
      }
    }

    fun withoutRegisteredSdks(action: () -> Unit) {
      val application = ApplicationManager.getApplication()
      Disposer.newDisposable().use {
        application.replaceService(ProjectJdkTable::class.java, ProjectJdkTableImpl(), it)
        assertUnexpectedSdksRegistration(action)
      }
    }

    fun withRegisteredSdks(vararg sdks: TestSdk, action: () -> Unit) {
      Disposer.newDisposable().use {
        registerSdks(*sdks, parentDisposable = it)
        assertUnexpectedSdksRegistration(action)
      }
    }
  }
}