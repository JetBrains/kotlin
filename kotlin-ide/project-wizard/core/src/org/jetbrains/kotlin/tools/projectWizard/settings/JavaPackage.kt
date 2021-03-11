package org.jetbrains.kotlin.tools.projectWizard.settings

import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.PomIR
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.TargetConfigurator
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import java.nio.file.Paths

class JavaPackage(val parts: List<String>) {
    constructor(`package`: String) : this(`package`.split('.'))

    fun asPath() = Paths.get(parts.joinToString(separator = "/"))
    fun asCodePackage() = parts.joinToString(separator = ".")

    companion object {
        fun fromCodeRepresentation(codePackage: String, vararg suffixParts: String) =
            JavaPackage(buildList {
                +codePackage.split('.')
                +suffixParts
            })
    }
}

fun Module.javaPackage(pomIr: PomIR): JavaPackage {
    val moduleName = when (configurator) {
        is TargetConfigurator -> parent!!.name
        else -> name
    }
    return JavaPackage.fromCodeRepresentation(pomIr.groupId, moduleName)
}