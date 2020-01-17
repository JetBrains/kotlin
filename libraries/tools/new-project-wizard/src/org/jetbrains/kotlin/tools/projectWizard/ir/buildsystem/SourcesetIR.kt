/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Sourceset
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import java.nio.file.Path


sealed class SourcesetIR : BuildSystemIR {
    abstract val sourcesetType: SourcesetType
    abstract val path: Path
    abstract val original: Sourceset
}

data class SingleplatformSourcesetIR(
    override val sourcesetType: SourcesetType,
    override val path: Path,
    override val irs: List<BuildSystemIR>,
    override val original: Sourceset
) : SourcesetIR(), IrsOwner {
    override fun withReplacedIrs(irs: List<BuildSystemIR>): SingleplatformSourcesetIR = copy(irs = irs)
    override fun BuildFilePrinter.render() = Unit
}

data class MultiplatformSourcesetIR(
    override val sourcesetType: SourcesetType,
    override val path: Path,
    val targetName: String,
    override val irs: List<BuildSystemIR>,
    override val original: Sourceset
) : SourcesetIR(), IrsOwner, GradleIR {
    override fun withReplacedIrs(irs: List<BuildSystemIR>): MultiplatformSourcesetIR = copy(irs = irs)

    override fun GradlePrinter.renderGradle() = getting(sourcesetName, prefix = null) {
        val dependencies = irsOfType<DependencyIR>()
        val needBody = dependencies.isNotEmpty() || dsl == GradlePrinter.GradleDsl.GROOVY
        if (needBody) {
            +" "
            inBrackets {
                if (dependencies.isNotEmpty()) {
                    indent()
                    sectionCall("dependencies", dependencies)
                }
            }
        }
    }
}

val MultiplatformSourcesetIR.sourcesetName
    get() = targetName + sourcesetType.name.capitalize()