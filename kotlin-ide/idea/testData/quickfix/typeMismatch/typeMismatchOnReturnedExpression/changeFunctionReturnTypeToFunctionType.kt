// "Change return type of enclosing function 'foo' to '(Long) -> Int'" "true"
fun foo(x: Any): Int {
    return {x: Long -> 42}<caret>
}