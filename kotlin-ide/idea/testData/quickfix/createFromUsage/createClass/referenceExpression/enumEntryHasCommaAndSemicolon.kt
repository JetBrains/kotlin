// "Create enum constant 'C'" "true"
enum class Bar {
    A,
    B,
    ;
}

fun main() {
    Bar.C<caret>
}