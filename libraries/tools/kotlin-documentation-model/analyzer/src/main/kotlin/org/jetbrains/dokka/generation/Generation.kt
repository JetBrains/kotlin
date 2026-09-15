/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.generation

import org.jetbrains.dokka.Timer

public interface Generation {
    public fun Timer.generate()
    public val generationName: String
}

// This needs to be public for now but in the future it should be replaced with system of checks provided by EP
public fun exitGenerationGracefully(reason: String): Nothing {
    throw GracefulGenerationExit(reason)
}

public class GracefulGenerationExit(public val reason: String) : Throwable()
