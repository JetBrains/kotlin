// "Migrate unsupported yield syntax" "true"
object yield {
    operator fun invoke(f: () -> Unit) = f()
}

fun test() {
    yie<caret>ld {  }
}
