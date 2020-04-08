class Foo{
    fun String.foo(){
        fun Foo.bar(){
            val s: String = <caret>
        }
    }
}

// EXIST: { lookupString:"this@foo", typeText:"String" }
