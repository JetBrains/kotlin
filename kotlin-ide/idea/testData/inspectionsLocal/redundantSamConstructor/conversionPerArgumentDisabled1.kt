// COMPILER_ARGUMENTS: -XXLanguage:+NewInference -XXLanguage:+SamConversionForKotlinFunctions -XXLanguage:+FunctionalInterfaceConversion -XXLanguage:-SamConversionPerArgument
// PROBLEM: none

fun test(r1: Runnable, r2: Runnable) {}

fun usage(r1: Runnable) {
    test(r1, Runnable<caret> {})
}
