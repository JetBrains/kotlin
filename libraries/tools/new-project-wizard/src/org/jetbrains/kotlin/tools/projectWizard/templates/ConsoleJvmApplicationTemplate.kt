package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.TaskRunningContext
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
    override val sourcesetTypes: Set<SourcesetType> = setOf(SourcesetType.main)

    override fun TaskRunningContext.getIrsToAddToBuildFile(
        sourceset: SourcesetIR
    ) = buildList<BuildSystemIR> {
        +runTaskIrs("MainKt")
    }

    override fun updateTargetIr(sourceset: SourcesetIR, targetConfigurationIR: TargetConfigurationIR): TargetConfigurationIR =
        targetConfigurationIR.addWithJavaIntoJvmTarget()

    override fun TaskRunningContext.getFileTemplates(sourceset: SourcesetIR) =
        buildList<FileTemplateDescriptor> {
            +FileTemplateDescriptor("$id/main.kt.vm", sourcesPath("main.kt"))
        }
}
