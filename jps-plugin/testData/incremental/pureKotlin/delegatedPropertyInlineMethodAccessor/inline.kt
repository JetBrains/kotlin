package inline

import kotlin.reflect.KProperty

class Inline {
    inline fun getValue(receiver: Any?, prop: KProperty<*>): Int {
        return 0
    }

    inline fun setValue(receiver: Any?, prop: KProperty<*>, value: Int) {
        println(value)
    }
}
