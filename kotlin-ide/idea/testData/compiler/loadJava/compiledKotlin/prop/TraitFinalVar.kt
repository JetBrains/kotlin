// KT-2228

package test

interface A {
    var v: String
        get() = "test"
        set(value) {
            throw UnsupportedOperationException()
        }
}