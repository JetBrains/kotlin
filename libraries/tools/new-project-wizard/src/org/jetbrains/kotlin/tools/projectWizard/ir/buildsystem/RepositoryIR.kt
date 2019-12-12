package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repository

data class RepositoryIR(val repository: Repository) : BuildSystemIR {
    override fun BuildFilePrinter.render() = when (this) {
        is GradlePrinter -> when (repository) {
            is DefaultRepository -> {
                +repository.type.asGradleName()
                +"()"
            }
            else -> Unit
        }
        else -> Unit
    }

    companion object {
        private fun DefaultRepository.Type.asGradleName() = when (this) {
            DefaultRepository.Type.JCENTER -> "jcenter"
            DefaultRepository.Type.MAVEN_CENTRAL -> "mavenCentral"
            DefaultRepository.Type.GOOGLE -> "google"
            DefaultRepository.Type.GRADLE_PLUGIN_PORTAL -> "gradlePluginPortal"
        }
    }
}
