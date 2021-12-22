package test

sealed class Base1 {
    sealed class Nested1 : Base1() {
        sealed class Nested2 : Nested1() {
            class Nested3 : Nested2()
        }
    }
}

sealed class Base2 {
    sealed class Nested1 : Base2() {
        class Nested2 : Nested1()
        class Nested3 : Nested1()
    }
}

sealed class Base3 {
    sealed class Nested1 : Base3() {
        class Nested2 : Nested1()
    }
}
