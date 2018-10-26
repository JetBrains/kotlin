/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.example.weather_func

internal typealias EventHandler<T> = (T) -> Unit

/**
 * Covers event handling for the program.
*/
internal class Event<T : Any?> {
    private var handlers = emptyList<EventHandler<T>>()

    fun subscribe(handler: EventHandler<T>) {
        handlers += handler
    }

    fun unsubscribe(handler: EventHandler<T>) {
        handlers -= handler
    }

    operator fun plusAssign(handler: EventHandler<T>) = subscribe(handler)
    operator fun minusAssign(handler: EventHandler<T>) = unsubscribe(handler)

    operator fun invoke(value: T) {
        var exception: Throwable? = null
        for (handler in handlers) {
            try { handler(value) }
            catch (ex: Throwable) { exception = ex }
        }
        // If the exception isn't null then throw it.
        exception?.let { throw it }
    }
}
