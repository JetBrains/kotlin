/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

/**
 * Provides information about an expression's location with an [Explanation] and it's result.
 *
 * For example, given the following,
 * ```
 * class Mascot(val name: String)
 * val mascot = Mascot("Kodee")
 * ```
 *
 * Analysis of `mascot.name == "Kodee"` would result in 4 expressions:
 * - An expression with `value=mascot`, `startOffset=0`, `endOffset=6`, and `displayOffset=0`,
 * - An expression with `value=mascot.name`, `startOffset=0`, `endOffset=11`, and `displayOffset=7`,
 * - An expression with `value="Kodee"`, `startOffset=15`, `endOffset=22`, and `displayOffset=15`,
 * - And an expression with `value=mascot.name == "Kodee"`, `startOffset=0`, `endOffset=22`, and `displayOffset=12`.
 */
@ExperimentalPowerAssert
public abstract class Expression internal constructor(
    /**
     * The text character, within [Explanation.source], where the expression source code begins (inclusive).
     */
    public val startOffset: Int,

    /**
     * The text character, within [Explanation.source], where the expression source code ends (exclusive).
     */
    public val endOffset: Int,

    /**
     * The text character, within [Explanation.source], where the expression result is best displayed.
     * This offset will always be in the range `startOffset..<endOffset`.
     */
    public val displayOffset: Int,

    /**
     * The evaluation result of the expression.
     */
    public val value: Any?,
) {
    /**
     * Returns a copy of the expression with all offsets adjusted by [deltaOffset].
     */
    public abstract fun copy(deltaOffset: Int): Expression
}
