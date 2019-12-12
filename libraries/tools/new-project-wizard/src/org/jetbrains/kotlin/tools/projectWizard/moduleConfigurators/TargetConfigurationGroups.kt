package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType

object TargetConfigurationGroups {
    val JVM = FinalTargetConfiguratorGroup(
        ModuleType.jvm.projectTypeName,
        ModuleType.jvm,
        listOf(
            JvmTargetConfigurator,
            AndroidTargetConfigurator
        )
    )

    val JS = FinalTargetConfiguratorGroup(
        ModuleType.js.projectTypeName,
        ModuleType.js,
        listOf(
            JsBrowserTargetConfigurator,
            JsNodeTargetConfigurator
        )
    )

    object NATIVE {
        val LINUX = FinalTargetConfiguratorGroup(
            "Linux",
            ModuleType.native,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.linuxX64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.linuxArm32Hfp),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.linuxMips32),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.linuxMipsel32)
            )
        )

        val WINDOWS = FinalTargetConfiguratorGroup(
            "Windows (MinGW)",
            ModuleType.native,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.mingwX64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.mingwX86)
            )
        )

        val MAC = FinalTargetConfiguratorGroup(
            "macOS",
            ModuleType.native,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.macosX64)
            )
        )

        val IOS = FinalTargetConfiguratorGroup(
            "iOS",
            ModuleType.native,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.iosArm32),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.iosArm64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.iosX64)
            )
        )

        val ANDROID_NATIVE = FinalTargetConfiguratorGroup(
            "Android Native",
            ModuleType.native,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.androidNativeArm64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.androidNativeArm32)
            )
        )

        val ALL = StepTargetConfiguratorGroup(
            ModuleType.native.projectTypeName,
            ModuleType.native,
            listOf(
                NativeForCurrentSystemTarget,
                LINUX,
                WINDOWS,
                MAC,
                IOS,
                ANDROID_NATIVE
            )
        )
    }

    val FIRST = FirstStepTargetConfiguratorGroup(
        listOf(
            CommonTargetConfigurator,
            JVM,
            NATIVE.ALL,
            JS
        )
    )
}