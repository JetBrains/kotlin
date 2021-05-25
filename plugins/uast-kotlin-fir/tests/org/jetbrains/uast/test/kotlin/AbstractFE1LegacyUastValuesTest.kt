/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import org.jetbrains.uast.test.common.kotlin.FirLegacyUastValuesTestBase

abstract class AbstractFE1LegacyUastValuesTest : AbstractFE1UastValuesTest(), FirLegacyUastValuesTestBase {
    // TODO: better not to see exceptions from legacy UAST
    private val whitelist : Set<String> = setOf(
        // TODO: div-by-zero error!
        "plugins/uast-kotlin/testData/Bitwise.kt",
    )
    override fun isExpectedToFail(filePath: String): Boolean {
        return filePath in whitelist
    }
}
