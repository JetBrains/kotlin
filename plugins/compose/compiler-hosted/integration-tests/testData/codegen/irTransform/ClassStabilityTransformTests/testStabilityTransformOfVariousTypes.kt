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
class Unstable { var value: Int = 0 }
class EmptyClass
class SingleStableVal(val p1: Int)
class SingleParamProp<T>(val p1: T)
class SingleParamNonProp<T>(p1: T) { val p2 = p1.hashCode() }
class DoubleParamSingleProp<T, V>(val p1: T, p2: V) { val p3 = p2.hashCode() }
class X<T>(val p1: List<T>)
class NonBackingFieldUnstableProp {
    val p1: Unstable get() { TODO() }
}
class NonBackingFieldUnstableVarProp {
    var p1: Unstable
        get() { TODO() }
        set(value) { }
}
class StableDelegateProp {
    var p1 by StableDelegate()
}
class UnstableDelegateProp {
    var p1 by UnstableDelegate()
}
