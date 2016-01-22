package foo

fun box(): String {
    val kotlin: String = "kotlin"

    if (kotlin.subSequence(0, kotlin.length) != kotlin) return "Fail 0"

    val kot: CharSequence = kotlin.subSequence(0, 3)
    if (kot.toString() != "kot") return "Fail 1: $kot"

    val tlin = (kotlin as CharSequence).subSequence(2, 6)
    if (tlin.toString() != "tlin") return "Fail 2: $tlin"

    return "OK"
}
