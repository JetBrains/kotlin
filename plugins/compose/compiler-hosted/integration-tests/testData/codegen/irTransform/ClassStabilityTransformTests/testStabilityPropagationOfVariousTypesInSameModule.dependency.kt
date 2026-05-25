package a
import androidx.compose.runtime.Stable
import kotlin.reflect.KProperty

@Stable
class StableDelegate {
    operator fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) {
    }
    operator fun getValue(thisObj: Any?, property: KProperty<*>): Int {
        return 10
    }
}

class UnstableDelegate {
    var value: Int = 0
    operator fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) {
        this.value = value
    }
    operator fun getValue(thisObj: Any?, property: KProperty<*>): Int {
        return 10
    }
}
class UnstableClass {
    var value: Int = 0
}
class StableClass

class EmptyClass
class SingleStableValInt(val p1: Int)
class SingleStableVal(val p1: StableClass)
class SingleParamProp<T>(val p1: T)
class SingleParamNonProp<T>(p1: T) { val p2 = p1.hashCode() }
class DoubleParamSingleProp<T, V>(val p1: T, p2: V) { val p3 = p2.hashCode() }
class NonBackingFieldUnstableVal {
    val p1: UnstableClass get() { TODO() }
}
class NonBackingFieldUnstableVar {
    var p1: UnstableClass
        get() { TODO() }
        set(value) { }
}
fun used(x: Any?) {}
