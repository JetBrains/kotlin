// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

// FILE: variables.kt
package `0`

val ascii = "ascii"
val `_` = "underscore"
val `__` = "double underscore"
// val `~` = "tilda" // name contains illegal characters: "~" // why?
val `with space` = "contains space"
val `∞` = "infinity unicode symbol"
val `0times` = "starts with digit"
val `for` = "keyword"
val `🤷` = "unicode emoji"

// FILE: functions.kt
package `1`

fun ascii() = "ascii"
fun `_`() = "underscore"
fun `__`() = "double underscore"
fun `with space`() = "contains space"
fun `∞`() = "infinity unicode symbol"
fun `0times`() = "starts with digit"
fun `for`(int: Int, `for`: Int, `for long`: Int) = "keyword"
fun `🤷`() = "unicode emoji"

// FILE: classes.kt
package `2`

// TODO: KT-64970 Proper mangling algorithm for Swift Export
// Wrapper class mangling generated for kotlin runtime doesn't non-trivial identifiers yet support yet.
/*
class ascii() // "ascii"
class `_`() // "underscore"
class `__`() // "double underscore"
class `with space`() // "contains space"
class `∞`() // "infinity unicode symbol"
class `0times`() // "starts with digit"
class `for`() // "keyword"
class `🤷`() // "unicode emoji"
*/

// FILE: typealiases.kt
package `4`

typealias ascii = Unit // "ascii"
typealias `_` = Unit // "underscore"
typealias `__` = Unit // "double underscore"
typealias `with space` = Unit // "contains space"
typealias `∞` = Unit // "infinity unicode symbol"
typealias `0times` = Unit // "starts with digit"
typealias `for` = Unit // "keyword"
typealias `🤷` = Unit // "unicode emoji"
