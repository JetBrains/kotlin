// "Replace with 'New'" "true"
package ppp

object New

@Deprecated(message = "Deprecated, use New", replaceWith = ReplaceWith("New"))
typealias Old = New

fun main(args: Array<String>) {
    val o = <caret>Old
}