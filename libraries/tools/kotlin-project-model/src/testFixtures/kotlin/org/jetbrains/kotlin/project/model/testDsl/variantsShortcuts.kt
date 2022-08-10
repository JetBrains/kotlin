/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.project.model.testDsl

import org.jetbrains.kotlin.project.model.KotlinNativeTargetAttribute
import org.jetbrains.kotlin.project.model.KotlinPlatformTypeAttribute
import org.jetbrains.kotlin.project.model.infra.TestKpmModule
import org.jetbrains.kotlin.project.model.infra.TestKpmVariant

fun TestKpmModule.jvm(configure: TestKpmVariant.() -> Unit = { }) =
    variant("jvm", KotlinPlatformTypeAttribute.JVM, configure = configure)

fun TestKpmModule.js(configure: TestKpmVariant.() -> Unit = { }) =
    variant("jvm", KotlinPlatformTypeAttribute.JS, configure = configure)

fun TestKpmModule.android(configure: TestKpmVariant.() -> Unit = { }) =
    variant("android", KotlinPlatformTypeAttribute.ANDROID_JVM, configure = configure)

fun TestKpmModule.nativeVariant(name: String, nativeTarget: String, configure: TestKpmVariant.() -> Unit = { }): TestKpmVariant {
    val variant = variant(name, KotlinPlatformTypeAttribute.NATIVE)
    variant.variantAttributes[KotlinNativeTargetAttribute] = nativeTarget
    variant.configure()
    return variant
}

fun TestKpmModule.linux(configure: TestKpmVariant.() -> Unit = { }) = nativeVariant("linux", "linux", configure)
fun TestKpmModule.macosX64(configure: TestKpmVariant.() -> Unit = { }) = nativeVariant("macosX64", "macosX64", configure)
