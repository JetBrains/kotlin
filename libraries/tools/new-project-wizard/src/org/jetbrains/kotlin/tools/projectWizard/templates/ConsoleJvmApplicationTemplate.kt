package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.TaskRunningContext
import org.jetbrains.kotlin.tools.projectWizard.core.asPath
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.addWithJavaIntoJvmTarget
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType

class ConsoleJvmApplicationTemplate : Template() {
    override val id: String = "consoleJvmApp"
    override val title: String = "Console JVM Module with main method"
    override val htmlDescription: String = """
        Console JVM module with main method and run task generated
    """.trimIndent()
    override val moduleTypes: Set<ModuleType> = setOf(ModuleType.jvm)

    override fun TaskRunningContext.getIrsToAddToBuildFile(
        module: ModuleIR
    ) = buildList<BuildSystemIR> {
        +runTaskIrs("MainKt")
    }

    override fun updateTargetIr(module: ModuleIR, targetConfigurationIR: TargetConfigurationIR): TargetConfigurationIR =
        targetConfigurationIR.addWithJavaIntoJvmTarget()

    override fun TaskRunningContext.getFileTemplates(module: ModuleIR) =
        buildList<FileTemplateDescriptorWithPath> {
            +(FileTemplateDescriptor("$id/main.kt.vm", "main.kt".asPath()) asSrcOf SourcesetType.main)
        }
}
