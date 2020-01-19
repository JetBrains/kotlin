/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle

import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter

data class SettingsGradleFileIR(
    val projectName: String,
    val subProjects: List<String>,
    override val irs: List<BuildSystemIR>
) : FileIR, GradleIR {
    override fun withReplacedIrs(irs: List<BuildSystemIR>): SettingsGradleFileIR = copy(irs = irs)

    override fun GradlePrinter.renderGradle() {
        +"rootProject.name = "; +projectName.quotified
        nl(lineBreaks = 2)
        if (subProjects.isNotEmpty()) {
            nl()
            subProjects.list(separator = { nl() }) { subProject ->
                +"include("; +subProject.quotified; +")"
            }
            nl(lineBreaks = 2)
        }
        val repositories = irsOfType<RepositoryIR>()
        if (repositories.isNotEmpty()) {
            sectionCall("pluginManagement", needIndent = true) {
                sectionCall("repositories") {
                    repositories.listNl()
                }
            }
        }

    }
}