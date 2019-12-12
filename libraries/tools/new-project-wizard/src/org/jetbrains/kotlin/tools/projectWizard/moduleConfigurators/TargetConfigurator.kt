package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.RawGradleIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.DefaultTargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetAccessIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind


interface TargetConfigurator : ModuleConfigurator {
    override val moduleKind get() = ModuleKind.target

    fun canCoexistsWith(other: List<TargetConfigurator>): Boolean = true

    fun createTargetIrs(module: Module): List<BuildSystemIR>
    fun createInnerTargetIrs(module: Module): List<BuildSystemIR> = emptyList()
}

interface SingleCoexistenceTargetConfigurator : TargetConfigurator {
    override fun canCoexistsWith(other: List<TargetConfigurator>): Boolean =
        other.none { it == this }
}

interface SimpleTargetConfigurator : TargetConfigurator {
    val moduleSubType: ModuleSubType
    override val moduleType get() = moduleSubType.moduleType
    override val id get() = "${moduleSubType.name}Target"
    override val text get() = moduleSubType.name.capitalize()

    override val suggestedModuleName: String? get() = moduleSubType.name


    override fun createTargetIrs(module: Module): List<BuildSystemIR> = buildList {
        +DefaultTargetConfigurationIR(
            module.createTargetAccessIr(moduleSubType),
            createInnerTargetIrs(module)
        )
    }
}

private fun Module.createTargetAccessIr(moduleSubType: ModuleSubType) =
    TargetAccessIR(
        moduleSubType,
        name.takeIf { it != moduleSubType.name }
    )


interface JsTargetConfigurator : TargetConfigurator {
    override val moduleType: ModuleType get() = ModuleType.js
}

object JsBrowserTargetConfigurator : JsTargetConfigurator {
    override val id = "jsBrowser"
    override val text = "Browser"

    override fun createTargetIrs(module: Module): List<BuildSystemIR> = buildList {
        +DefaultTargetConfigurationIR(
            module.createTargetAccessIr(ModuleSubType.js),
            buildList {
                +RawGradleIR {
                    sectionCall("browser") {}
                }
            }
        )
    }
}

object JsNodeTargetConfigurator : JsTargetConfigurator {
    override val id = "jsNode"
    override val text = "Node.js"

    override fun createTargetIrs(module: Module): List<BuildSystemIR> = buildList {
        +DefaultTargetConfigurationIR(
            module.createTargetAccessIr(ModuleSubType.js),
            buildList {
                +RawGradleIR {
                    sectionCall("nodejs") {}
                }
            }
        )
    }
}

object CommonTargetConfigurator : SimpleTargetConfigurator, SingleCoexistenceTargetConfigurator {
    override val moduleSubType = ModuleSubType.common
}

object JvmTargetConfigurator : TargetConfigurator,
    SimpleTargetConfigurator,
    JvmModuleConfigurator,
    SingleCoexistenceTargetConfigurator {
    override val moduleSubType = ModuleSubType.jvm
}

object AndroidTargetConfigurator : TargetConfigurator,
    SimpleTargetConfigurator,
    AndroidModuleConfigurator,
    SingleCoexistenceTargetConfigurator {
    override val moduleSubType = ModuleSubType.android
}