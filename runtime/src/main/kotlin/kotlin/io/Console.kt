package kotlin.io

@kotlin.SymbolName("Kotlin_io_Console_print")
external public fun print(message: String)

/*
public fun print(message: Int) {
    print(message.toString())
} */

@kotlin.SymbolName("Kotlin_io_Console_println")
external public fun println(message: String)

@kotlin.SymbolName("Kotlin_io_Console_println0")
external public fun println()

@kotlin.SymbolName("Kotlin_io_Console_readLine")
external public fun readLine(): String
