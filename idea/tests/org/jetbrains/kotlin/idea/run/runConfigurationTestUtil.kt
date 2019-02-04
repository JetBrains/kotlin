/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.Executor
import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.RunManagerEx
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.JavaCommandLine
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.psi.PsiElement
import com.intellij.testFramework.MapDataContext
import org.junit.Assert

fun getJavaRunParameters(configuration: RunConfiguration): JavaParameters {
    val state = configuration.getState(MockExecutor, ExecutionEnvironmentBuilder.create(configuration.project, MockExecutor, MockProfile).build())

    Assert.assertNotNull(state)
    Assert.assertTrue(state is JavaCommandLine)

    configuration.checkConfiguration()
    return (state as JavaCommandLine).javaParameters!!
}

fun createConfigurationFromElement(element: PsiElement, save: Boolean = false): RunConfiguration {
    val dataContext = MapDataContext()
    dataContext.put(Location.DATA_KEY, PsiLocation(element.project, element))

    val runnerAndConfigurationSettings = ConfigurationContext.getFromContext(dataContext).configuration
    if (save) {
        RunManagerEx.getInstanceEx(element.project).setTemporaryConfiguration(runnerAndConfigurationSettings)
    }
    return runnerAndConfigurationSettings!!.configuration
}


private object MockExecutor : DefaultRunExecutor() {
    override fun getId() = DefaultRunExecutor.EXECUTOR_ID
}

private object MockProfile : RunProfile {
    override fun getState(executor: Executor, env: ExecutionEnvironment) = null
    override fun getIcon() = null
    override fun getName() = "unknown"
}
