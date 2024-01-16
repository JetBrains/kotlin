/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.konan.test.blackbox.support.group

import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty

// Deprecated: Use test directives instead:
// `IGNORE_BACKEND: NATIVE`, `IGNORE_BACKEND_K1: NATIVE`, `IGNORE_BACKEND_K2: NATIVE` if test fails in any test config
// `IGNORE_NATIVE`, `IGNORE_NATIVE_K1`, `IGNORE_NATIVE_K2` with/without a property matcher, for usual easy fails (please provide issue link)
// `DISABLE_NATIVE`, `DISABLE_NATIVE_K1`, `DISABLE_NATIVE_K2` with/without a property matcher, in case compiler crashes within JVM, or compilation/execution would be a resource waste
@Target(AnnotationTarget.CLASS)
internal annotation class DisabledTests(
    val sourceLocations: Array<String>
)

// @DisabledTestsIfProperty(...) is intended primarily to turn off tests in bulk to reduce pressure on CI infrastructure for certain targets
// To mark failed tests, please use the following test directives instead:
// `IGNORE_NATIVE`, `IGNORE_NATIVE_K1`, `IGNORE_NATIVE_K2` with/without a property matcher, for usual easy fails (please provide issue link)
// `DISABLE_NATIVE`, `DISABLE_NATIVE_K1`, `DISABLE_NATIVE_K2` with/without a property matcher, in case compiler crashes within JVM, or compilation/execution would be a resource waste
@Target(AnnotationTarget.CLASS)
internal annotation class DisabledTestsIfProperty(
    val sourceLocations: Array<String>,
    val property: ClassLevelProperty,
    val propertyValue: String
)
