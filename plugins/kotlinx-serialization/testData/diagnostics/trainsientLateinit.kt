// ISSUE: KT-73961

class Bar {
    @kotlinx.serialization.Transient
    <!UNNECESSARY_LATEINIT!>lateinit<!> var foo: String

    constructor(foo: String) {
        this.foo = foo
    }
}
