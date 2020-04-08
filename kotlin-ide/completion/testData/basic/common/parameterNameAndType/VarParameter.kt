package pack

class FooBar

class Boo

class C(private var b<caret>)

// EXIST: { itemText: "bar: FooBar", tailText: " (pack)" }
// ABSENT: { itemText: "fooBar: FooBar" }
// EXIST: { itemText: "boo: Boo", tailText: " (pack)" }
