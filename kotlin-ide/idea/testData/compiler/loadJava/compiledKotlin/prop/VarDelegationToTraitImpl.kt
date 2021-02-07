package test

// test composed from KT-2193

interface A {
    open var v: String
        get() = "test"
        set(value) {
            throw UnsupportedOperationException()
        }
}

class B() : A