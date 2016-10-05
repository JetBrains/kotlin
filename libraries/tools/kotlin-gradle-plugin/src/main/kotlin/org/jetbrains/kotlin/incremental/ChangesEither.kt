package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.name.FqName

internal sealed class ChangesEither {
    internal class Known(
            val lookupSymbols: Collection<LookupSymbol> = emptyList(),
            val fqNames: Collection<FqName> = emptyList()
    ) : ChangesEither()
    internal class Unknown : ChangesEither()
}