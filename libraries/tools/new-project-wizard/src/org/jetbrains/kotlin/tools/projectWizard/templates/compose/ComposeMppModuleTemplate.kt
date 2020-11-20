/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates.compose

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.BuildScriptDependencyIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.BuildScriptRepositoryIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleImportIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.irsList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.mpp.applyMppStructure
import org.jetbrains.kotlin.tools.projectWizard.mpp.mppSources
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.plugins.pomIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repositories
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import org.jetbrains.kotlin.tools.projectWizard.settings.javaPackage
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptor
import org.jetbrains.kotlin.tools.projectWizard.templates.Template

class ComposeMppModuleTemplate : Template() {
    @NonNls
    override val id: String = "composeMppModule"

    override val title: String = KotlinNewProjectWizardBundle.message("module.template.compose.mpp.title")
    override val description: String = KotlinNewProjectWizardBundle.message("module.template.compose.mpp.description")

    override fun isSupportedByModuleType(module: Module, projectKind: ProjectKind): Boolean =
        module.configurator == MppModuleConfigurator && projectKind == ProjectKind.COMPOSE

    override fun Writer.getIrsToAddToBuildFile(module: ModuleIR): List<BuildSystemIR> = irsList {
        +GradleImportIR("org.jetbrains.compose.compose")
        +GradleOnlyPluginByNameIR("org.jetbrains.compose", version = Versions.JETBRAINS_COMPOSE)
        +RepositoryIR(Repositories.JETBRAINS_COMPOSE_DEV)
        +RepositoryIR(DefaultRepository.JCENTER)
        +RepositoryIR(DefaultRepository.GOOGLE)
    }

    override fun Reader.updateBuildFileIRs(irs: List<BuildSystemIR>): List<BuildSystemIR> = irs.filterNot {
        it.safeAs<GradleOnlyPluginByNameIR>()?.pluginId == AndroidModuleConfigurator.DEPENDENCIES.KOTLIN_ANDROID_EXTENSIONS_NAME
    }

    override fun Reader.updateModuleIR(module: ModuleIR): ModuleIR = when (module.originalModule.configurator) {
        CommonTargetConfigurator -> module.withIrs(
            CustomGradleDependencyDependencyIR("compose.runtime", DependencyType.MAIN, DependencyKind.api),
            CustomGradleDependencyDependencyIR("compose.foundation", DependencyType.MAIN, DependencyKind.api),
            CustomGradleDependencyDependencyIR("compose.material", DependencyType.MAIN, DependencyKind.api),
        )
        AndroidTargetConfigurator -> module.withIrs(
            AndroidSinglePlatformModuleConfigurator.DEPENDENCIES.APP_COMPAT.withDependencyKind(DependencyKind.api),
            DEPENDENCIES.ANDROID_KTX.withDependencyKind(DependencyKind.api)
        ).withoutIrs { it == AndroidModuleConfigurator.DEPENDENCIES.MATERIAL }
        else -> module
    }

    override fun Writer.runArbitratyTask(module: ModuleIR): TaskResult<Unit> = inContextOfModuleConfigurator(module.originalModule) {
        val javaPackage = module.originalModule.javaPackage(pomIR())
        val mpp = mppSources(javaPackage) {
            mppFile("platform.kt") {
                function("getPlatformName(): String") {
                    actualFor(ModuleSubType.jvm, actualBody = """return "Desktop" """)
                    actualFor(ModuleSubType.android, actualBody = """return "Android" """)
                    default("""return "Platform" """)
                }
            }
            filesFor(ModuleSubType.common) {
                file(FileTemplateDescriptor("composeMpp/App.kt.vm", relativePath = null), "App.kt", SourcesetType.main)
            }
        }
        applyMppStructure(mpp, module.originalModule, module.path)
    }

    object DEPENDENCIES {
        val ANDROID_KTX = ArtifactBasedLibraryDependencyIR(
            MavenArtifact(DefaultRepository.GOOGLE, "androidx.core", "core-ktx"),
            version = Versions.ANDROID.ANDROIDX_KTX,
            dependencyType = DependencyType.MAIN
        )
    }
}