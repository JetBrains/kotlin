import kotlin.reflect.KProperty

fun foo() {
    a<caret>
}

val a by Delegate()

class Delegate {
    fun getValue(t: Any?, p: KProperty<*>) = 1
}

// EXISTS: a.getValue(Any?\, KProperty<*>)
