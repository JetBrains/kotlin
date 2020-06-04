/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.testFramework.exceptionCases.AbstractExceptionCase
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.ComparisonFailure

@DaemonAnalyzerTestCase.CanChangeDocumentDuringHighlighting
abstract class AbstractScriptDefinitionsOrderTest : AbstractScriptConfigurationTest() {
    fun doTest(unused: String) {
        configureScriptFile(testDataFile())

        assertException(object : AbstractExceptionCase<ComparisonFailure>() {
            override fun getExpectedExceptionClass(): Class<ComparisonFailure> = ComparisonFailure::class.java

            override fun tryClosure() {
                checkHighlighting(editor, false, false)
            }
        })

        val definitions = InTextDirectivesUtils.findStringWithPrefixes(myFile.text, "// SCRIPT DEFINITIONS: ")
            ?.split(";")
            ?.map { it.substringBefore(":").trim() to it.substringAfter(":").trim() }
            ?: error("SCRIPT DEFINITIONS directive should be defined")

        val allDefinitions = ScriptDefinitionsManager.getInstance(project).getAllDefinitions()
        for ((definitionName, action) in definitions) {
            val scriptDefinition = allDefinitions
                .find { it.name == definitionName }
                ?: error("Unknown script definition name in SCRIPT DEFINITIONS directive: name=$definitionName, all={${allDefinitions.joinToString { it.name }}}")
            when (action) {
                "off" -> KotlinScriptingSettings.getInstance(project).setEnabled(scriptDefinition, false)
                else -> KotlinScriptingSettings.getInstance(project).setOrder(scriptDefinition, action.toInt())
            }
        }

        ScriptDefinitionsManager.getInstance(project).reorderScriptDefinitions()

        checkHighlighting(editor, false, false)
    }
}