/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.MapDataContext
import org.jetbrains.kotlin.idea.util.application.runWriteAction
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

fun createLibraryWithLongPaths(project: Project): Library {
    val maxCommandlineLengthWindows = 24500
    val maxFilenameLengthWindows = 245

    return runWriteAction {
        val modifiableModel = ProjectLibraryTable.getInstance(project).modifiableModel
        val library = try {
            modifiableModel.createLibrary("LibraryWithLongPaths", null)
        } finally {
            modifiableModel.commit()
        }
        with(library.modifiableModel) {
            for (i in 0..maxCommandlineLengthWindows / maxFilenameLengthWindows) {
                val tmpFile = VirtualFileManager.constructUrl(
                    LocalFileSystem.getInstance().protocol,
                    FileUtil.createTempDirectory("file$i", "a".repeat(maxFilenameLengthWindows)).path
                )
                addRoot(tmpFile, OrderRootType.CLASSES)
            }
            commit()
        }
        return@runWriteAction library
    }
}


private object MockExecutor : DefaultRunExecutor() {
    override fun getId() = EXECUTOR_ID
}

private object MockProfile : RunProfile {
    override fun getState(executor: Executor, env: ExecutionEnvironment) = null
    override fun getIcon() = null
    override fun getName() = "unknown"
}
