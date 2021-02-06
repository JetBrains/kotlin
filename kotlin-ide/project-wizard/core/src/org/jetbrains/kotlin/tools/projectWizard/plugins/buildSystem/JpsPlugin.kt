package org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem

import org.jetbrains.kotlin.tools.projectWizard.core.Context

class JpsPlugin(context: Context) : BuildSystemPlugin(context) {
    val addBuildSystemData by addBuildSystemData(
        BuildSystemData(
            type = BuildSystemType.Jps,
            buildFileData = null
        )
    )
}