package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.Reader

import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.NonDefaultTargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module

interface NativeTargetConfigurator : TargetConfigurator {
    val isDesktopTarget: Boolean
    val isIosTarget: Boolean
}

class RealNativeTargetConfigurator private constructor(
    override val moduleSubType: ModuleSubType
) : NativeTargetConfigurator, SimpleTargetConfigurator {
    override val text: String = moduleSubType.name.capitalize()
    override val isDesktopTarget: Boolean
        get() = moduleSubType.isNativeDesktop

    override val isIosTarget: Boolean
        get() = moduleSubType.isIOS

    override fun createInnerTargetIrs(reader: Reader, module: Module): List<BuildSystemIR> = if (moduleSubType.isIOS) irsList {
        "binaries" {
            "framework"  {
                "baseName" assign const(module.parent!!.name)
            }
        }
    } else emptyList()

    companion object {
        val configurators = ModuleSubType.values()
            .filter { it.moduleType == ModuleType.native }
            .map(::RealNativeTargetConfigurator)

        val configuratorsByModuleType = configurators.associateBy { it.moduleSubType }
    }
}

object NativeForCurrentSystemTarget : NativeTargetConfigurator, SingleCoexistenceTargetConfigurator {
    override val moduleType = ModuleType.native
    override val isDesktopTarget: Boolean = true
    override val isIosTarget: Boolean = false

    @NonNls
    override val id = "nativeForCurrentSystem"

    override val text = KotlinNewProjectWizardBundle.message("module.configurator.native.for.current.system")


    override fun Reader.createTargetIrs(
        module: Module
    ): List<BuildSystemIR> {
        val moduleName = module.name
        val variableName = "${moduleName}Target"

        return irsList {
            "hostOs" createValue raw("System.getProperty(\"os.name\")")
            "isMingwX64" createValue raw("hostOs.startsWith(\"Windows\")")

            addRaw {
                when (dsl) {
                    GradlePrinter.GradleDsl.KOTLIN -> {
                        +"val $variableName = when "
                        inBrackets {
                            indent()
                            +"""hostOs == "Mac OS X" -> macosX64("$moduleName")"""; nlIndented()
                            +"""hostOs == "Linux" -> linuxX64("$moduleName")"""; nlIndented()
                            +"""isMingwX64 -> mingwX64("$moduleName")"""; nlIndented()
                            +"""else -> throw GradleException("Host OS is not supported in Kotlin/Native.")"""
                        }
                    }
                    GradlePrinter.GradleDsl.GROOVY -> {
                        +"""KotlinNativeTargetWithTests $variableName"""; nlIndented()
                        +"""if (hostOs == "Mac OS X") $variableName = macosX64('$moduleName')"""; nlIndented()
                        +"""else if (hostOs == "Linux") $variableName = linuxX64("$moduleName")"""; nlIndented()
                        +"""else if (isMingwX64) $variableName = mingwX64("$moduleName")"""; nlIndented()
                        +"""else throw new GradleException("Host OS is not supported in Kotlin/Native.")""";
                    }
                }
                nl()
            }

            +NonDefaultTargetConfigurationIR(
                variableName = variableName,
                targetName = moduleName,
                irs = createInnerTargetIrs(this@createTargetIrs, module).toPersistentList()
            )
        }
    }

    override fun createBuildFileIRs(
        reader: Reader,
        configurationData: ModulesToIrConversionData,
        module: Module
    ): List<BuildSystemIR> = irsList {
        if (configurationData.buildSystemType == BuildSystemType.GradleGroovyDsl) {
            import("org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests")
        }
    }
}
