// LANGUAGE_VERSION: 1.4
// ERROR: Too many arguments for public fun main(): Unit defined in root package in file simple.kt

fun main(args<caret>: Array<String>) {
}

fun b() {
    main(arrayOf("test"))
}