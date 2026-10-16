/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.validity

internal fun interface PreGenerationChecker : () -> PreGenerationCheckerOutput {

    override fun invoke(): PreGenerationCheckerOutput
}

internal data class PreGenerationCheckerOutput(val result: Boolean, val messages: List<String>) {

    operator fun plus(pair: Pair<Boolean, List<String>>): Pair<Boolean, List<String>> {
        return Pair(result && pair.first, messages + pair.second)
    }
}
