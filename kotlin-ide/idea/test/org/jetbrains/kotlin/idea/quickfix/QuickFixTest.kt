/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.InspectionTestUtil
import org.jetbrains.kotlin.idea.quickfix.utils.findInspectionFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

interface QuickFixTest {
    fun parseInspectionsToEnable(beforeFileName: String, beforeFileText: String): List<InspectionProfileEntry> {
        val toolsStrings = InTextDirectivesUtils.findListWithPrefixes(beforeFileText, "TOOL:")
        val profiles = try {
            if (toolsStrings.isNotEmpty()) toolsStrings.map { toolFqName ->
                @Suppress("UNCHECKED_CAST")
                Class.forName(toolFqName) as Class<InspectionProfileEntry>
            } else {
                val inspectionFile = findInspectionFile(File(beforeFileName).parentFile) ?: return emptyList()
                val className = FileUtil.loadFile(inspectionFile).trim { it <= ' ' }
                @Suppress("UNCHECKED_CAST") val inspectionClass = Class.forName(className) as Class<InspectionProfileEntry>
                listOf(inspectionClass)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to create inspections", e)
        }

        return InspectionTestUtil.instantiateTools(profiles)
    }
}