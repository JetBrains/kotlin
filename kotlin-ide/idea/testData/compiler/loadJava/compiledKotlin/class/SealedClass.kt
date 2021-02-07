package test

class Inheritor3 : SealedClass()

sealed class SealedClass {
    class Inheritor1 : SealedClass()
}

class Inheritor2 : SealedClass()