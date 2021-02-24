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
import org.jetbrains.kotlin.tools.projectWizard.mpp.mppSources
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectPath
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.JavaPackage
import org.jetbrains.kotlin.tools.projectWizard.settings.javaPackage
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptor
import org.jetbrains.kotlin.tools.projectWizard.templates.asSrcOf
import java.nio.file.Path
import kotlin.reflect.KClass

sealed class ModuleDependencyType(
    val from: KClass<out ModuleConfigurator>,
    val to: KClass<out ModuleConfigurator>
) {
    fun accepts(from: Module, to: Module) =
        this.from.isInstance(from.configurator)
                && this.to.isInstance(to.configurator)
                && additionalAcceptanceChecker(from, to)

    open fun additionalAcceptanceChecker(from: Module, to: Module) = true

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

    open fun Writer.runArbitraryTask(
        from: Module,
        to: Module,
        toModulePath: Path,
        data: ModulesToIrConversionData
    ): TaskResult<Unit> =
        UNIT_SUCCESS

    open fun Writer.runArbitraryTaskBeforeIRsCreated(
        from: Module,
        to: Module,
    ): TaskResult<Unit> =
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

    object JVMSinglePlatformToMPP : ModuleDependencyType(
        from = JvmSinglePlatformModuleConfigurator::class,
        to = MppModuleConfigurator::class
    )

    object JVMTargetToMPP : ModuleDependencyType(
        from = JvmTargetConfigurator::class,
        to = MppModuleConfigurator::class
    ) {
        override fun additionalAcceptanceChecker(from: Module, to: Module): Boolean =
            from !in to.subModules
    }

    object IOSToMppSinglePlatformToMPP : ModuleDependencyType(
        from = IOSSinglePlatformModuleConfigurator::class,
        to = MppModuleConfigurator::class
    ) {
        override fun createDependencyIrs(from: Module, to: Module, data: ModulesToIrConversionData): List<BuildSystemIR> =
            emptyList()

        override fun Writer.runArbitraryTask(
            from: Module,
            to: Module,
            toModulePath: Path,
            data: ModulesToIrConversionData
        ): TaskResult<Unit> = compute {
            inContextOfModuleConfigurator(from) {
                IOSSinglePlatformModuleConfigurator.dependentModule.reference.update {
                    IOSSinglePlatformModuleConfigurator.DependentModuleReference(to).asSuccess()
                }
            }.ensure()
            addDummyFileIfNeeded(to, toModulePath).ensure()
        }

        private fun Writer.addDummyFileIfNeeded(
            to: Module,
            toModulePath: Path,
        ): TaskResult<Unit> {
            val needDummyFile = false/*TODO*/
            return if (needDummyFile) {
                val dummyFilePath =
                    Defaults.SRC_DIR / "${to.iosTargetSafe()!!.name}Main" / to.configurator.kotlinDirectoryName / "dummyFile.kt"
                TemplatesPlugin.addFileTemplate.execute(
                    FileTemplate(
                        FileTemplateDescriptor("ios/dummyFile.kt", dummyFilePath),
                        projectPath / toModulePath
                    )
                )
            } else UNIT_SUCCESS
        }

        override fun additionalAcceptanceChecker(from: Module, to: Module): Boolean =
            to.iosTargetSafe() != null

        private fun Module.iosTargetSafe(): Module? = subModules.firstOrNull { module ->
            module.configurator.safeAs<NativeTargetConfigurator>()?.isIosTarget == true
        }

        @OptIn(ExperimentalStdlibApi::class)
        override fun Reader.createToIRs(from: Module, to: Module, data: ModulesToIrConversionData): TaskResult<List<BuildSystemIR>> {
            val iosTargetName = to.iosTargetSafe()?.name
                ?: return Failure(InvalidModuleDependencyError(from, to, "Module ${to.name} should contain at least one iOS target"))


            return irsList {
                +GradleConfigureTaskIR(GradleByClassTasksCreateIR("packForXcode", "Sync")) {
                    "group" assign const("build")
                    "mode" createValue GradleBinaryExpressionIR(
                        raw { +"System.getenv("; +"CONFIGURATION".quotified; +")" },
                        "?:",
                        const("DEBUG")
                    )
                    "sdkName" createValue GradleBinaryExpressionIR(
                        raw { +"System.getenv("; +"SDK_NAME".quotified; +")" },
                        "?:",
                        const("iphonesimulator")
                    )
                    "targetName" createValue raw {
                        +iosTargetName.quotified
                        when (dsl) {
                            GradlePrinter.GradleDsl.KOTLIN -> +""" + if (sdkName.startsWith("iphoneos")) "Arm64" else "X64""""
                            GradlePrinter.GradleDsl.GROOVY -> +""" + (sdkName.startsWith('iphoneos') ? 'Arm64' : 'X64')"""
                        }
                    }
                    "framework" createValue raw {
                        +"kotlin.targets"
                        when (dsl) {
                            GradlePrinter.GradleDsl.KOTLIN -> +""".getByName<KotlinNativeTarget>(targetName)"""
                            GradlePrinter.GradleDsl.GROOVY -> +"""[targetName]"""
                        }
                        +".binaries.getFramework(mode)"
                    };

                    addRaw { +"inputs.property(${"mode".quotified}, mode)" }
                    addRaw("dependsOn(framework.linkTask)")
                    "targetDir" createValue new("File", raw("buildDir"), const("xcode-frameworks"))
                    addRaw("from({ framework.outputDirectory })")
                    addRaw("into(targetDir)")
                }
                addRaw { +"""tasks.getByName(${"build".quotified}).dependsOn(packForXcode)""" }
                import("org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget")

            }.asSuccess()
        }
    }

    companion object {
        private val ALL = listOf(
            JVMSinglePlatformToJVMSinglePlatform,
            JVMSinglePlatformToMPP,
            AndroidSinglePlatformToMPP,
            JVMTargetToMPP,
            IOSToMppSinglePlatformToMPP
        )

        fun getPossibleDependencyType(from: Module, to: Module): ModuleDependencyType? =
            ALL.firstOrNull { it.accepts(from, to) }

        fun isDependencyPossible(from: Module, to: Module): Boolean =
            getPossibleDependencyType(from, to) != null
    }
}