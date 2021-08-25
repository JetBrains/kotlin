package testProject

class JvmPrinter {
    fun print(msg: String) {
        System.out.println(msg)
    }
}

actual typealias Printer = JvmPrinter

fun main(args: Array<String>) {
    businessLogic(args, JvmPrinter())
}