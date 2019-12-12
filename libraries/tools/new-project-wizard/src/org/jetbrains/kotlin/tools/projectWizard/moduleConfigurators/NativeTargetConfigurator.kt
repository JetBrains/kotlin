package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.CreateGradleValueIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.RawGradleIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.NativeTargetInternalIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.NonDefaultTargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module

interface NativeTargetConfigurator : TargetConfigurator {
    override fun createInnerTargetIrs(module: Module): List<BuildSystemIR> = buildList {
        +NativeTargetInternalIR("MAIN CLASS")
    }
}

class RealNativeTargetConfigurator private constructor(
    override val moduleSubType: ModuleSubType
) : NativeTargetConfigurator, SimpleTargetConfigurator {
    companion object {
        val configurators = ModuleSubType.values()
            .filter { it.moduleType == ModuleType.native }
            .map(::RealNativeTargetConfigurator)

        val configuratorsByModuleType = configurators.associateBy { it.moduleSubType }
    }
}

object NativeForCurrentSystemTarget : NativeTargetConfigurator, SingleCoexistenceTargetConfigurator {
    override val moduleType = ModuleType.native
    override val id = "nativeForCurrentSystem"
    override val text = "For Current System"


    override fun createTargetIrs(module: Module): List<BuildSystemIR> {
        val moduleName = module.name
        return buildList {
            +CreateGradleValueIR("hostOs", RawGradleIR { +"System.getProperty(\"os.name\")" })
            +CreateGradleValueIR("isMingwX64", RawGradleIR { +"hostOs.startsWith(\"Windows\")" })

            //TODO do not use RawGradleIR here
            +RawGradleIR {
                when (dsl) {
                    GradlePrinter.GradleDsl.KOTLIN -> {
                        +"val $DEFAULT_TARGET_VARIABLE_NAME = when "
                        inBrackets {
                            indent()
                            +"""hostOs == "Mac OS X" -> macosX64("$moduleName")"""; nlIndented()
                            +"""hostOs == "Linux" -> linuxX64("$moduleName")"""; nlIndented()
                            +"""isMingwX64 -> mingwX64("$moduleName")"""; nlIndented()
                            +"""else -> throw GradleException("Host OS is not supported in Kotlin/Native.")"""
                        }
                    }
                    GradlePrinter.GradleDsl.GROOVY -> {
                        +"""org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests $DEFAULT_TARGET_VARIABLE_NAME"""; nlIndented()
                        +"""if (hostOs == "Mac OS X") $DEFAULT_TARGET_VARIABLE_NAME = macosX64('$moduleName')"""; nlIndented()
                        +"""else if (hostOs == "Linux") $DEFAULT_TARGET_VARIABLE_NAME = linuxX64("$moduleName")"""; nlIndented()
                        +"""else if (isMingwX64) return $DEFAULT_TARGET_VARIABLE_NAME = mingwX64("$moduleName")"""; nlIndented()
                        +"""else throw new GradleException("Host OS is not supported in Kotlin/Native.")""";
                    }
                }
                nl()
            }



            +NonDefaultTargetConfigurationIR(
                DEFAULT_TARGET_VARIABLE_NAME,
                createInnerTargetIrs(module)
            )
        }
    }

    private const val DEFAULT_TARGET_VARIABLE_NAME = "nativeTarget"
}
