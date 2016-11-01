package kotlin_native.io

@kotlin_native.SymbolName("Kotlin_io_Console_print")
external public fun print(message: kotlin_native.String)

@kotlin_native.SymbolName("Kotlin_io_Console_readLine")
external public fun readLine(): kotlin_native.String
