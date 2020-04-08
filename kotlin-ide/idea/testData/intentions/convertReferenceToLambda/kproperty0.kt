// IS_APPLICABLE: false

import kotlin.reflect.KProperty0

fun <P : Any> p(p: KProperty0<P>) {}

class B {
    val s: String = ""
    init {
        p(<caret>this::s)
    }
}