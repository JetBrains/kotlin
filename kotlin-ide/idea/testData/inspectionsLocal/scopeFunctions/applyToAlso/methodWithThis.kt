// WITH_RUNTIME
// FIX: Convert to 'also'

val x = hashSetOf<String>().<caret>apply {
    this.add("x")
}
