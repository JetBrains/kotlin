/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import org.jetbrains.uast.test.common.kotlin.FirLegacyUastIdentifiersTestBase

abstract class AbstractFE1LegacyUastIdentifiersTest : AbstractFE1UastIdentifiersTest(), FirLegacyUastIdentifiersTestBase {
    // TODO: better not to see exceptions from legacy UAST
    private val whitelist : Set<String> = setOf(
        "plugins/uast-kotlin/testData/DestructuringDeclaration.kt",
        "plugins/uast-kotlin/testData/LambdaReturn.kt",
        "plugins/uast-kotlin/testData/WhenAndDestructing.kt"
    )
    override fun isExpectedToFail(filePath: String): Boolean {
        return filePath in whitelist
    }
}
