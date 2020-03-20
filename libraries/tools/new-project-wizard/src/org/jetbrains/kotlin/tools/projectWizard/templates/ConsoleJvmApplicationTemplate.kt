package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.kotlin.tools.projectWizard.core.Writer
import org.jetbrains.kotlin.tools.projectWizard.core.asPath
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.addWithJavaIntoJvmTarget
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.runTaskIrs
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType

class ConsoleJvmApplicationTemplate : Template() {
    override val id: String = "consoleJvmApp"
    override val title: String = "Console Application"
    override val description: String = """Simple “Hello World!” Kotlin/JVM application that works in the console"""
    override val moduleTypes: Set<ModuleType> = setOf(ModuleType.jvm)

    override fun Writer.getIrsToAddToBuildFile(
        module: ModuleIR
    ) = buildList<BuildSystemIR> {
        +runTaskIrs("MainKt")
    }

    override fun updateTargetIr(module: ModuleIR, targetConfigurationIR: TargetConfigurationIR): TargetConfigurationIR =
        targetConfigurationIR.addWithJavaIntoJvmTarget()

    override fun Writer.getFileTemplates(module: ModuleIR) =
        buildList<FileTemplateDescriptorWithPath> {
            +(FileTemplateDescriptor("$id/main.kt.vm", "main.kt".asPath()) asSrcOf SourcesetType.main)
        }
}
