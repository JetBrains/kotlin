/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle

import kotlinx.collections.immutable.PersistentList
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter

data class SettingsGradleFileIR(
    @NonNls val projectName: String,
    val subProjects: List<String>,
    override val irs: PersistentList<BuildSystemIR>
) : FileIR, GradleIR {
    override fun withReplacedIrs(irs: PersistentList<BuildSystemIR>): SettingsGradleFileIR = copy(irs = irs)

    override fun GradlePrinter.renderGradle() {
        irsOfTypeOrNull<PluginManagementIR>()?.let { pluginManagementIrs ->
            sectionCall("pluginManagement", needIndent = true) {
                pluginManagementIrs.irsOfTypeOrNull<PluginManagementRepositoryIR>()?.let { repositories ->
                    sectionCall("repositories") {
                        repositories.distinctAndSorted().listNl()
                    }
                }
                nl()
                freeIrs().removeSingleIRDuplicates().listNl()
            }
        }
        nl()

        +"rootProject.name = "; +projectName.quotified
        nl(lineBreaks = 2)
        if (subProjects.isNotEmpty()) {
            nl()
            subProjects.list(separator = { nl() }) { subProject ->
                +"include("; +subProject.quotified; +")"
            }
            nl(lineBreaks = 2)
        }
    }
}