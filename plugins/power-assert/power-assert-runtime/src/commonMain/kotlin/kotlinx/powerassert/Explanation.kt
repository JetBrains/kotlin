/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

/**
 * Provides information about a section of source code and its evaluation.
 */
@ExperimentalPowerAssert
public abstract class Explanation internal constructor() {
    /**
     * The source text character offset within the containing file.
     * Offset will always be at column 0 within the containing file,
     * as all indentation is preserved by [source].
     */
    public abstract val offset: Int

    /**
     * The source code text block. Text is provided with all original indentation preserved,
     * but unrelated source code is redacted with whitespace characters.
     */
    public abstract val source: String

    /**
     * All [Expression]s which were evaluated as part of this explanation.
     * Expressions are provided in evaluation order.
     */
    public abstract val expressions: List<Expression>
}
