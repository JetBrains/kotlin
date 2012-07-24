package kotlin.dom

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.events.*
import java.io.Closeable


fun <T: Event> eventHandler(eventType: Class<T>, handler: (T) -> Unit): EventListener {
    return object : EventListener {
        public override fun handleEvent(e: Event?) {
            if (e != null && eventType.isInstance(e)) {
                handler(e as T)
            }
        }
    }
}

/**
 * Registers a handler on the named event
 */
public fun Node?.on(name: String, capture: Boolean, handler: (Event) -> Unit): Closeable? {
    return on(name, capture, javaClass<Event>(), handler)
}

public fun <T: Event> Node?.on(name: String, capture: Boolean, eventType: Class<T>, handler: (T) -> Unit): Closeable? {
    return if (this is EventTarget) {
        val target: EventTarget = this
        val listener = eventHandler(eventType, handler)
        target.addEventListener(name, listener, capture)
        object: Closeable {
            public override fun close() {
                target.removeEventListener(name, listener, capture)
            }

            public override fun toString(): String? = "CloseableEventListener($target, $name)"
        }
    } else {
        null
    }
}


public fun Node?.onClick(capture: Boolean = false, handler: (MouseEvent) -> Unit): Closeable? {
    return on("click", capture, javaClass<MouseEvent>(), handler)
}

public fun Node?.onDoubleClick(capture: Boolean = false, handler: (MouseEvent) -> Unit): Closeable? {
    return on("dblclick", capture, javaClass<MouseEvent>(), handler)
}