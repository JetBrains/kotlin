private val runnableSupplier: () -> () -> Unit
  get() =
    <caret>{ { } }

fun main(args: Array<String>) {
}
