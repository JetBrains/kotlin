/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.passes.util

import org.jetbrains.kotlin.sir.SirElement
import org.jetbrains.sir.passes.SirPass
import org.jetbrains.sir.passes.utility.assertValid

fun <S : SirElement, R, T> SirPass<S, T, R>.runWithAsserts(element: S, data: T): R {
    element.assertValid()
    val result = run(element, data)
    if (result is SirElement) {
        result.assertValid()
    }
    return result
}