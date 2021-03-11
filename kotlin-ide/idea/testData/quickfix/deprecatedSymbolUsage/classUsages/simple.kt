// "Replace with 'NewClass'" "true"

@Deprecated("", ReplaceWith("NewClass"))
class OldClass

class NewClass

fun foo(): OldClass<caret>? {
    return null
}
