package ppp

class X<T1> {
    fun <T2> f5(xxxValue1: T1, xxxValue2: T2){}

    inner class Nested {
        fun foo(xxx<caret>)
    }
}

// EXIST: { itemText: "xxxValue1: T1", tailText: null }
// NOTHING_ELSE
