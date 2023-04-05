/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

val KotlinMultiplatformExtension.commonMain: KotlinSourceSet
    get() = sourceSets.getByName("commonMain")

val KotlinMultiplatformExtension.commonTest: KotlinSourceSet
    get() = sourceSets.getByName("commonTest")

val KotlinMultiplatformExtension.jvmMain: KotlinSourceSet
    get() = sourceSets.getByName("jvmMain")

val KotlinMultiplatformExtension.jvmTest: KotlinSourceSet
    get() = sourceSets.getByName("jvmTest")

val KotlinMultiplatformExtension.androidMain: KotlinSourceSet
    get() = sourceSets.getByName("androidMain")

val KotlinMultiplatformExtension.androidTest: KotlinSourceSet
    get() = sourceSets.getByName("androidTest")

val KotlinMultiplatformExtension.iosMain: KotlinSourceSet
    get() = sourceSets.getByName("iosMain")

val KotlinMultiplatformExtension.iosTest: KotlinSourceSet
    get() = sourceSets.getByName("iosTest")

operator fun KotlinSourceSet.invoke(config: KotlinSourceSet.() -> Unit) = config()
