// EXIT_CODE: !0
// OUTPUT_REGEX: Uncaught Kotlin exception: kotlin\.Error: an error\R(?!.*FAIL.*).*

val p: Nothing = throw Error("an error")

fun main() {
    println("FAIL")
}