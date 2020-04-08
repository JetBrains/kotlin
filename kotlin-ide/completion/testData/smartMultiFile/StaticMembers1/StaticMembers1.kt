package test

import dependency1.O1.foo
import JavaClass1.f

class C

fun foo(): C {
    return <caret>
}

// INVOCATION_COUNT: 1

// EXIST: { allLookupStrings: "bar", itemText: "O1.bar", tailText: "() (dependency1)", attributes: "" }
// EXIST: { itemText: "foo", tailText: "()", attributes: "" }
// ABSENT: { itemText: "O1.foo" }
// ABSENT: { itemText: "O2.foo" }

// EXIST: { allLookupStrings: "g", itemText: "JavaClass1.g", tailText: "() (<root>)", attributes: "" }
// EXIST: { itemText: "f", tailText: "()", attributes: "" }
// ABSENT: { itemText: "JavaClass1.f" }
// ABSENT: { itemText: "JavaClass1.h" }
// ABSENT: { itemText: "JavaClass2.f" }
// ABSENT: { itemText: "JavaClass2.g" }
