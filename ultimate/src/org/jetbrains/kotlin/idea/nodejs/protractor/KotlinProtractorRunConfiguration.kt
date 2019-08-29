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

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.protractor.ProtractorUtil
import com.intellij.openapi.project.Project
import com.intellij.util.PathUtil
import org.jdom.Element
import java.io.File

// Based on com.intellij.javascript.protractor.ProtractorRunConfiguration
class KotlinProtractorRunConfiguration(
        project: Project,
        factory: ConfigurationFactory,
        name: String
) : LocatableConfigurationBase<Any>(project, factory, name) {
    var runSettings = KotlinProtractorRunSettings()

    val protractorPackage: NodePackage
        get() {
            val project = project
            val interpreter = runSettings.interpreterRef.resolve(project)
            return ProtractorUtil.getProtractorPackage(project, null, interpreter, true)
        }

    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
            KotlinProtractorRunState(this, environment, protractorPackage)

    override fun getConfigurationEditor() = KotlinProtractorRunConfigurationEditor(project)

    override fun readExternal(element: Element) {
        super.readExternal(element)
        runSettings = KotlinProtractorRunSettings.readFromXML(element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        runSettings.writeToXML(element)
    }

    fun initialize() {
        setNameChangedByUser(false)
    }

    override fun checkConfiguration() {
        val isOptionalConfig = runSettings.testFileSystemDependentPath.isNotBlank() || runSettings.seleniumAddress.isNotBlank()
        if (isOptionalConfig) {
            validatePath("test file", runSettings.testFileSystemDependentPath, true, false)
            if (runSettings.seleniumAddress.isBlank()) {
                throw RuntimeConfigurationError("Unspecified Selenium address")
            }
        }
        if (!isOptionalConfig || runSettings.configFileSystemDependentPath.isNotBlank()) {
            validatePath("configuration file", runSettings.configFileSystemDependentPath, true, false)
        }

        val interpreter = runSettings.interpreterRef.resolve(project)
        NodeJsLocalInterpreter.checkForRunConfiguration(interpreter)

        validatePath("protractor package", protractorPackage.systemDependentPath, true, true)
    }

    override fun suggestedName(): String? {
        val name = PathUtil.getFileName(runSettings.testFileSystemDependentPath)
        return if (name.isNotBlank()) name else null
    }

    private fun validatePath(name: String,
                             path: String?,
                             shouldBeAbsolute: Boolean,
                             shouldBeDirectory: Boolean
    ) {
        if (path.isNullOrBlank()) throw RuntimeConfigurationError("Unspecified " + name)

        val file = File(path!!)

        if (shouldBeAbsolute && !file.isAbsolute) throw RuntimeConfigurationError("No such " + name)

        val exists = if (shouldBeDirectory) file.isDirectory else file.isFile
        if (!exists) throw RuntimeConfigurationError("No such " + name)
    }
}