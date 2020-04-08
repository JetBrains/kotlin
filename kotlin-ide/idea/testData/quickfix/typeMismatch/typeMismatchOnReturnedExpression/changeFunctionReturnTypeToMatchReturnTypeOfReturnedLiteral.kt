// "Change return type of enclosing function 'foo' to '() -> Any'" "true"
fun foo(x: Any): () -> Int {
    return {x<caret>}
}