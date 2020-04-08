// "Change return type of called function 'A.compareTo' to 'Int'" "true"
interface A {
    operator fun compareTo(other: Any): String
}
fun foo(x: A) {
    if (x <<caret> 0) {}
}