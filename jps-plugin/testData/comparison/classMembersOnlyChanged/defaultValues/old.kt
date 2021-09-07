package test

class A {
    fun argumentAdded() {}
    fun argumentRemoved(x: Int = 2) {}

    fun valueAdded(x: Int) {}
    fun valueRemoved(x: Int = 4) {}
    fun valueChanged(x: Int = 5) {}
}

class ConstructorValueAdded(x: Int)
class ConstructorValueRemoved(x: Int = 8)
class ConstructorValueChanged(x: Int = 19)

class ConstructorArgumentAdded()
class ConstructorArgumentRemoved(x: Int = 10)