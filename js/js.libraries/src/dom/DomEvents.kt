@file:Suppress("DEPRECATION_ERROR")
package kotlin.dom

import org.w3c.dom.*
import org.w3c.dom.events.*

/**
 * Turns an event handler function into an [EventListener]
 */
@Deprecated("Use EventListener SAM-like constructor instead", ReplaceWith("EventListener(handler)", "org.w3c.dom.events.EventListener"), level = DeprecationLevel.ERROR)
public fun eventHandler(handler: (Event) -> Unit): EventListener = EventListener(handler)

@Deprecated("Use EventListener SAM-like constructor instead", ReplaceWith("EventListener(handler)", "org.w3c.dom.events.EventListener"), level = DeprecationLevel.ERROR)
public fun mouseEventHandler(handler: (MouseEvent) -> Unit): EventListener {
    return eventHandler { e ->
        if (e is MouseEvent) {
            handler(e)
        }
    }
}

@Deprecated("This API is going to be removed", level = DeprecationLevel.ERROR)
public interface Closeable {
    public open fun close(): Unit
}

/**
 * Registers a handler on the named event
 */
@Deprecated("This API is going to be removed", level = DeprecationLevel.ERROR)
public fun Node.on(name: String, capture: Boolean, handler: (Event) -> Unit): Closeable? {
    return on(name, capture, eventHandler(handler))
}

/**
 * Registers an [EventListener] on the named event
 */
@Deprecated("This API is going to be removed", level = DeprecationLevel.ERROR)
public fun Node?.on(name: String, capture: Boolean, listener: EventListener): Closeable? {
    // TODO: instanceof EventTarget is a very bad idea!
    // TODO: nullable target is a very bad idea!
    // TODO: receiver should be EventTarget
    val target = this as? EventTarget
    return if (target != null) {
        target.addEventListener(name, listener, capture)
        CloseableEventListener(target, listener, name, capture)
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

@Deprecated("This API is going to be removed", level = DeprecationLevel.ERROR)
public fun Node?.onClick(capture: Boolean = false, handler: (MouseEvent) -> Unit): Closeable? {
    return on("click", capture, mouseEventHandler(handler))
}

@Deprecated("This API is going to be removed", level = DeprecationLevel.ERROR)
public fun Node?.onDoubleClick(capture: Boolean = false, handler: (MouseEvent) -> Unit): Closeable? {
    return on("dblclick", capture, mouseEventHandler(handler))
}