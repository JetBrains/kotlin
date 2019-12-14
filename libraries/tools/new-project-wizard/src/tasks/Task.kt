/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package tasks

import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.runTaskIrs
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType

interface Task {
    val name: String
    fun createTaskIRs(moduleIR: ModuleIR, buildSystemType: BuildSystemType): List<BuildSystemIR>
}

class JvmRunTask(private val mainClass: String) : Task {
    override val name: String = "Run task"

    override fun createTaskIRs(moduleIR: ModuleIR, buildSystemType: BuildSystemType): List<BuildSystemIR> = buildList {
        +runTaskIrs(mainClass)
    }
}