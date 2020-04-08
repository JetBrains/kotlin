// "Replace with 'NewClass'" "true"

@Deprecated("", ReplaceWith("NewClass"))
class OldClass

class NewClass

fun foo(): List<OldClass<caret>>? {
    return null
}
