package org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask

class JpsPlugin(context: Context) : BuildSystemPlugin(context) {
    override val path = PATH

    companion object {
        private const val PATH = "buildSystem.jps"
        val addBuildSystemData by addBuildSystemData(
            PATH,
            BuildSystemData(
                type = BuildSystemType.Jps,
                buildFileData = null
            )
        )
    }

    override val pipelineTasks: List<PipelineTask> = super.pipelineTasks + listOf(addBuildSystemData)
}