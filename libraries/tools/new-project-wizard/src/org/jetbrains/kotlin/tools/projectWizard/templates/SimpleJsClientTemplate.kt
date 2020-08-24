/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates


import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.core.Writer
import org.jetbrains.kotlin.tools.projectWizard.core.asPath
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.TemplateSetting
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ArtifactBasedLibraryDependencyIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.DependencyIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.DependencyType
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repositories
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType

class SimpleJsClientTemplate : JsClientTemplate() {
    override val title: String = KotlinNewProjectWizardBundle.message("module.template.js.simple.title")
    override val description: String = KotlinNewProjectWizardBundle.message("module.template.js.simple.description")

    @NonNls
    override val id: String = "simpleJsClient"

    val useKotlinxHtml by booleanSetting(
        KotlinNewProjectWizardBundle.message("module.template.simple.use.kotlinx.html"),
        GenerationPhase.PROJECT_GENERATION
    ) {
        defaultValue = value(false)
    }

    override val settings: List<TemplateSetting<*, *>> = listOf(useKotlinxHtml)

    override fun Writer.getRequiredLibraries(module: ModuleIR): List<DependencyIR> = withSettingsOf(module.originalModule) {
        buildList {
            if (useKotlinxHtml.reference.settingValue()) {
                +ArtifactBasedLibraryDependencyIR(
                    MavenArtifact(Repositories.KOTLINX, "org.jetbrains.kotlinx", "kotlinx-html"),
                    Versions.KOTLINX.KOTLINX_HTML(KotlinPlugin.version.propertyValue.version),
                    DependencyType.MAIN
                )
            }
        }
    }

    override fun Reader.getFileTemplates(module: ModuleIR): List<FileTemplateDescriptorWithPath> =
        withSettingsOf(module.originalModule) {
            buildList {
                val hasKtorServNeighbourTarget = hasKtorServNeighbourTarget(module)
                if (!hasKtorServNeighbourTarget) {
                    +(FileTemplateDescriptor("$id/index.html.vm") asResourceOf SourcesetType.main)
                }
                if (useKotlinxHtml.reference.settingValue()) {
                    +(FileTemplateDescriptor("$id/client.kt.vm") asSrcOf SourcesetType.main)
                    +(FileTemplateDescriptor("$id/TestClient.kt.vm", "TestClient.kt".asPath()) asSrcOf SourcesetType.test)
                }
            }
        }
}
