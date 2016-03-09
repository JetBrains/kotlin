package test

class A {
    fun argumentAdded(x: Int = 1) {}
    fun argumentRemoved() {}

    fun valueAdded(x: Int = 3) {}
    fun valueRemoved(x: Int) {}
    fun valueChanged(x: Int = 6) {}
}

class ConstructorValueAdded(x: Int = 7)
class ConstructorValueRemoved(x: Int)
class ConstructorValueChanged(x: Int = 20)

class ConstructorArgumentAdded(x: Int = 9)
class ConstructorArgumentRemoved()