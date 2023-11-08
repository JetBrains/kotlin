// EXIT_CODE: !0
// OUTPUT_REGEX: Uncaught Kotlin exception: kotlin\.Error: an error\n.*

fun main() {
    throw Error("an error")
}