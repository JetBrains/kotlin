package pack

class FooFaa

class Fuu

fun f(myFooF<caret>)

// EXIST: { lookupString: "myFooFaa : FooFaa", itemText: "myFooFaa: FooFaa", tailText: " (pack)" }
// EXIST: { lookupString: "myFooFuu : Fuu", itemText: "myFooFuu: Fuu", tailText: " (pack)" }
// ABSENT: { itemText: "myFooFooFaa: FooFaa" }
