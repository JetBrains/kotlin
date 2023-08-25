val foo: Int = 42
var bar: Int = 0
var baz: Int
    get() = 42
    set(newValue) { bar = newValue }

var obj: Any = Unit // FIXME: replace with null after swiftexport supports nullability