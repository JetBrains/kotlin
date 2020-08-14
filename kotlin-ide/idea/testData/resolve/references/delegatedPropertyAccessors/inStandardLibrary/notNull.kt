import kotlin.properties.Delegates

var x: Int <caret>by Delegates.notNull()

// REF: (in kotlin.properties.ReadWriteProperty).setValue(T, KProperty<*>, V)
