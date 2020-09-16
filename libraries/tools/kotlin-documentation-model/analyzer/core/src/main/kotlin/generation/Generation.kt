package org.jetbrains.dokka.generation

import org.jetbrains.dokka.Timer

interface Generation {
    fun Timer.generate()
    val generationName: String
}

// This needs to be public for now but in the future it should be replaced with system of checks provided by EP
fun exitGenerationGracefully(reason: String): Nothing {
    throw GracefulGenerationExit(reason)
}

class GracefulGenerationExit(val reason: String) : Throwable()