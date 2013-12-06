package kotlin.beans

import java.beans.*

// TODO this file may be deleted when this issue is implemented
// http://youtrack.jetbrains.com/issue/KT-1752

/**
 * Creates a [[PropertyChangeListener]] for the given function for processing each [[PropertyChangeEvent]]
 */
fun propertyChangeListener(fn: (PropertyChangeEvent) -> Unit): PropertyChangeListener = FunctionPropertyChangeListener(fn)

private class FunctionPropertyChangeListener(val fn: (PropertyChangeEvent) -> Unit) : PropertyChangeListener {
    public override fun propertyChange(e: PropertyChangeEvent) {
        if (e != null) {
            (fn)(e)
        }
    }
}
