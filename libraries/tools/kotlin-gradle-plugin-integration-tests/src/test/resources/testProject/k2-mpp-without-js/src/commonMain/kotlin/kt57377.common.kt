// ISSUE: KT-57377
package kt57377

internal class StringBox(val value: String)
internal fun createBox(value: String): StringBox = StringBox(value)
