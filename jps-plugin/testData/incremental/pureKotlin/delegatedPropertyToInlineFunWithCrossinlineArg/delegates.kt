package delegates

import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

inline fun crashMe(crossinline callback: () -> Unit): ReadWriteProperty<Any, Unit> {
    return Delegates.observable(Unit) { desc, old, new -> callback() }
}