package sample

enum class Foo {
    X, Y;

    fun foo(): Foo = this
}

fun foo(){
    val f : Foo = <caret>
}

// EXIST: { lookupString:"X", itemText:"Foo.X", tailText:" (sample)", typeText:"Foo" }
// EXIST: { lookupString:"Y", itemText:"Foo.Y", tailText:" (sample)", typeText:"Foo" }
// ABSENT: { itemText:"Foo.foo" }
