// COMPILER_PLUGIN: org.jetbrains.kotlin.samWithReceiver sam-with-receiver-compiler-plugin.jar annotation=java.lang.FunctionalInterface
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
