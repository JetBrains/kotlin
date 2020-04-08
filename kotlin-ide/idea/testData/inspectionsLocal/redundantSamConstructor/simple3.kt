// COMPILER_ARGUMENTS: -XXLanguage:+NewInference -XXLanguage:+SamConversionForKotlinFunctions

fun usage(r: Runnable) {}

fun test() {
    usage(Runnable<caret> { })
}
