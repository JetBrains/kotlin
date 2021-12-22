package test

sealed class Base1 {
    class Nested1 : Base1()
    class Nested2 : Base1()
}

sealed class Base2 {
    class Nested1 : Base2()
    class Nested2 : Base2()
}

