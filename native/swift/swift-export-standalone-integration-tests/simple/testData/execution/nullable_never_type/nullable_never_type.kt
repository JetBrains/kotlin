// KIND: STANDALONE
// MODULE: NullableNeverType
// FILE: input.kt
fun meaningOfLife(input: Int): Nothing? = if (input == 42) null else TODO("input was not 42")