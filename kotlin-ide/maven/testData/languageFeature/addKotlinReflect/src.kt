import kotlin.reflect.full.*

class Foo(val prop: Any) {
    fun func() {}
}

fun y01() = Foo::prop.gett<caret>er
