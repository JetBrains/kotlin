open class Foo{
    fun Any.f() {
        if (this@Foo is Bar && this is Bar){
            var a: Bar = <caret>
        }
    }
}

class Bar : Foo


// EXIST: { lookupString: "this" }
// EXIST: { lookupString: "this@Foo" }
