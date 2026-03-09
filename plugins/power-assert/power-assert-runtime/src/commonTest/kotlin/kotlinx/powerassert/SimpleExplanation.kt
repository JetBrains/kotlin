/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

@ExperimentalPowerAssert
class SimpleExplanation(
    override val offset: Int,
    override val source: String,
    override val expressions: List<Expression>,
) : Explanation() {
    override fun toString(): String {
        return "SimpleExplanation(offset=$offset, source='$source', expressions=$expressions)"
    }
}
