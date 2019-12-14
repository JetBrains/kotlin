/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.kotlin.tools.projectWizard.core.TaskRunningContext
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.entity.TemplateSetting
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.*
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repositories
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

class SimpleJsClientTemplate : Template() {
    override val title: String = "Simple JS client"
    override val htmlDescription: String = title
    override val moduleTypes: Set<ModuleType> = setOf(ModuleType.js)
    override val sourcesetTypes: Set<SourcesetType> = setOf(SourcesetType.main)
    override val id: String = "simpleJsClient"

    override fun TaskRunningContext.getRequiredLibraries(sourceset: SourcesetIR): List<DependencyIR> =
        buildList {
            +ArtifactBasedLibraryDependencyIR(
                MavenArtifact(DefaultRepository.JCENTER, "org.jetbrains.kotlinx", "kotlinx-html-js"),
                Version.fromString("0.6.12"),
                DependencyType.MAIN
            )
        }

    override fun TaskRunningContext.getFileTemplates(sourceset: SourcesetIR): List<FileTemplateDescriptor> = listOf(
        FileTemplateDescriptor("$id/client.kt.vm", sourcesPath("client.kt"))
    )

    override fun TaskRunningContext.getIrsToAddToBuildFile(sourceset: SourcesetIR): List<BuildSystemIR> = buildList {
        +RepositoryIR(DefaultRepository.JCENTER)
        if (sourceset is SourcesetModuleIR) {
            +GradleImportIR("org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack")
            val taskAccessIR = GetGradleTaskIR(
                "${sourceset.targetName}BrowserWebpack",
                "KotlinWebpack",
                buildBody {
                    +GradleAssignmentIR("outputFileName", GradleStringConstIR(JS_OUTPUT_FILE_NAME))
                }
            )
            +CreateGradleValueIR(JS_BROWSER_WEBPACK_TASK_ALIAS, taskAccessIR)
        }
    }

    companion object {
        const val JS_OUTPUT_FILE_NAME = "output.js"
        const val JS_BROWSER_WEBPACK_TASK_ALIAS = "jsBrowserWebpack"
    }
}