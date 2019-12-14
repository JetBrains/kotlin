package org.jetbrains.kotlin.tools.projectWizard.ir

import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.FreeIR

interface IR

interface TaskIR : FreeIR {
    val name: String
}