// "Add '<*, *>'" "true"
public fun foo(a: Any) {
    when (a) {
        is kotlin.collections.Map<caret> -> {}
        else -> {}
    }
}