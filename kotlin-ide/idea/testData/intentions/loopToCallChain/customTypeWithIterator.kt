// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
class X {
    operator fun iterator(): Iterator<String>{
        return emptyList<String>().iterator()
    }
}

fun foo(x: X): String? {
    <caret>for (s in x) {
        if (s.isNotBlank()) {
            return s
        }
    }
    return null
}
