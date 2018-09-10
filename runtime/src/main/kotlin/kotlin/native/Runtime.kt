/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.native

/**
 * Initializes Kotlin runtime for the current thread, if not inited already.
 */
@SymbolName("Kotlin_initRuntimeIfNeeded")
external public fun initRuntimeIfNeeded(): Unit

/**
 * Deinitializes Kotlin runtime for the current thread, if was inited.
 */
@SymbolName("Kotlin_deinitRuntimeIfNeeded")
external public fun deinitRuntimeIfNeeded(): Unit

/**
 * Exception thrown when top level variable is accessed from incorrect execution context.
 */
public class IncorrectDereferenceException : RuntimeException {
    constructor() : super()

    constructor(message: String) : super(message)
}