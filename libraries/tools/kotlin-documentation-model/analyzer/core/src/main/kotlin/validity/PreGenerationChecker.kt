/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.validity

fun interface PreGenerationChecker : () -> PreGenerationCheckerOutput {

    override fun invoke(): PreGenerationCheckerOutput
}

data class PreGenerationCheckerOutput(val result: Boolean, val messages: List<String>) {

    operator fun plus(pair: Pair<Boolean, List<String>>) =
        Pair(result && pair.first, messages + pair.second)
}
