/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.InspectionTestUtil
import org.jetbrains.kotlin.idea.quickfix.utils.findInspectionFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

interface QuickFixTest {
    fun parseInspectionsToEnable(beforeFileName: String, beforeFileText: String): List<InspectionProfileEntry> {
        val toolsStrings = InTextDirectivesUtils.findListWithPrefixes(beforeFileText, "TOOL:")
        if (toolsStrings.isNotEmpty()) {
            return toolsStrings.map { toolFqName ->
                try {
                    val aClass = Class.forName(toolFqName)
                    return@map aClass.newInstance() as LocalInspectionTool
                } catch (e: Exception) {
                    throw IllegalArgumentException("Failed to create inspection for key '$toolFqName'", e)
                }
            }
        }

        val inspectionFile = findInspectionFile(File(beforeFileName).parentFile)
        if (inspectionFile != null) {
            val className = FileUtil.loadFile(inspectionFile).trim { it <= ' ' }
            val inspectionClass = Class.forName(className) as Class<InspectionProfileEntry>
            return InspectionTestUtil.instantiateTools(listOf<Class<out InspectionProfileEntry>>(inspectionClass))
        }

        return emptyList()
    }
}