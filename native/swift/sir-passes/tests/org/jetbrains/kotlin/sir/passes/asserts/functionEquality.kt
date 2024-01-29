/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.passes.asserts

import org.jetbrains.kotlin.sir.SirFunction
import kotlin.test.assertEquals

fun assertSirFunctionsEquals(expected: SirFunction, actual: SirFunction) {
    assertEquals(
        actual = actual.parent,
        expected = expected.parent
    )
    assertEquals(
        actual = actual.name,
        expected = expected.name
    )
    assertEquals(
        actual = actual.parameters,
        expected = expected.parameters
    )
    assertEquals(
        actual = actual.returnType,
        expected = expected.returnType
    )
    assertEquals(
        actual = actual.isStatic,
        expected = expected.isStatic
    )
    assertEquals(
        actual = actual.documentation,
        expected = expected.documentation
    )
}
