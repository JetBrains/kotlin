/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension

val KotlinPm20ProjectExtension.instrumentedTest
    get() = modules.maybeCreate("instrumentedTest")

fun KotlinPm20ProjectExtension.instrumentedTest(configure: GradleKpmModule.() -> Unit) {
    instrumentedTest.apply(configure)
}
