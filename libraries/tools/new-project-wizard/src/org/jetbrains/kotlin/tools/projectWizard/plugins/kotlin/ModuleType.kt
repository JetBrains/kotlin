package org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin

import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.StdlibType
import java.util.*

//TODO maybe split into interfaces like it made with module configurators
@Suppress("EnumEntryName")
enum class ModuleType(val projectTypeName: String) {
    jvm(KotlinNewProjectWizardBundle.message("module.type.jvm")),
    js(KotlinNewProjectWizardBundle.message("module.type.js")),
    native(KotlinNewProjectWizardBundle.message("module.type.native")),
    common(KotlinNewProjectWizardBundle.message("module.type.common")),
    android(KotlinNewProjectWizardBundle.message("module.type.android")),
//    ios(KotlinNewProjectWizardBundle.message("module.type.ios"))

    ;

    companion object {
        val ALL = setOf(jvm, js, native, common, android)
    }
}

@Suppress("EnumEntryName", "Unused", "SpellCheckingInspection")
enum class ModuleSubType(val moduleType: ModuleType) {
    jvm(ModuleType.jvm),
    js(ModuleType.js),
    android(ModuleType.android),
    androidNativeArm32(ModuleType.native), androidNativeArm64(ModuleType.native),
    iosArm32(ModuleType.native), iosArm64(ModuleType.native), iosX64(ModuleType.native),
    ios(ModuleType.native)/*TODO TEMPORARY TILL HMPP WIZARD PART IS MERGED*/,
    linuxArm32Hfp(ModuleType.native), linuxMips32(ModuleType.native), linuxMipsel32(ModuleType.native),
    linuxX64(ModuleType.native),
    macosX64(ModuleType.native),
    mingwX64(ModuleType.native), mingwX86(ModuleType.native),
    common(ModuleType.common)
}

val ModuleSubType.isIOS: Boolean
    get() = this in EnumSet.of(ModuleSubType.iosX64, ModuleSubType.iosArm32, ModuleSubType.iosArm64, ModuleSubType.ios)

val ModuleSubType.isNativeDesktop: Boolean
    get() = this in EnumSet.of(
        ModuleSubType.linuxX64,
        ModuleSubType.macosX64,
        ModuleSubType.mingwX64, ModuleSubType.mingwX86
    )


fun ModuleType.correspondingStdlib(): StdlibType? = when (this) {
    ModuleType.jvm -> StdlibType.StdlibJdk8
    ModuleType.js -> StdlibType.StdlibJs
    ModuleType.native -> null
    ModuleType.common -> StdlibType.StdlibCommon
    ModuleType.android -> StdlibType.StdlibJdk7
}

