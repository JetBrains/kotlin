// "Create member function 'F.getValue'" "true"
import kotlin.reflect.KProperty

class F {
    operator fun setValue(x: X, property: KProperty<*>, i: Int) { }
}

class X {
    var f: Int by F()<caret>
}
