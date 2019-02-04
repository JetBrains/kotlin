/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.InspectionEP
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.codeInspection.ex.InspectionToolWrapper
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.UsefulTestCase
import gnu.trove.THashMap
import org.jetbrains.kotlin.idea.KotlinPluginUtil

class InspectionDescriptionTest : LightPlatformTestCase() {

    fun testDescriptionsAndShortNames() {
        val shortNames = THashMap<String, InspectionToolWrapper<InspectionProfileEntry, InspectionEP>>()

        val inspectionTools = loadKotlinInspections()
        val errors = StringBuilder()
        for (toolWrapper in inspectionTools) {
            val description = toolWrapper.loadDescription()

            if (description == null) {
                errors.append("description is null for inspection '").append(desc(toolWrapper))
            }

            val shortName = toolWrapper.shortName
            val tool = shortNames[shortName]
            if (tool != null) {
                errors.append("Short names must be unique: " + shortName + "\n" +
                              "inspection: '" + desc(tool) + "\n" +
                              "        and '" + desc(toolWrapper))
            }
            shortNames.put(shortName, toolWrapper)
        }

        UsefulTestCase.assertEmpty(errors.toString())
    }

    private fun loadKotlinInspections(): List<InspectionToolWrapper<InspectionProfileEntry, InspectionEP>> {
        return InspectionToolRegistrar.getInstance().createTools().filter {
            it.extension.pluginDescriptor.pluginId == KotlinPluginUtil.KOTLIN_PLUGIN_ID
        }
    }

    private fun loadKotlinInspectionExtensions() =
            LocalInspectionEP.LOCAL_INSPECTION.extensions.filter {
                it.pluginDescriptor.pluginId == KotlinPluginUtil.KOTLIN_PLUGIN_ID
            }

    private fun desc(tool: InspectionToolWrapper<InspectionProfileEntry, InspectionEP>): String {
        return tool.toString() + " ('" + tool.descriptionContextClass + "') " +
               "in " + if (tool.extension == null) null else tool.extension.pluginDescriptor
    }

    fun testExtensionPoints() {
        val shortNames = THashMap<String, LocalInspectionEP>()

        val inspectionEPs = Extensions.getExtensions(LocalInspectionEP.LOCAL_INSPECTION)
        val tools = inspectionEPs.size
        val errors = StringBuilder()
        for (ep in inspectionEPs) {
            val shortName = ep.getShortName()
            val ep1 = shortNames[shortName]
            if (ep1 != null) {
                errors.append("Short names must be unique: '" + shortName + "':\n" +
                              "inspection: '" + ep1.implementationClass + "' in '" + ep1.pluginDescriptor + "'\n" +
                              ";       and '" + ep.implementationClass + "' in '" + ep.pluginDescriptor + "'")
            }
            shortNames.put(shortName, ep)
        }
        println("$tools inspection tools total via EP")

        UsefulTestCase.assertEmpty(errors.toString())
    }

    fun testInspectionMappings() {
        val toolWrappers = loadKotlinInspections()
        val errors = StringBuilder()
        toolWrappers.filter({ toolWrapper -> toolWrapper.extension == null }).forEach { toolWrapper ->
            errors.append("Please add XML mapping for ").append(toolWrapper.tool::class.java)
        }

        UsefulTestCase.assertEmpty(errors.toString())
    }

    fun testMismatchedIds() {
        val failMessages = mutableListOf<String>()
        for (ep in loadKotlinInspectionExtensions()) {
            val toolName = ep.implementationClass
            val tool = ep.instantiateTool()
            if (tool is LocalInspectionTool) {
                checkValue(failMessages, toolName, "suppressId", ep.id, ep.getShortName(), tool.id)
                checkValue(failMessages, toolName, "alternateId", ep.alternativeId, null, tool.alternativeID)
                checkValue(failMessages, toolName, "shortName", ep.getShortName(), null, tool.shortName)
            }
        }
        UsefulTestCase.assertEmpty(StringUtil.join(failMessages, "\n"), failMessages)
    }

    fun testNotEmptyToolNames() {
        val failMessages = mutableListOf<String>()
        for (ep in LocalInspectionEP.LOCAL_INSPECTION.extensions) {
            val toolName = ep.implementationClass
            if (ep.getDisplayName().isNullOrEmpty()) {
                failMessages.add(toolName + ": toolName is not set, tool won't be available in `run inspection` action")
            }
        }
        UsefulTestCase.assertEmpty(failMessages.joinToString("\n"), failMessages)
    }

    private fun checkValue(failMessages: MutableCollection<String>,
                           toolName: String,
                           attributeName: String,
                           xmlValue: String?,
                           defaultXmlValue: String?,
                           javaValue: String?) {
        if (StringUtil.isNotEmpty(xmlValue)) {
            if (javaValue != xmlValue) {
                failMessages.add("$toolName: mismatched $attributeName. Xml: $xmlValue; Java: $javaValue")
            }
        }
        else if (StringUtil.isNotEmpty(javaValue)) {
            if (javaValue != defaultXmlValue) {
                failMessages.add("$toolName: $attributeName overridden in wrong way, will work in tests only. Please set appropriate $attributeName value in XML ($javaValue)")
            }
        }
    }
}
