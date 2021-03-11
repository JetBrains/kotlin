package foo

import kotlin.reflect.KProperty

operator fun String.getValue(nothing: Nothing?, property: KProperty<*>) = ""