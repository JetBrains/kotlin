class Foo {
    fun bar() {
    }
}

val x = Foo::bar

// REF:Foo::bar
// RESULT:KtLightMethodImpl:bar
