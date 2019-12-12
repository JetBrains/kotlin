package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.TaskRunningContext
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.buildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
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
        +ApplicationPluginIR("MainKt")

        if (buildSystemType.isGradle) {
            +GradleSectionIR("application", buildBody {
                +GradleAssignmentIR("mainClassName", GradleStringConstIR("MainKt"))
            })
        }

        if (sourceset is SourcesetModuleIR) {
            +GetGradleTaskIR(
                "run",
                "JavaExec",
                buildBody {
                    val compilationAccess =
                        CompilationAccessIr(sourceset.type.name, sourceset.sourcesetType.name, null)
                    val classPath = RawGradleIR {
                        when (dsl) {
                            GradlePrinter.GradleDsl.KOTLIN -> {
                                +"with("
                                compilationAccess.render(this)
                                +") "
                                inBrackets {
                                    indent()
                                    +"output.allOutputs + compileDependencyFiles + runtimeDependencyFiles"
                                }
                            }
                            GradlePrinter.GradleDsl.GROOVY -> {
                                compilationAccess.render(this); +".output.allOutputs + "; nl()
                                indented {
                                    indent(); compilationAccess.render(this); +".compileDependencyFiles + "; nl()
                                    indent(); compilationAccess.render(this); +".runtimeDependencyFiles"
                                }
                            }
                        }
                    }

                    +GradleAssignmentIR(
                        "classpath",
                        classPath
                    )
                }
            )
        }
    }

    override fun TaskRunningContext.getFileTemplates(sourceset: SourcesetIR) =
        buildList<FileTemplateDescriptor> {
            +FileTemplateDescriptor("$id/main.kt.vm", sourcesPath("main.kt"))
        }
}
