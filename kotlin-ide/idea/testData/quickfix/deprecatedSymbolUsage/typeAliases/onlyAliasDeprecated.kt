// "Replace with 'Some'" "true"

class Some

@Deprecated("Use Some instead", replaceWith = ReplaceWith("Some"))
typealias A = Some

val a: <caret>A = A()