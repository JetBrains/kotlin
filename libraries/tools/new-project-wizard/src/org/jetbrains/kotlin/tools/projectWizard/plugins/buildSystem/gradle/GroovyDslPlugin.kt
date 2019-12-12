package org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.checker
import org.jetbrains.kotlin.tools.projectWizard.core.service.GradleService
import org.jetbrains.kotlin.tools.projectWizard.core.service.MavenService
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildFileData
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemData
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.MavenPrinter

class GroovyDslPlugin(context: Context) : GradlePlugin(context) {
    override val title: String = "Gradle (Groovy DSL)"

    val addBuildSystemData by addBuildSystemData(
        BuildSystemData(
            type = BuildSystemType.GradleGroovyDsl,
            buildSystemServiceClass = GradleService::class,
            buildFileData = BuildFileData(
                createPrinter = { GradlePrinter(GradlePrinter.GradleDsl.GROOVY) },
                buildFileName = "build.gradle"
            )
        )
    )
}