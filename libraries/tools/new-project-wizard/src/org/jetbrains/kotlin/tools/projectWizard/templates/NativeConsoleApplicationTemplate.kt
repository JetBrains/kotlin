/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates


import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.NativeTargetInternalIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.withIrs
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.NativeTargetConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.moduleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module

class NativeConsoleApplicationTemplate : Template() {
    override val title: String = KotlinNewProjectWizardBundle.message("module.template.native.console.title")
    override val description: String = KotlinNewProjectWizardBundle.message("module.template.native.console.description")

    override fun isSupportedByModuleType(module: Module): Boolean =
        module.configurator.moduleType == ModuleType.native

    override val id: String = "nativeConsoleApp"

    override fun isApplicableTo(
        reader: Reader,
        module: Module
    ): Boolean =
        module.configurator.safeAs<NativeTargetConfigurator>()?.isDesktopTarget == true

    override fun updateTargetIr(module: ModuleIR, targetConfigurationIR: TargetConfigurationIR): TargetConfigurationIR =
        targetConfigurationIR.withIrs(NativeTargetInternalIR("main"))

    override fun Reader.getFileTemplates(module: ModuleIR): List<FileTemplateDescriptorWithPath> = buildList {
        +(FileTemplateDescriptor("$id/main.kt.vm", "main.kt".asPath()) asSrcOf SourcesetType.main)
    }
}