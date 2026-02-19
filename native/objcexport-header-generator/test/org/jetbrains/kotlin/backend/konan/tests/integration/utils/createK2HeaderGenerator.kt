/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests.integration.utils

import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator

internal fun createK2HeaderGenerator(): HeaderGenerator {
    val clazz = Class.forName("org.jetbrains.kotlin.objcexport.testUtils.AnalysisApiHeaderGenerator")
    val field = clazz.getDeclaredField("INSTANCE")
    return field.get(null) as HeaderGenerator
}