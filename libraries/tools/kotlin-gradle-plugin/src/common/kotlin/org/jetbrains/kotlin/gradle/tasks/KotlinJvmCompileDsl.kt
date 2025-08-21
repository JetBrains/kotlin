/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

/**
 * Workaround for deprecation warning in import.
 */
@Suppress("DEPRECATION_ERROR")
typealias KotlinJvmCompileDsl = org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
