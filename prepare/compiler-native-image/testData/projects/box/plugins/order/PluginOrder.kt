// COMPILER_PLUGIN: allopen-compiler-plugin.jar annotation=AllOpen
// COMPILER_PLUGIN: noarg-compiler-plugin.jar annotation=NoArg
// COMPILER_PLUGIN_ORDER: org.jetbrains.kotlin.noarg>org.jetbrains.kotlin.allopen
// EXPECTED_PLUGIN_ORDER: org.jetbrains.kotlin.noarg org.jetbrains.kotlin.allopen

annotation class AllOpen
annotation class NoArg

fun box(): String = "OK"
