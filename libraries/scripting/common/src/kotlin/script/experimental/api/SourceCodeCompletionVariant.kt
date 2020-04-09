/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

data class SourceCodeCompletionVariant(
    val text: String,
    val displayText: String,
    val tail: String,
    val icon: String
)