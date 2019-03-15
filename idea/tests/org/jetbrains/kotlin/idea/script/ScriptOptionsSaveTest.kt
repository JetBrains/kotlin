/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.testFramework.LightProjectDescriptor
import org.jdom.output.XMLOutputter
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor

class ScriptOptionsSaveTest : KotlinLightCodeInsightFixtureTestCase() {

    fun testSaveAutoReload() {
        val project = myFixture.project
        val settings = KotlinScriptingSettings.getInstance(project)
        val initialAutoReload = settings.isAutoReloadEnabled

        settings.isAutoReloadEnabled = !initialAutoReload

        assertEquals(
            "isAutoReloadEnabled should be set to true",
            "<KotlinScriptingSettings><option name=\"isAutoReloadEnabled\" value=\"true\" /></KotlinScriptingSettings>",
            XMLOutputter().outputString(settings.state)
        )

        settings.isAutoReloadEnabled = initialAutoReload
    }

    fun testSaveScriptDefinitionOff() {
        val project = myFixture.project
        val scriptDefinition = ScriptDefinitionsManager.getInstance(project).getAllDefinitions().first()

        val settings = KotlinScriptingSettings.getInstance(project)

        val initialIsEnabled = settings.isScriptDefinitionEnabled(scriptDefinition)

        settings.setEnabled(scriptDefinition, !initialIsEnabled)

        assertEquals(
            "scriptDefinition should be off",
            "<KotlinScriptingSettings><scriptDefinition><order>0</order><isEnabled>false</isEnabled></scriptDefinition></KotlinScriptingSettings>",
            XMLOutputter().outputString(settings.state).replace("scriptDefinition .*\">".toRegex(), "scriptDefinition>")
        )

        settings.setEnabled(scriptDefinition, initialIsEnabled)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinLightProjectDescriptor.INSTANCE
    }
}