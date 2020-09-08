package org.jetbrains.dokka.validity

interface PreGenerationChecker : () -> PreGenerationCheckerOutput {

    override fun invoke(): PreGenerationCheckerOutput
}

data class PreGenerationCheckerOutput(val result: Boolean, val messages: List<String>) {

    operator fun plus(pair: Pair<Boolean, List<String>>) =
        Pair(result && pair.first, messages + pair.second)
}