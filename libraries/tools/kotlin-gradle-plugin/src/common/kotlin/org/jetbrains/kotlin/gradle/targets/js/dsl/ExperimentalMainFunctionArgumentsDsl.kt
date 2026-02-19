/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import kotlin.RequiresOptIn.Level.WARNING
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * API marker for defining the source of arguments for the main function.
 *
 * Passing arguments to the main function is Experimental.
 * It may be dropped or changed at any time.
 *
 * See https://kotl.in/kotlin-js-pass-arguments-to-main-function
 *
 * @see KotlinJsTargetDsl.passAsArgumentToMainFunction
 * @see KotlinJsNodeDsl.passProcessArgvToMainFunction
 */
@RequiresOptIn(level = WARNING)
@Target(FUNCTION)
annotation class ExperimentalMainFunctionArgumentsDsl
