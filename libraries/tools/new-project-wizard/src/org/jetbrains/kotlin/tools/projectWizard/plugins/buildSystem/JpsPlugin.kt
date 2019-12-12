package org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.service.JpsService

class JpsPlugin(context: Context) : BuildSystemPlugin(context) {
    override val title: String = "IDEA"

    val addBuildSystemData by addBuildSystemData(
        BuildSystemData(
            type = BuildSystemType.Jps,
            buildSystemServiceClass = JpsService::class,
            buildFileData = null
        )
    )
}