package test

class A {
    companion object {
        annotation class Anno1

        class B {
            annotation class Anno2
        }
    }
}

@A.Companion.Anno1 @A.Companion.B.Anno2 class C