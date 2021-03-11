/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.testFramework.IdeaTestUtil
import org.jetbrains.kotlin.idea.refactoring.rename.loadTestConfiguration
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

abstract class AbstractMultiModuleMoveTest : KotlinMultiFileTestCase() {
    override fun getTestRoot(): String = "/refactoring/moveMultiModule/"

    override fun getTestDataDirectory() = IDEA_TEST_DATA_DIR

    fun doTest(path: String) {
        val config = loadTestConfiguration(File(path))

        isMultiModule = true

        doTestCommittingDocuments { rootDir, _ ->
            val modulesWithJvmRuntime: List<Module>
            val modulesWithJsRuntime: List<Module>
            val modulesWithCommonRuntime: List<Module>

            PluginTestCaseBase.addJdk(testRootDisposable, IdeaTestUtil::getMockJdk18)

            val withRuntime = config["withRuntime"]?.asBoolean ?: false
            if (withRuntime) {
                val moduleManager = ModuleManager.getInstance(project)
                modulesWithJvmRuntime = (config["modulesWithRuntime"]?.asJsonArray?.map { moduleManager.findModuleByName(it.asString!!)!! }
                    ?: moduleManager.modules.toList())
                modulesWithJvmRuntime.forEach { ConfigLibraryUtil.configureKotlinRuntimeAndSdk(it, IdeaTestUtil.getMockJdk18()) }

                modulesWithJsRuntime =
                    (config["modulesWithJsRuntime"]?.asJsonArray?.map { moduleManager.findModuleByName(it.asString!!)!! } ?: emptyList())
                modulesWithJsRuntime.forEach { module ->
                    ConfigLibraryUtil.configureSdk(module, IdeaTestUtil.getMockJdk18())
                    ConfigLibraryUtil.configureKotlinStdlibJs(module)
                }

                modulesWithCommonRuntime =
                    (config["modulesWithCommonRuntime"]?.asJsonArray?.map { moduleManager.findModuleByName(it.asString!!)!! }
                        ?: emptyList())
                modulesWithCommonRuntime.forEach { ConfigLibraryUtil.configureKotlinStdlibCommon(it) }
            } else {
                modulesWithJvmRuntime = emptyList()
                modulesWithJsRuntime = emptyList()
                modulesWithCommonRuntime = emptyList()
            }

            try {
                runMoveRefactoring(path, config, rootDir, project)
            } finally {
                modulesWithJvmRuntime.forEach {
                    ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(it, IdeaTestUtil.getMockJdk18())
                }
                modulesWithJsRuntime.forEach {
                    ConfigLibraryUtil.unConfigureKotlinJsRuntimeAndSdk(it, IdeaTestUtil.getMockJdk18())
                }
                modulesWithCommonRuntime.forEach {
                    ConfigLibraryUtil.unConfigureKotlinCommonRuntime(it)
                }
            }
        }
    }
}