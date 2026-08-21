/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
package org.w3c.dom.events

import kotlinx.browser.PLEASE_USE_KOTLINX_BROWSER_INSTEAD

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public fun EventListener(handler: (Event) -> Unit): EventListener = EventListenerHandler(handler)

private class EventListenerHandler(private val handler: (Event) -> Unit) : EventListener {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    public override fun handleEvent(event: Event) {
        handler(event)
    }

    public override fun toString(): String = "EventListenerHandler($handler)"
}
