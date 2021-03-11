// "Replace with 'File'" "true"

@Deprecated("", ReplaceWith("File", "java.io.File"))
class OldClass

fun foo(): OldClass<caret>? {
    return null
}
