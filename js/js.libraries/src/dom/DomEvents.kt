package kotlin.dom

import org.w3c.dom.*
import org.w3c.dom.events.*

/**
 * Turns an event handler function into an [EventListener]
 */
public fun eventHandler(handler: (Event) -> Unit): EventListener {
    return EventListenerHandler(handler)
}

private class EventListenerHandler(private val handler: (Event) -> Unit) : EventListener {
    public override fun handleEvent(e: Event) {
        handler(e)
    }
    public override fun toString(): String = "EventListenerHandler($handler)"
}

public fun mouseEventHandler(handler: (MouseEvent) -> Unit): EventListener {
    return eventHandler { e ->
        if (e is MouseEvent) {
            handler(e)
        }
    }
}

public interface Closeable {
    public open fun close(): Unit
}

/**
 * Registers a handler on the named event
 */
public fun Node.on(name: String, capture: Boolean, handler: (Event) -> Unit): Closeable? {
    return on(name, capture, eventHandler(handler))
}

/**
 * Registers an [EventListener] on the named event
 */
public fun Node?.on(name: String, capture: Boolean, listener: EventListener): Closeable? {
    return if (this is EventTarget) {
        addEventListener(name, listener, capture)
        CloseableEventListener(this, listener, name, capture)
    } else {
        null
    }
}

private class CloseableEventListener(
        private val target: EventTarget,
        private val listener: EventListener,
        private val name: String,
        private val capture: Boolean
) : Closeable {
    public override fun close() {
        target.removeEventListener(name, listener, capture)
    }

    public override fun toString(): String = "CloseableEventListener($target, $name)"
}

public fun Node?.onClick(capture: Boolean = false, handler: (MouseEvent) -> Unit): Closeable? {
    return on("click", capture, mouseEventHandler(handler))
}

public fun Node?.onDoubleClick(capture: Boolean = false, handler: (MouseEvent) -> Unit): Closeable? {
    return on("dblclick", capture, mouseEventHandler(handler))
}