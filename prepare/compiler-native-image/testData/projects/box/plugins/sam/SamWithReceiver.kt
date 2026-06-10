// COMPILER_PLUGIN: kotlin-sam-with-receiver-compiler-plugin-2.4.20.jar annotation=java.lang.FunctionalInterface
// FULL_JDK

import java.util.function.Consumer

fun box(): String {
    val sb = StringBuilder()
    val consumer = Consumer<StringBuilder> {
        append("OK")
    }
    consumer.accept(sb)
    return sb.toString()
}
