plugins {
    kotlin("multiplatform")
}

kotlin {
    // Using deprecated target to provoke ERROR-diagnostic. If you came here because you removed
    // that target entirely, just replace it with any other construction from KGP that provokes
    // ERROR-diagnostic
    @Suppress("DEPRECATION_ERROR")
    linuxMips32()
}
