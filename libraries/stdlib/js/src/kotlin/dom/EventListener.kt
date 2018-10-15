/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.w3c.dom.events

public fun EventListener(handler: (Event) -> Unit): EventListener = EventListenerHandler(handler)

private class EventListenerHandler(private val handler: (Event) -> Unit) : EventListener {
    public override fun handleEvent(event: Event) {
        handler(event)
    }

    public override fun toString(): String = "EventListenerHandler($handler)"
}
