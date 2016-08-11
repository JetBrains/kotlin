package org.jetbrains.kotlin.gradle.tasks

import org.jetbrains.kotlin.incremental.LookupSymbol

internal sealed class ChangesEither {
    internal class Known(val lookupSymbols: Set<LookupSymbol>) : ChangesEither()
    internal class Unknown : ChangesEither()
}