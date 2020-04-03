package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.MavenPrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.CustomMavenRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repository

interface RepositoryWrapper {
    val repository: Repository
}

fun <W : RepositoryWrapper> List<W>.distinctAndSorted() =
    distinctBy(RepositoryWrapper::repository)
        .sortedBy { wrapper ->
            if (wrapper.repository is DefaultRepository) 0 else 1
        }

data class RepositoryIR(override val repository: Repository) : BuildSystemIR, RepositoryWrapper {
    override fun BuildFilePrinter.render() = when (this) {
        is GradlePrinter -> when (repository) {
            is DefaultRepository -> {
                +repository.type.gradleName
                +"()"
            }
            is CustomMavenRepository -> {
                sectionCall("maven", needIndent = true) {
                    assignmentOrCall("url") {
                        val url = repository.url.quotified
                        when (dsl) {
                            GradlePrinter.GradleDsl.KOTLIN -> call("uri") { +url }
                            GradlePrinter.GradleDsl.GROOVY -> +url
                        }
                    }
                }
            }
            else -> Unit
        }
        is MavenPrinter -> {
            node("repository") {
                singleLineNode("id") { +repository.idForMaven }
                singleLineNode("url") { +repository.url }
            }
        }
        else -> Unit
    }
}
