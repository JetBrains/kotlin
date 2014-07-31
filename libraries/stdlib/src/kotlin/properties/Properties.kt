package kotlin.properties

import java.beans.PropertyChangeSupport
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

val PropertyChangeEvent.source: Any
    get() = getSource()!!

val PropertyChangeEvent.oldValue: Any?
    get() = getOldValue()

val PropertyChangeEvent.newValue: Any?
    get() = getNewValue()

val PropertyChangeEvent.name: String?
    get() = getPropertyName()

fun PropertyChangeSupport.property<T>(initialValue: T): ReadWriteProperty<Any?, T> {
    return Delegates.observable(initialValue) { desc, oldValue, newValue ->
        firePropertyChange(desc.name, oldValue, newValue)
    }
}

trait PropertyChangeFeatures {
    protected val pcs: PropertyChangeSupport

    protected fun property<T>(init: T): ReadWriteProperty<Any?, T> {
        return Delegates.observable(init) { desc, oldValue, newValue ->
            pcs.firePropertyChange(desc.name, oldValue, newValue)
        }
    }

    public fun addPropertyChangeListener(listener: PropertyChangeListener): Unit = pcs.addPropertyChangeListener(listener)
    public fun addPropertyChangeListener(propertyName: String, listener: PropertyChangeListener): Unit = pcs.addPropertyChangeListener(propertyName, listener)
    public fun addPropertyChangeListener(f: (PropertyChangeEvent) -> Unit): Unit = pcs.addPropertyChangeListener(f)
    public fun addPropertyChangeListener(propertyName: String, f: (PropertyChangeEvent) -> Unit): Unit = pcs.addPropertyChangeListener(propertyName, f)

    public fun removePropertyChangeListener(listener: PropertyChangeListener): Unit = pcs.removePropertyChangeListener(listener)
    public fun removePropertyChangeListener(propertyName: String, listener: PropertyChangeListener): Unit = pcs.removePropertyChangeListener(propertyName, listener)
    public fun removePropertyChangeListener(f: (PropertyChangeEvent) -> Unit): Unit = pcs.removePropertyChangeListener(f)
    public fun removePropertyChangeListener(propertyName: String, f: (PropertyChangeEvent) -> Unit): Unit = pcs.removePropertyChangeListener(propertyName, f)
}
