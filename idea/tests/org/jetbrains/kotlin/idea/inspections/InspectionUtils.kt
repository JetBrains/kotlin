/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.GlobalInspectionTool
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.codeInspection.ui.InspectionToolPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.InspectionTestUtil
import com.intellij.testFramework.createGlobalContextForTool
import org.jdom.Element

fun runInspection(
    inspection: LocalInspectionTool, project: Project, files: List<VirtualFile>? = null, withTestDir: String? = null
): InspectionToolPresentation {
    val wrapper = LocalInspectionToolWrapper(inspection)

    val tool = wrapper.tool
    if (tool is PluginVersionDependentInspection) {
        tool.testVersionMessage = "\$PLUGIN_VERSION"
    }

    val scope = if (files != null) AnalysisScope(project, files) else AnalysisScope(project)
    scope.invalidate()
    val globalContext = createGlobalContextForTool(scope, project, listOf(wrapper))
    InspectionTestUtil.runTool(wrapper, scope, globalContext)
    if (withTestDir != null) {
        InspectionTestUtil.compareToolResults(globalContext, wrapper, false, withTestDir)
    }
    return globalContext.getPresentation(wrapper)
}

fun runInspection(
    inspectionClass: Class<*>, project: Project,
    settings: Element? = null, files: List<VirtualFile>? = null, withTestDir: String? = null
): InspectionToolPresentation {
    @Suppress("UNCHECKED_CAST")
    val profileEntryClass = inspectionClass as Class<InspectionProfileEntry>

    val inspection = InspectionTestUtil.instantiateTools(listOf(profileEntryClass)).singleOrNull()
        ?: error("Can't create `$inspectionClass` inspection")

    if (settings != null) {
        inspection.readSettings(settings)
    }

    val localInspection = when (inspection) {
        is LocalInspectionTool -> inspection
        is GlobalInspectionTool -> inspection.sharedLocalInspectionTool ?: error("Global inspection ${inspection::class} without local counterpart")
        else -> error("Unknown class for inspection instance")
    }

    return runInspection(localInspection, project, files, withTestDir)
}
