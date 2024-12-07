/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import kotlin.reflect.KProperty1
import kotlin.test.fail

fun <T : CommonToolArguments, R> T.assertNotNull(property: KProperty1<T, R?>): R {
    return property.get(this) ?: fail("Missing '${property.name}'")
}