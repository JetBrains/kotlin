package kotlin.properties

import kotlin.*
import kotlin.util.*
import java.util.List
import java.util.Map
import java.util.HashMap
import java.util.ArrayList

class ChangeEvent(val source: Any, val name: String, val oldValue: Any?, val newValue: Any?) {
    var propogationId: Any? = null

    fun toString() = "ChangeEvent($name, $oldValue, $newValue)"
}

trait ChangeListener {
    fun onPropertyChange(event: ChangeEvent): Unit
}

/**
 * Represents an object where properties can be listened to and notified on
 * updates for easier binding to user interfaces, undo/redo command stacks and
 * change tracking mechanisms for persistence or distributed change notifications.
 */
abstract class ChangeSupport {
    private var allListeners: List<ChangeListener>? = null
    private var nameListeners: Map<String, List<ChangeListener>>? = null


    fun addChangeListener(listener: ChangeListener) {
        if (allListeners == null) {
            allListeners = ArrayList<ChangeListener>()
        }
        allListeners?.add(listener)
    }

    fun addChangeListener(name: String, listener: ChangeListener) {
        if (nameListeners == null) {
            nameListeners = HashMap<String, List<ChangeListener>>()
        }
        var listeners = nameListeners?.get(name)
        if (listeners == null) {
            listeners = arrayList<ChangeListener>()
            nameListeners?.put(name, listeners.sure())
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
            for (listener in allListeners) {
                listener.onPropertyChange(event)
            }
        }
    }

    fun onPropertyChange(fn: (ChangeEvent) -> Unit) {
        // TODO
        //addChangeListener(DelegateChangeListener(fn))
    }

    fun onPropertyChange(name: String, fn: (ChangeEvent) -> Unit) {
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
            override fun onPropertyChange(event: ChangeEvent): Unit {
                fn(event)
            }
        }
    }

}


class DelegateChangeListener(val f: (ChangeEvent) -> Unit) : ChangeListener {
    override fun onPropertyChange(event: ChangeEvent): Unit {
        f(event)
    }
}


