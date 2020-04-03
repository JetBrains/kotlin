/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.asSingletonList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.DependencyType
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.GradleRootProjectDependencyIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleDependencyIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.*
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.isIOS
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import kotlin.collections.buildList
import kotlin.reflect.KClass

sealed class ModuleDependencyType(
    val from: KClass<out ModuleConfigurator>,
    val to: KClass<out ModuleConfigurator>
) {
    fun accepts(from: Module, to: Module) =
        this.from.isInstance(from.configurator) && this.to.isInstance(to.configurator)

    open fun createDependencyIrs(from: Module, to: Module, data: ModulesToIrConversionData): List<BuildSystemIR> {
        val path = to.path
        val modulePomIr = data.pomIr.copy(artifactId = to.name)
        return when {
            data.isSingleRootModuleMode
                    && to.path.parts.singleOrNull() == data.rootModules.single().name
                    && data.buildSystemType.isGradle -> GradleRootProjectDependencyIR(DependencyType.MAIN)
            else -> ModuleDependencyIR(
                path.considerSingleRootModuleMode(data.isSingleRootModuleMode),
                modulePomIr,
                DependencyType.MAIN
            )
        }.asSingletonList()
    }

    open fun SettingsWriter.runArbitraryTask(from: Module, to: Module, data: ModulesToIrConversionData): TaskResult<Unit> =
        UNIT_SUCCESS

    open fun Reader.createToIRs(from: Module, to: Module, data: ModulesToIrConversionData): TaskResult<List<BuildSystemIR>> =
        Success(emptyList())

    object JVMSinglePlatformToJVMSinglePlatform : ModuleDependencyType(
        from = JvmSinglePlatformModuleConfigurator::class,
        to = JvmSinglePlatformModuleConfigurator::class
    )

    object AndroidSinglePlatformToMPP : ModuleDependencyType(
        from = AndroidSinglePlatformModuleConfigurator::class,
        to = MppModuleConfigurator::class
    )

    object IOSToMppSinglePlatformToMPP : ModuleDependencyType(
        from = IOSSinglePlatformModuleConfigurator::class,
        to = MppModuleConfigurator::class
    ) {
        override fun createDependencyIrs(from: Module, to: Module, data: ModulesToIrConversionData): List<BuildSystemIR> =
            emptyList()

        override fun SettingsWriter.runArbitraryTask(
            from: Module,
            to: Module,
            data: ModulesToIrConversionData
        ): TaskResult<Unit> = withSettingsOf(from) {
            IOSSinglePlatformModuleConfigurator.dependentModule.reference
                .setValue(IOSSinglePlatformModuleConfigurator.DependentModuleReference(to))
            UNIT_SUCCESS
        }

        @OptIn(ExperimentalStdlibApi::class)
        override fun Reader.createToIRs(from: Module, to: Module, data: ModulesToIrConversionData): TaskResult<List<BuildSystemIR>> {
            val iosTargetName = to.subModules.firstOrNull { module ->
                module.configurator.safeAs<SimpleTargetConfigurator>()?.moduleSubType?.isIOS == true
            }?.name ?: return Failure(InvalidModuleDependencyError(from, to, "Module ${to.name} should contain at least one iOS target"))

            val packForXcodeTask = GradleConfigureTaskIR(GradleByClassTasksCreateIR("packForXcode", "Sync")) {
                add(GradleAssignmentIR("group", GradleStringConstIR("build")))
                add(
                    CreateGradleValueIR(
                        "mode",
                        GradleBinaryExpressionIR(
                            RawGradleIR { +"System.getenv("; +"CONFIGURATION".quotified; +")" },
                            "?:",
                            GradleStringConstIR("DEBUG")
                        )
                    )
                )

                add(
                    CreateGradleValueIR(
                        "framework",
                        RawGradleIR {
                            +"kotlin.targets."
                            when (dsl) {
                                GradlePrinter.GradleDsl.KOTLIN -> +"""getByName<KotlinNativeTarget>("$iosTargetName")"""
                                GradlePrinter.GradleDsl.GROOVY -> +iosTargetName
                            }
                            +".binaries.getFramework(mode)"

                        }
                    )
                )
                addRawIR { "inputs.property(${"mode".quotified}, mode)" }
                addRawIR { "dependsOn(framework.linkTask)" }
                add(
                    CreateGradleValueIR(
                        "targetDir",
                        GradleCallIr("File", rawIR("buildDir"), GradleStringConstIR("xcode-frameworks"), isConstructorCall = true)
                    )
                )
                addRawIR { "from({ framework.outputDirectory })" }
                addRawIR { "into(targetDir)" }
            }

            val dependency = rawIR { """tasks.getByName("build").dependsOn(packForXcode)""" }

            return buildList {
                add(packForXcodeTask)
                add(dependency)
                add(GradleImportIR("org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget"))
            }.asSuccess()
        }
    }

    companion object {
        private val ALL = listOf(
            JVMSinglePlatformToJVMSinglePlatform,
            AndroidSinglePlatformToMPP,
            IOSToMppSinglePlatformToMPP
        )

        fun getPossibleDependencyType(from: Module, to: Module): ModuleDependencyType? =
            ALL.firstOrNull { it.accepts(from, to) }

        fun isDependencyPossible(from: Module, to: Module): Boolean =
            getPossibleDependencyType(from, to) != null
    }
}