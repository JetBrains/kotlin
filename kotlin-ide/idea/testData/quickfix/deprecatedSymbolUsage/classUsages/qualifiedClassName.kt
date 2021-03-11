// "Replace with 'NewClass'" "true"

package ppp

@Deprecated("", ReplaceWith("NewClass"))
class OldClass

class NewClass

fun foo(): ppp.OldClass<caret>? {
    return null
}
