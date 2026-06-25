/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

@Suppress("DEPRECATION_ERROR")
@PublishedApi
@UsedFromCompilerGeneratedCode
@SinceKotlin("2.5")
internal actual fun throwNoWhenBranchMatchedException(subject: Any): Nothing {
    throw NoWhenBranchMatchedException("No branch matched for subject: $subject")
}
