// IGNORE K2

interface A {
    interface B {
        interface C
    }

    companion object D {
        enum class E {
            E1,
            E2,
        }

        sealed class F {
            class G : F()
        }
    }
}
