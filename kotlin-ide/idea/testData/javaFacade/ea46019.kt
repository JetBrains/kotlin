import kotlin.reflect.KProperty

class SomeProp() {
    fun getValue(t: Any, metadata: KProperty<*>) = 42
}

class Some<caret>() {
    val renderer  by SomeProp()
}
