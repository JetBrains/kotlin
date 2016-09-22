// ERROR: Property must be initialized or be abstract
class Foo {
    private external fun nativeMethod()

    var bar: Int
        external get
        external set
}