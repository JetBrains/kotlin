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
import com.intellij.util.containers.ContainerUtil
import gnu.trove.THashMap
import org.jetbrains.kotlin.idea.KotlinPluginUtil

class InspectionDescriptionTest : LightPlatformTestCase() {

    fun testDescriptionsAndShortNames() {
        val shortNames = THashMap<String, InspectionToolWrapper<InspectionProfileEntry, InspectionEP>>()

        val inspectionTools = loadKotlinInspections()
        val tools = inspectionTools.size
        val errors = StringBuilder()
        for (toolWrapper in inspectionTools) {
            val description = toolWrapper.loadDescription()

            if (description == null) {
                errors.append("description is null for inspection '").append(desc(toolWrapper))
            }

            val shortName = toolWrapper.shortName
            val tool1 = shortNames[shortName]
            if (tool1 != null) {
                errors.append("Short names must be unique: " + shortName + "\n" +
                              "inspection: '" + desc(tool1) + "\n" +
                              "        and '" + desc(toolWrapper))
            }
            shortNames.put(shortName, toolWrapper)
        }
        println("$tools inspection tools total")

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
        return tool.toString() + " ('" + tool.getDescriptionContextClass() + "') " +
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

    fun testShortNameConvention() {
        val tools = loadKotlinInspections()
        var count = 0
        for (tool in tools) {
            val name = tool.shortName
            val nameByClass = InspectionProfileEntry.getShortName(tool.tool.javaClass.simpleName)
            if (name != nameByClass) {
                println(name + " vs " + nameByClass)
                count++
            }
        }
        println("$count from ${tools.size} violates convention")
    }

    fun testInspectionMappings() {
        val tools = loadKotlinInspections()
        val errors = StringBuilder()
        tools.filter({ tool -> tool.extension == null }).forEach { tool ->
            errors.append("Please add XML mapping for ").append(tool.getTool().javaClass)
        }

        UsefulTestCase.assertEmpty(errors.toString())
    }

    fun testMismatchedIds() {
        val failMessages = ContainerUtil.newLinkedList<String>()
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
        val failMessages = ContainerUtil.newLinkedList<String>()
        for (ep in LocalInspectionEP.LOCAL_INSPECTION.extensions) {
            val toolName = ep.implementationClass
            if (StringUtil.isEmpty(ep.getDisplayName())) {
                failMessages.add(toolName + ": toolName is not set, tool won't be available in `run inspection` action")
            }
        }
        UsefulTestCase.assertEmpty(StringUtil.join(failMessages, "\n"), failMessages)
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
