package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaDeprecation
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

/**
 * Returns `deprecated`, `unavailable` or null
 *
 * Objective-C method, property or constructor may have two attributes
 * - `deprecated` which is returned when [KaSymbol] is `@Deprecated` and has level [KaDeprecation.Level.WARNING]
 * - `unavailable` which is returned when [KaSymbol] is `@Deprecated` and has level [KaDeprecation.Level.WARNING] or [KaDeprecation.Level.HIDDEN]
 *
 * Edge case when [KaSymbol] is constructor, then [getObjCDeprecationStatus] is called on containing symbol.
 *
 * See K1 [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapper.getDeprecation]
 */
@OptIn(KaExperimentalApi::class)
internal fun KaSession.getObjCDeprecationStatus(symbol: KaSymbol): String? {
    return symbol.deprecation?.toDeprecationAttribute() ?: if (symbol.isConstructor) {
        symbol.containingDeclaration?.deprecation?.toDeprecationAttribute()
    } else null
}

@KaExperimentalApi
private fun KaDeprecation.toDeprecationAttribute(): String {
    val attribute = when (level) {
        KaDeprecation.Level.WARNING -> "deprecated"
        KaDeprecation.Level.ERROR, KaDeprecation.Level.HIDDEN -> "unavailable"
    }

    // TODO: consider avoiding code generation for unavailable.

    return renderDeprecationAttribute(attribute, message = "")
}

private fun renderDeprecationAttribute(attribute: String, message: String) = "$attribute(${quoteAsCStringLiteral(message)})"

private fun quoteAsCStringLiteral(str: String): String = buildString {
    append('"')
    for (c in str) {
        when (c) {
            '\n' -> append("\\n")

            '\r' -> append("\\r")

            '"', '\\' -> append('\\').append(c)

            // TODO: handle more special cases.
            else -> append(c)
        }
    }
    append('"')
}