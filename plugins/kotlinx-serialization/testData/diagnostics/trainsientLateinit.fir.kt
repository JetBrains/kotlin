// ISSUE: KT-73961

class Bar {
    @kotlinx.serialization.Transient
    lateinit var foo: String

    constructor(foo: String) {
        this.foo = foo
    }
}
