/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.kotlin.tools.projectWizard.core.TaskRunningContext
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.SourcesetIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.NativeTargetInternalIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.withIrs
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType

class NativeConsoleApplicationTemplate : Template() {
    override val title: String = "Native Console Application"
    override val htmlDescription: String = title
    override val moduleTypes: Set<ModuleType> = setOf(ModuleType.native)
    override val sourcesetTypes: Set<SourcesetType> = setOf(SourcesetType.main)
    override val id: String = "nativeConsoleApp"

    override fun updateTargetIr(sourceset: SourcesetIR, targetConfigurationIR: TargetConfigurationIR): TargetConfigurationIR =
        targetConfigurationIR.withIrs(NativeTargetInternalIR("main"))

    override fun TaskRunningContext.getFileTemplates(sourceset: SourcesetIR): List<FileTemplateDescriptor> = buildList {
        +FileTemplateDescriptor("$id/main.kt.vm", sourcesPath("main.kt"))
    }
}