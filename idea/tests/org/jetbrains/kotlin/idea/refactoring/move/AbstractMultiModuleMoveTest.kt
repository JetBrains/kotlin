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

package org.jetbrains.kotlin.idea.refactoring.move

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import org.jetbrains.kotlin.idea.refactoring.rename.loadTestConfiguration
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

abstract class AbstractMultiModuleMoveTest : KotlinMultiFileTestCase() {
    override fun getTestRoot(): String = "/refactoring/moveMultiModule/"

    override fun getTestDataPath(): String = PluginTestCaseBase.getTestDataPathBase()

    fun doTest(path: String) {
        val config = loadTestConfiguration(File(path))

        isMultiModule = true

        doTestCommittingDocuments { rootDir, _ ->
            val modulesWithJvmRuntime: List<Module>
            val modulesWithJsRuntime: List<Module>

            val withRuntime = config["withRuntime"]?.asBoolean ?: false
            if (withRuntime) {
                val moduleManager = ModuleManager.getInstance(project)
                modulesWithJvmRuntime =
                        (config["modulesWithRuntime"]?.asJsonArray?.map { moduleManager.findModuleByName(it.asString!!)!! }
                         ?: moduleManager.modules.toList())
                modulesWithJvmRuntime.forEach { ConfigLibraryUtil.configureKotlinRuntimeAndSdk(it, PluginTestCaseBase.mockJdk()) }
                modulesWithJsRuntime =
                        (config["modulesWithJsRuntime"]?.asJsonArray?.map { moduleManager.findModuleByName(it.asString!!)!! }
                         ?: emptyList())
                modulesWithJsRuntime.forEach { ConfigLibraryUtil.configureKotlinJsRuntimeAndSdk(it, PluginTestCaseBase.mockJdk()) }
            }
            else {
                modulesWithJvmRuntime = emptyList()
                modulesWithJsRuntime = emptyList()
            }

            try {
                runMoveRefactoring(path, config, rootDir, project)
            }
            finally {
                modulesWithJvmRuntime.forEach {
                    ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(it, PluginTestCaseBase.mockJdk())
                }
                modulesWithJsRuntime.forEach {
                    ConfigLibraryUtil.unConfigureKotlinJsRuntimeAndSdk(it, PluginTestCaseBase.mockJdk())
                }
            }
        }
    }
}