open class Foo{
    fun Bar.f() {
        fun Any.g() {
            if (this is Bar){
                var a: Bar = this@<caret>
            }
        }
    }
}

class Bar : Foo


// EXIST: { lookupString: "this@g" }
// EXIST: { lookupString: "this@f" }
// ABSENT: { lookupString: "this@Foo" }
