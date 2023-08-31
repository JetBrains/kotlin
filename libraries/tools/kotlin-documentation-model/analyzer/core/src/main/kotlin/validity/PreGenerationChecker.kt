/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.validity

public fun interface PreGenerationChecker : () -> PreGenerationCheckerOutput {

    override fun invoke(): PreGenerationCheckerOutput
}

public data class PreGenerationCheckerOutput(val result: Boolean, val messages: List<String>) {

    public operator fun plus(pair: Pair<Boolean, List<String>>): Pair<Boolean, List<String>> {
        return Pair(result && pair.first, messages + pair.second)
    }
}
