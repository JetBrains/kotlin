// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationInfoProvider
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.BaseProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.SmartSerializer
import com.intellij.util.xmlb.annotations.OptionTag
import org.jdom.Element

class ProgramRunnerTest : BasePlatformTestCase() {
  fun testRunnerSettingsSurvivesRunnerUnload() {
    val programRunnerWithSettings = MockProgramRunnerWithSettings()
    val disposable = Disposer.newDisposable()
    ProgramRunner.PROGRAM_RUNNER_EP.getPoint(null).registerExtension(programRunnerWithSettings, disposable)

    val runManager = RunManager.getInstance(project)
    val runnerAndConfigurationSettings = runManager.createConfiguration("Test", FakeConfigurationFactory()) as RunnerAndConfigurationSettingsImpl
    runManager.addConfiguration(runnerAndConfigurationSettings)
    val settings = runnerAndConfigurationSettings.getRunnerSettings(programRunnerWithSettings)
    assertEquals("myTest", settings!!.test)

    val element = Element("configuration")
    runnerAndConfigurationSettings.writeExternal(element)

    Disposer.dispose(disposable)

    runnerAndConfigurationSettings.readExternal(element, false)

    ProgramRunner.PROGRAM_RUNNER_EP.getPoint(null).registerExtension(programRunnerWithSettings, testRootDisposable)
    val settingsAfterReload = runnerAndConfigurationSettings.getRunnerSettings(programRunnerWithSettings)
    assertEquals("myTest", settingsAfterReload!!.test)
  }

  private class MockSettings : RunnerSettings {
    @OptionTag("TEST")
    var test: String = "myTest"

    private val serializer = SmartSerializer()

    override fun readExternal(element: Element) {
      serializer.readExternal(this, element)
    }

    override fun writeExternal(element: Element) {
      serializer.writeExternal(this, element)
    }
  }

  private class MockProgramRunnerWithSettings : BaseProgramRunner<MockSettings>() {
    override fun getRunnerId(): String = "MOCK_RUNNER_WITH_SETTINGS"

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute(environment: ExecutionEnvironment, callback: ProgramRunner.Callback?, state: RunProfileState) {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createConfigurationData(settingsProvider: ConfigurationInfoProvider): MockSettings? {
      return MockSettings()
    }
  }
}