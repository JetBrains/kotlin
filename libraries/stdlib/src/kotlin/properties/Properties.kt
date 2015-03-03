package kotlin.properties

import java.util.HashMap
import java.util.ArrayList

deprecated("This class is part of an old, incomplete and suboptimal design of change notifications and is going to be removed")
public class ChangeEvent(
        public val source: Any,
        public val name: String,
        public val oldValue: Any?,
        public val newValue: Any?
) {
    override fun toString(): String = "ChangeEvent($name, $oldValue, $newValue)"
}

deprecated("This class is part of an old, incomplete and suboptimal design of change notifications and is going to be removed")
public trait ChangeListener {
    public fun onPropertyChange(event: ChangeEvent): Unit
}

/**
 * Represents an object where properties can be listened to and notified on
 * updates for easier binding to user interfaces, undo/redo command stacks and
 * change tracking mechanisms for persistence or distributed change notifications.
 */
deprecated("This class is part of an old, incomplete and suboptimal design of change notifications and is going to be removed")
public abstract class ChangeSupport {
    private var allListeners: MutableList<ChangeListener>? = null
    private var nameListeners: MutableMap<String, MutableList<ChangeListener>>? = null


    public fun addChangeListener(listener: ChangeListener) {
        if (allListeners == null) {
            allListeners = ArrayList<ChangeListener>()
        }
        allListeners?.add(listener)
    }

    public fun addChangeListener(name: String, listener: ChangeListener) {
        if (nameListeners == null) {
            nameListeners = HashMap<String, MutableList<ChangeListener>>()
        }
        var listeners = nameListeners?.get(name)
        if (listeners == null) {
            listeners = arrayListOf<ChangeListener>()
            nameListeners?.put(name, listeners!!)
        }
        listeners?.add(listener)
    }

    protected fun <T> changeProperty(name: String, oldValue: T?, newValue: T?): Unit {
        if (oldValue != newValue) {
            firePropertyChanged(ChangeEvent(this, name, oldValue, newValue))
        }
    }

    protected fun firePropertyChanged(event: ChangeEvent): Unit {
        if (nameListeners != null) {
            val listeners = nameListeners?.get(event.name)
            if (listeners != null) {
                for (listener in listeners) {
                    listener.onPropertyChange(event)
                }
            }
        }
        if (allListeners != null) {
            for (listener in allListeners!!) {
                listener.onPropertyChange(event)
            }
        }
    }

    protected fun property<T>(init: T): ReadWriteProperty<Any?, T> {
        return Delegates.observable(init) { desc, oldValue, newValue -> changeProperty(desc.name, oldValue, newValue) }
    }

    public fun onPropertyChange(fn: (ChangeEvent) -> Unit) {
        // TODO
        //addChangeListener(DelegateChangeListener(fn))
    }

    public fun onPropertyChange(name: String, fn: (ChangeEvent) -> Unit) {
        // TODO
        //addChangeListener(name, DelegateChangeListener(fn))
    }
}
