/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.uast

import com.intellij.psi.PsiType

internal val ERROR_NAME = "<error>"

internal val LINE_SEPARATOR = System.getProperty("line.separator") ?: "\n"

val String.withMargin: String
    get() = lines().joinToString(LINE_SEPARATOR) { "    " + it }

internal operator fun String.times(n: Int) = this.repeat(n)

internal fun List<UElement>.asLogString() = joinToString(LINE_SEPARATOR) { it.asLogString().withMargin }

internal fun StringBuilder.appendWithSpace(s: String) {
    if (s.isNotEmpty()) {
        append(s)
        append(' ')
    }
}

internal tailrec fun UExpression.unwrapParenthesis(): UExpression = when (this) {
    is UParenthesizedExpression -> expression.unwrapParenthesis()
    else -> this
}

internal fun <T> lz(f: () -> T) = lazy(LazyThreadSafetyMode.NONE, f)

internal val PsiType.name: String
    get() = getCanonicalText(false)