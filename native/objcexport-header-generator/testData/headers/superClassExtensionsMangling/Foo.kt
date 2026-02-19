open class Foo
class Bar : Foo()

fun Foo.extFun() = Unit
fun Bar.extFun() = Unit
fun Foo.extFun(p0: String, p1: Boolean) = Unit
fun Bar.extFun(p0: Boolean, p1: String) = Unit
val Foo.propVal: Int
    get() = 42
val Bar.propVal: Int
    get() = 42
var Foo.propVar: Int
    get() = 42
    set(value) {}
var Bar.propVar: Int
    get() = 42
    set(value) {}