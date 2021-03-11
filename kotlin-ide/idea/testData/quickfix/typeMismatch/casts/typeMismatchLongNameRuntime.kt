// "Cast expression 'module' to 'LinkedHashSet<Int>'" "true"
// DISABLE-ERRORS

fun foo(): java.util.LinkedHashSet<Int> {
    val module: java.util.HashSet<Int> = java.util.LinkedHashSet<Int>()
    return module<caret>
}