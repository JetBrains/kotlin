/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.IntentionActionBean
import com.intellij.codeInsight.intention.IntentionManager
import com.intellij.codeInsight.intention.impl.config.IntentionManagerImpl
import com.intellij.openapi.extensions.Extensions
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class IntentionDescriptionTest : LightPlatformTestCase() {

    private val necessaryNormalNames = listOf("description.html", "before.kt.template", "after.kt.template")
    private val necessaryXmlNames = listOf("description.html", "before.xml.template", "after.xml.template")
    private val necessaryMavenNames = listOf("description.html")

    fun testDescriptionsAndShortNames() {
        val intentionTools = loadKotlinIntentions()
        val errors = StringBuilder()
        for (tool in intentionTools) {
            val className = tool.className
            val shortName = className.substringAfterLast(".").replace("$", "")
            val directory = File("idea/resources-en/intentionDescriptions/$shortName")
            if (!directory.exists() || !directory.isDirectory) {
                if (tool.categories != null) {
                    errors.append("No description directory for intention '").append(className).append("'\n")
                }
            } else {
                val necessaryNames = when {
                    shortName.isMavenIntentionName() -> necessaryMavenNames
                    shortName.isXmlIntentionName() -> necessaryXmlNames
                    else -> necessaryNormalNames
                }
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

    private fun String.isMavenIntentionName() = startsWith("MavenPlugin")

    private fun String.isXmlIntentionName() = startsWith("Add") && endsWith("ToManifest")

    private fun loadKotlinIntentions(): List<IntentionActionBean> {
        val extensionPoint = Extensions.getRootArea().getExtensionPoint(IntentionManagerImpl.EP_INTENTION_ACTIONS)
        return extensionPoint.extensions.toList().filter {
            it.pluginDescriptor.pluginId == KotlinPluginUtil.KOTLIN_PLUGIN_ID
        }
    }
}