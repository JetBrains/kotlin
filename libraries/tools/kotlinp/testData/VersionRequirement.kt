// IGNORE K2

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package test

import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind

@RequireKotlin("1.2", "Klass must not be used!", DeprecationLevel.WARNING, RequireKotlinVersionKind.API_VERSION)
class Klass

class Konstructor @RequireKotlin("42.0", "Konstructor must not be used!", DeprecationLevel.WARNING, RequireKotlinVersionKind.LANGUAGE_VERSION, 42) constructor()

@RequireKotlin("1.1", level = DeprecationLevel.HIDDEN, versionKind = RequireKotlinVersionKind.API_VERSION, errorCode = 314)
typealias Typealias = String

@RequireKotlin("1.2.40", level = DeprecationLevel.ERROR, versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
fun function() {}

@RequireKotlin("1.3", "property must not be used!")
val property = ""
