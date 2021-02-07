//ALLOW_AST_ACCESS
package test

import kotlin.reflect.KProperty

class A {
    var a by MyProperty()
}

class MyProperty<T> {
    operator fun getValue(t: T, p: KProperty<*>): Int = 42
    operator fun setValue(t: T, p: KProperty<*>, i: Int) {}
}
