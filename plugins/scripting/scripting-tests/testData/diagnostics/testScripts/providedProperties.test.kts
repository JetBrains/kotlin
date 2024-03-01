// SCRIPT_PROVIDED_PROPERTIES: prop1: kotlin.String, prop2: java.io.File
// MUTE_LL_FIR: KT-66276

val rv = args[1] + prop1 + prop2.path
