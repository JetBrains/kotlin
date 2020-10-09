/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repository
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

interface PluginManagementIR : GradleIR

data class PluginManagementRepositoryIR(val repositoryIR: RepositoryIR) : PluginManagementIR, RepositoryWrapper {
    override fun GradlePrinter.renderGradle() {
        repositoryIR.render(this)
    }

    override val repository: Repository
        get() = repositoryIR.repository
}