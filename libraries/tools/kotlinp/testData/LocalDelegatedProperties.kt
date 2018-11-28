import kotlin.reflect.KProperty

class Delegate<T>(val value: T? = null) {
    operator fun getValue(instance: Any?, property: KProperty<*>): T = value!!
}

val nonLocal by Delegate<String>()

val init0 = run {
    val local1 by Delegate<Double>()
    val local2 by Delegate<Any>()
}

val init1 = run {
    val local3 by Delegate<CharSequence?>()
}

class Class {
    init {
        val local4 by Delegate<Array<String>>()
    }

    fun f() {
        val local5 by Delegate<List<Unit>?>()

        fun g() {
            val local6 by Delegate<Int>()
        }
    }
}
