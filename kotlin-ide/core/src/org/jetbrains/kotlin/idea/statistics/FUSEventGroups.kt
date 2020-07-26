/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

/* Note: along with adding a group to this enum you should also add its GROUP_ID to plugin.xml and get it whitelisted
 * (see https://confluence.jetbrains.com/display/FUS/IntelliJ+Reporting+API).
 *
 * Default value for [events] parameter is intended for collectors which
 *    1. don't have yet a set of allowed values for FUS Whitelist
 *    2. set of possible values is defined by enums in a feature's source code
 */
enum class FUSEventGroups(groupIdSuffix: String, val events: Set<String> = setOf()) {

    GradleTarget("gradle.target", gradleTargetEvents),
    Refactoring("ide.refactoring", refactoringEvents),
    Debug("ide.debugger"),
    J2K("ide.j2k"),
    Editor("ide.editor"),
    Settings("ide.settings"),
    GradlePerformance("gradle.performance"),
    NewWizard("ide.new.wizard"),
    MigrationTool("ide.migrationTool");

    val GROUP_ID: String = "kotlin.$groupIdSuffix"
}

@Suppress("EnumEntryName")
enum class GradleStatisticsEvents {
    Environment,
    Kapt,
    CompilerPlugins,
    MPP,
    Libraries,
    GradleConfiguration,
    ComponentVersions,
    KotlinFeatures,
    GradlePerformance,
    UseScenarios
}

val gradleTargetEvents = setOf(
    "kotlin-android",
    "kotlin-platform-common",
    "kotlin-platform-js",
    "kotlin-platform-jvm",
    "MPP.androidJvm",
    "MPP.androidJvm.android",
    "MPP.common",
    "MPP.common.metadata",
    "MPP.js",
    "MPP.js.js",
    "MPP.jvm",
    "MPP.jvm.jvm",
    "MPP.jvm.jvmWithJava",
    "MPP.native",
    "MPP.native.androidNativeArm32",
    "MPP.native.androidNativeArm64",
    "MPP.native.iosArm32",
    "MPP.native.iosArm64",
    "MPP.native.iosX64",
    "MPP.native.linuxArm32Hfp",
    "MPP.native.linuxArm64",
    "MPP.native.linuxMips32",
    "MPP.native.linuxMipsel32",
    "MPP.native.linuxX64",
    "MPP.native.macosX64",
    "MPP.native.mingwX64",
    "MPP.native.mingwX86",
    "MPP.native.wasm32",
    "MPP.native.zephyrStm32f4Disco"
)
val refactoringEvents = setOf(
    "RenameKotlinFileProcessor",
    "RenameKotlinFunctionProcessor",
    "RenameKotlinPropertyProcessor",
    "RenameKotlinPropertyProcessor",
    "RenameKotlinClassifierProcessor",
    "RenameKotlinClassifierProcessor",
    "RenameKotlinClassifierProcessor",
    "RenameKotlinParameterProcessor",
    "JavaMemberByKotlinReferenceInplaceRenameHandler",
    "KotlinPushDownHandler",
    "KotlinPullUpHandler"
)
