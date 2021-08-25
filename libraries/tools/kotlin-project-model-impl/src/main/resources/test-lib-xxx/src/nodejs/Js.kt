package testProject

class JsPrinter {
    fun print(msg: String) {
        js("console.log('Native stuff: ' + msg)")
    }
}

actual typealias Printer = JsPrinter

external val process: dynamic

fun main() {
    val argv = process.argv.slice(2) as Array<String>;
    businessLogic(argv, JsPrinter())
}