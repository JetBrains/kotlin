/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.codeInspection.ui.InspectionToolPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.InspectionTestUtil
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl.createGlobalContextForTool
import com.intellij.util.xml.highlighting.DomElementsInspection
import org.jetbrains.kotlin.idea.inspections.gradle.DifferentKotlinGradleVersionInspection
import org.jetbrains.kotlin.idea.maven.inspections.DifferentKotlinMavenVersionInspection
import org.jetbrains.plugins.gradle.codeInspection.GradleBaseInspection

fun runInspection(
        inspection: LocalInspectionTool, project: Project, files: List<VirtualFile>? = null, withTestDir: String? = null
): InspectionToolPresentation {
    val wrapper = LocalInspectionToolWrapper(inspection)

    val tool = wrapper.tool
    if (tool is DomElementsInspection<*> || tool is GradleBaseInspection) {
        when (tool) {
            is DifferentKotlinMavenVersionInspection -> tool.testVersionMessage = "\$PLUGIN_VERSION"
            is DifferentKotlinGradleVersionInspection -> tool.testVersionMessage = "\$PLUGIN_VERSION"
        }
    }

    val scope = if (files != null) AnalysisScope(project, files) else AnalysisScope(project)
    scope.invalidate()
    val globalContext = createGlobalContextForTool(scope, project, InspectionManager.getInstance(project) as InspectionManagerEx, wrapper)
    InspectionTestUtil.runTool(wrapper, scope, globalContext)
    if (withTestDir != null) {
        InspectionTestUtil.compareToolResults(globalContext, wrapper, false, withTestDir)
    }
    return globalContext.getPresentation(wrapper)
}

fun runInspection(
        inspectionClass: Class<*>, project: Project, files: List<VirtualFile>? = null, withTestDir: String? = null
): InspectionToolPresentation {
    return runInspection(inspectionClass.newInstance() as LocalInspectionTool, project, files, withTestDir)
}
