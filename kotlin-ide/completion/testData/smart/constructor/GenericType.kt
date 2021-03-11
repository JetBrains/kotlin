class Foo<T>(t: T)

fun foo(p : Any){
    var a : Foo<String> = <caret>
}

// EXIST: { lookupString:"Foo", itemText:"Foo", tailText:"(t: String) (<root>)" }
