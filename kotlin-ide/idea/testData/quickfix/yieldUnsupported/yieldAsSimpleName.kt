// "Migrate unsupported yield syntax" "true"
object yield {}

fun test() {
    val foo = yie<caret>ld
}
