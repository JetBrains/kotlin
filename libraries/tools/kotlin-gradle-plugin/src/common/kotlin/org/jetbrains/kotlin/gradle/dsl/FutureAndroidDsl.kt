/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.gradle.kotlin.dsl

import org.gradle.api.Action
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinTarget

abstract class KotlinMultiplatformAndroidTarget(project: Project) : AbstractKotlinTarget(project)


val KotlinMultiplatformExtension.`android`: KotlinMultiplatformAndroidTarget get() = TODO("new android property is called")

/**
 * Configures the [android][com.android.build.gradle.LibraryExtension] extension.
 */
fun KotlinMultiplatformExtension.`android`(configure: Action<KotlinMultiplatformAndroidTarget>): Unit = println("New function is called!!")
