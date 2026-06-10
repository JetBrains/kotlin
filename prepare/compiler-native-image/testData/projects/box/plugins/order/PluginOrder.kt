// COMPILER_PLUGIN: kotlin-allopen-compiler-plugin-2.4.20.jar annotation=AllOpen
// COMPILER_PLUGIN: kotlin-noarg-compiler-plugin-2.4.20.jar annotation=NoArg
// COMPILER_PLUGIN_ORDER: org.jetbrains.kotlin.noarg>org.jetbrains.kotlin.allopen
// EXPECTED_PLUGIN_ORDER: org.jetbrains.kotlin.noarg org.jetbrains.kotlin.allopen

annotation class AllOpen
annotation class NoArg

fun box(): String = "OK"
