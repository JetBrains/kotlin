/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.util

import kotlin.script.experimental.api.EvaluatedSnippet

val EvaluatedSnippet.isValueResult: Boolean
    get() = hasResult && error == null

val EvaluatedSnippet.isUnitResult: Boolean
    get() = !hasResult && error == null

val EvaluatedSnippet.isErrorResult: Boolean
    get() = error != null