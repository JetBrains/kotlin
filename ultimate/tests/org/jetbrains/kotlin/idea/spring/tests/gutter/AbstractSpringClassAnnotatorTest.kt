/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.spring.tests.gutter

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import icons.SpringApiIcons
import junit.framework.Assert
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.spring.tests.SpringTestFixtureExtension
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.TestFixtureExtension
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractSpringClassAnnotatorTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = LightCodeInsightFixtureTestCase.JAVA_LATEST

    override fun setUp() {
        super.setUp()
        TestFixtureExtension.loadFixture<SpringTestFixtureExtension>(myModule)
    }

    protected fun doTest(path: String) {
        val configFilePath = "${KotlinTestUtils.getHomeDirectory()}/$path"
        val configFile = File(configFilePath)
        val testRoot = configFile.parentFile

        val config = JsonParser().parse(FileUtil.loadFile(configFile, true)) as JsonObject

        val withRuntime = config["withRuntime"]?.asBoolean ?: false
        if (withRuntime) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
        }

        try {
            val springConfigFiles = (config["springConfig"] as JsonArray).map { it.asString }

            myFixture.testDataPath = testRoot.absolutePath
            TestFixtureExtension.getFixture<SpringTestFixtureExtension>()!!.configureFileSet(myFixture, springConfigFiles)
            for (file in testRoot.listFiles()) {
                val name = file.name
                if (name in springConfigFiles) continue
                if (file.isDirectory) myFixture.copyDirectoryToProject(name, name) else myFixture.copyFileToProject(name)
            }

            val fileName = config.getString("file")
            val iconName = config.getString("icon")
            val icon = SpringApiIcons::class.java.getField(iconName)[null]

            val gutterMark = myFixture.findGutter(fileName)!!.let {
                if (it.icon == icon) it
                else myFixture.findGuttersAtCaret().first { it.icon == icon }
            }

            val tooltip = config.getString("tooltip")
            Assert.assertEquals(tooltip, gutterMark.tooltipText)

            val naming = config.getString("naming")
            val targets = (config["targets"] as JsonArray).map { it.asString }
            when (naming) {
                "bean" -> checkBeanGutterTargets(gutterMark, targets)
                "property" -> checkBeanPropertyTargets(gutterMark, targets)
                "generic" -> checkPsiElementGutterTargets(gutterMark, targets)
                else -> error("Unexpected naming: $naming")
            }
        }
        finally {
            if (withRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
            }
        }
    }

    override fun tearDown() {
        TestFixtureExtension.unloadFixture<SpringTestFixtureExtension>()
        super.tearDown()
    }
}
