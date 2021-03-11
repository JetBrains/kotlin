package sample

abstract class Foo {
    object X: Foo()
    class Y: Foo()
}

fun foo(){
    val f : Foo = <caret>
}

// EXIST: { lookupString:"X", itemText:"X", tailText:" (sample.Foo)" }
// EXIST: { lookupString:"Y", itemText:"Y", tailText:"() (sample.Foo)" }
