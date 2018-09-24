/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.importsDumper

import com.intellij.openapi.project.Project
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import kotlinx.serialization.map
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

class ImportsDumperExtension(destinationPath: String) : AnalysisHandlerExtension {
    private val destination: File = File(destinationPath)

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        val filePathToImports: MutableMap<String, List<String>> = mutableMapOf()

        for (file in files) {
            filePathToImports[file.virtualFilePath] = file.importDirectives.map { it.text }
        }

        val serializer = (StringSerializer to StringSerializer.list).map

        val jsonStringWithImports = JSON.stringify(serializer, filePathToImports)

        destination.writeText(jsonStringWithImports)

        return AnalysisResult.success(bindingTrace.bindingContext, module, shouldGenerateCode = false)
    }
}