package kotlin.properties

import java.util.HashMap
import java.util.ArrayList

public class ChangeEvent(val source: Any, val name: String, val oldValue: Any?, val newValue: Any?) {
    var propogationId: Any? = null

    public fun toString() : String = "ChangeEvent($name, $oldValue, $newValue)"
}

public trait ChangeListener {
    public fun onPropertyChange(event: ChangeEvent): Unit
}

/**
 * Represents an object where properties can be listened to and notified on
 * updates for easier binding to user interfaces, undo/redo command stacks and
 * change tracking mechanisms for persistence or distributed change notifications.
 */
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
            listeners = arrayList<ChangeListener>()
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

/*
TODO this seems to generate a compiler barf!
see http://youtrack.jetbrains.com/issue/KT-1362

    protected fun createChangeListener(fn: (ChangeEvent) -> Unit): ChangeListener {
        return DelegateChangeListener(fn)
    }

    protected fun createChangeListener(fn: (ChangeEvent) -> Unit): ChangeListener {
        return ChangeListener {
            public override fun onPropertyChange(event: ChangeEvent): Unit {
                fn(event)
            }
        }
    }

}


class DelegateChangeListener(val f: (ChangeEvent) -> Unit) : ChangeListener {
    public override fun onPropertyChange(event: ChangeEvent): Unit {
        f(event)
    }
}


