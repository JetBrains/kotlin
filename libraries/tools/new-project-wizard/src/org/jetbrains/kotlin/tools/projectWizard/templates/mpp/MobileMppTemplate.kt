/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates.mpp

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.Writer
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.RepositoryIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.irsList
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.MppModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.mpp.applyMppStructure
import org.jetbrains.kotlin.tools.projectWizard.mpp.mppSources
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.plugins.pomIR
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import org.jetbrains.kotlin.tools.projectWizard.settings.javaPackage
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptor
import org.jetbrains.kotlin.tools.projectWizard.templates.Template

class MobileMppTemplate : Template() {
    @NonNls
    override val id: String = "mobileMppModule"

    override val title: String = KotlinNewProjectWizardBundle.message("module.template.mpp.mobile.title")
    override val description: String = KotlinNewProjectWizardBundle.message("module.template.mpp.mobile.description")

    override fun isSupportedByModuleType(module: Module, projectKind: ProjectKind): Boolean =
        module.configurator == MppModuleConfigurator


    override fun Writer.getIrsToAddToBuildFile(module: ModuleIR): List<BuildSystemIR> = irsList {
        +RepositoryIR(DefaultRepository.JCENTER)
    }

    override fun Writer.runArbitratyTask(module: ModuleIR): TaskResult<Unit> {
        val javaPackage = module.originalModule.javaPackage(pomIR())

        val mpp = mppSources(javaPackage) {
            mppFile("Platform.kt") {
                `class`("Platform") {
                    expectBody = "val platform: String"
                    actualFor(
                        ModuleSubType.android,
                        actualBody = """actual val platform: String = "Android ${'$'}{android.os.Build.VERSION.SDK_INT}""""
                    )

                    actualFor(
                        ModuleSubType.iosArm64, ModuleSubType.iosX64, ModuleSubType.ios,
                        actualBody =
                        """actual val platform: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion"""
                    ) {
                        import("platform.UIKit.UIDevice")
                    }
                }
            }

            filesFor(ModuleSubType.common) {
                file(FileTemplateDescriptor("mppCommon/Greeting.kt.vm", relativePath = null), "Greeting.kt", SourcesetType.main)
            }

            filesFor(ModuleSubType.android) {
                file(FileTemplateDescriptor("android/androidTest.kt.vm", relativePath = null), "androidTest.kt", SourcesetType.test)
            }

            filesFor(ModuleSubType.iosArm64, ModuleSubType.iosX64, ModuleSubType.ios) {
                file(FileTemplateDescriptor("ios/iosTest.kt.vm", relativePath = null), "iosTest.kt", SourcesetType.test)
            }

        }

        return applyMppStructure(mpp, module.originalModule, module.path)
    }
}