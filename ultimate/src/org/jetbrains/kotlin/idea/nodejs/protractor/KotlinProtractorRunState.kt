/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.nodejs.protractor

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.autotest.ToggleAutoTestAction
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.ui.ConsoleView
import com.intellij.javascript.HelperFilesLocator
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.debug.NodeLocalDebugRunProfileState
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.protractor.ProtractorConsoleFilter
import com.intellij.javascript.protractor.ProtractorOutputToGeneralTestEventsConverter
import com.intellij.javascript.protractor.ProtractorUtil
import com.intellij.javascript.testFramework.navigation.JSTestLocationProvider
import com.intellij.lang.javascript.buildTools.base.JsbtUtil
import com.intellij.openapi.util.Disposer
import com.intellij.util.PathUtil
import com.intellij.util.execution.ParametersListUtil
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

class KotlinProtractorRunState(
        private val runConfiguration: KotlinProtractorRunConfiguration,
        private val environment: ExecutionEnvironment,
        private val protractorPackage: NodePackage
) : RunProfileState, NodeLocalDebugRunProfileState {
    companion object {
        val FRAMEWORK_NAME = "KotlinProtractorJavaScriptTestRunner"
        private val INTELLIJ_CONFIG_FILE_PATH = "protractor-intellij/lib/protractor-intellij-config.js"
    }

    private val project = runConfiguration.project
    private val runSettings = runConfiguration.runSettings

    override fun execute(debugPort: Int): ExecutionResult {
        val interpreter = runSettings.interpreterRef.resolveAsLocal(project)
        val commandLine = createCommandLine(interpreter, debugPort)
        val processHandler = NodeCommandLineUtil.createProcessHandler(commandLine, false)
        val consoleView = createSmtRunnerConsoleView(commandLine.workDirectory)
        ProcessTerminatedListener.attach(processHandler)
        consoleView.attachToProcess(processHandler)
        foldCommandLine(consoleView, processHandler)
        val executionResult = DefaultExecutionResult(consoleView, processHandler)
        executionResult.setRestartActions(ToggleAutoTestAction())
        return executionResult
    }

    private fun createSmtRunnerConsoleView(workingDirectory: File?): ConsoleView {
        val testConsoleProperties = ProtractorConsoleProperties(runConfiguration, environment.executor, JSTestLocationProvider())
        val consoleView = SMTestRunnerConnectionUtil.createConsole(FRAMEWORK_NAME, testConsoleProperties)
        consoleView.addMessageFilter(ProtractorConsoleFilter(project, workingDirectory))
        Disposer.register(environment, consoleView)
        return consoleView
    }

    override fun foldCommandLine(consoleView: ConsoleView, processHandler: ProcessHandler) {
        val configFileName = PathUtil.getFileName(runSettings.configFileSystemDependentPath)
        val testFileName = PathUtil.getFileName(runSettings.testFileSystemDependentPath)
        val seleniumAddress = runSettings.seleniumAddress
        val parameters = ArrayList<String>().apply {
            add("protractor")
            if (configFileName.isNotBlank()) {
                add(configFileName)
            }
            if (testFileName.isNotBlank()) {
                add("--specs=$testFileName")
            }
            if (seleniumAddress.isNotBlank()) {
                add("--seleniumAddress=$seleniumAddress")
            }
            addAll(ParametersListUtil.parse(runSettings.extraOptions.trim()))
        }
        val foldedCommandLine = ParametersListUtil.join(parameters)
        JsbtUtil.foldCommandLine(consoleView, processHandler, foldedCommandLine)
    }

    private fun createCommandLine(interpreter: NodeJsLocalInterpreter, debugPort: Int): GeneralCommandLine {
        val commandLine = GeneralCommandLine().apply { charset = StandardCharsets.UTF_8 }

        runSettings.envData.configureCommandLine(commandLine, true)

        val originalConfigFilePath = runSettings.configFileSystemDependentPath
        val testFilePath = runSettings.testFileSystemDependentPath
        val seleniumAddress = runSettings.seleniumAddress
        val withConfigFile = originalConfigFilePath.isNotBlank()
        commandLine.setWorkDirectory(project.basePath)

        commandLine.exePath = interpreter.interpreterSystemDependentPath
        NodeCommandLineUtil.addNodeOptionsForDebugging(commandLine, emptyList(), debugPort, true, interpreter, true)
        NodeCommandLineUtil.configureUsefulEnvironment(commandLine)
        commandLine.addParameter(ProtractorUtil.getProtractorMainJsFile(protractorPackage).absolutePath)

        val intellijConfigFile = try {
            HelperFilesLocator.getFileRelativeToHelpersDir(INTELLIJ_CONFIG_FILE_PATH)
        }
        catch (e: IOException) {
            throw ExecutionException("Cannot locate wrapper config file", e)
        }

        commandLine.addParameter(intellijConfigFile.absolutePath)

        val actualConfigFilePath = if (withConfigFile) {
            originalConfigFilePath
        }
        else {
            val tempConfigFile = File.createTempFile("protractor", "conf.js")
            tempConfigFile.writeText("exports.config = {}")
            tempConfigFile.absolutePath
        }

        commandLine.addParameter("--intellijOriginalConfigFile=$actualConfigFilePath")

        if (testFilePath.isNotBlank()) {
            commandLine.addParameter("--specs=$testFilePath")
        }

        if (seleniumAddress.isNotBlank()) {
            commandLine.addParameter("--seleniumAddress=$seleniumAddress")
        }

        commandLine.addParameters(ParametersListUtil.parse(runSettings.extraOptions.trim()))

        commandLine.addParameter("--disableChecks")

        return commandLine
    }

    private class ProtractorConsoleProperties(
            configuration: KotlinProtractorRunConfiguration,
            executor: Executor,
            private val myLocator: SMTestLocator
    ) : SMTRunnerConsoleProperties(configuration, FRAMEWORK_NAME, executor), SMCustomMessagesParsing {
        init {
            isUsePredefinedMessageFilter = false
            setIfUndefined(TestConsoleProperties.HIDE_PASSED_TESTS, false)
            setIfUndefined(TestConsoleProperties.HIDE_IGNORED_TEST, true)
            setIfUndefined(TestConsoleProperties.SCROLL_TO_SOURCE, true)
            setIfUndefined(TestConsoleProperties.SELECT_FIRST_DEFECT, true)
            isIdBasedTestTree = true
            isPrintTestingStartedTime = false
        }

        override fun getTestLocator() = myLocator

        override fun createTestEventsConverter(testFrameworkName: String, consoleProperties: TestConsoleProperties) =
                ProtractorOutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties.isEditable)
    }
}