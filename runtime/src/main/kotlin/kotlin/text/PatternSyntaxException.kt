/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

/**
 * Encapsulates a syntax error that occurred during the compilation of a
 * [Pattern]. Might include a detailed description, the original regular
 * expression, and the index at which the error occurred.
 */
internal class PatternSyntaxException(val description: String = "", val pattern: String = "", index: Int = -1)
    : IllegalArgumentException("Error in \"$pattern\" ($index). $description")
