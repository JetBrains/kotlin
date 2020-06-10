package org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildFileData
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemData
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter

class GroovyDslPlugin(context: Context) : GradlePlugin(context) {
    override val path = "buildSystem.gradle.groovyDsl"

    val addBuildSystemData by addBuildSystemData(
        BuildSystemData(
            type = BuildSystemType.GradleGroovyDsl,
            buildFileData = BuildFileData(
                createPrinter = { GradlePrinter(GradlePrinter.GradleDsl.GROOVY) },
                buildFileName = "build.gradle"
            )
        )
    )

    override val pipelineTasks: List<PipelineTask> = super.pipelineTasks +
            listOf(
                addBuildSystemData,
            )
}