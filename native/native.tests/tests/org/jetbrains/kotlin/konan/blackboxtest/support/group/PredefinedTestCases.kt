/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.group

import org.jetbrains.kotlin.konan.blackboxtest.support.settings.TestSettings

@Target(AnnotationTarget.CLASS)
@TestSettings(providerClass = PredefinedTestCaseGroupProvider::class)
annotation class PredefinedTestCases(vararg val testCases: PredefinedTestCase)

@Target()
annotation class PredefinedTestCase(val name: String, val freeCompilerArgs: Array<String>, val sourceLocations: Array<String>)
