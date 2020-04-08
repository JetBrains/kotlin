// "Replace with 'New<String, Int>'" "true"

@Deprecated("Use New", replaceWith = ReplaceWith("New<T, U>"))
class Old<T, U>

class New<T, U>

fun foo(): <caret>Old<String, Int>? = null