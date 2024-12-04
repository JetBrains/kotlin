package inline

import kotlin.reflect.KProperty

class Inline {
    inline operator fun getValue(receiver: Any?, prop: KProperty<*>): Int {
        return 0
    }

    inline operator fun setValue(receiver: Any?, prop: KProperty<*>, value: Int) {
        println(value)
    }
}
