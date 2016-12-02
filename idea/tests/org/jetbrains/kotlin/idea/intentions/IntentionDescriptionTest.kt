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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.IntentionActionBean
import com.intellij.codeInsight.intention.IntentionManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import java.io.File

class IntentionDescriptionTest : LightPlatformTestCase() {

    val necessaryNormalNames = listOf("description.html", "before.kt.template", "after.kt.template")
    val necessaryMavenNames = listOf("description.html")

    fun testDescriptionsAndShortNames() {
        val intentionTools = loadKotlinIntentions()
        val errors = StringBuilder()
        for (tool in intentionTools) {
            val className = tool.className
            val shortName = className.substringAfterLast(".").replace("$", "")
            val directory = File("idea/resources/intentionDescriptions/$shortName")
            if (!directory.exists() || !directory.isDirectory) {
                errors.append("No description directory for intention '").append(className).append("'\n")
            }
            else {
                val necessaryNames = if (shortName.startsWith("MavenPlugin")) necessaryMavenNames else necessaryNormalNames
                for (fileName in necessaryNames) {
                    val file = directory.resolve(fileName)
                    if (!file.exists() || !file.isFile) {
                        errors.append("No description file $fileName for intention '").append(className).append("'\n")
                    }
                }
            }
        }
        UsefulTestCase.assertEmpty(errors.toString())
    }

    private fun loadKotlinIntentions(): List<IntentionActionBean> {
        val extensionPoint = Extensions.getArea(null).getExtensionPoint(IntentionManager.EP_INTENTION_ACTIONS)
        return extensionPoint.extensions.toList().filter {
            it.pluginDescriptor.pluginId == KotlinPluginUtil.KOTLIN_PLUGIN_ID
        }
    }
}