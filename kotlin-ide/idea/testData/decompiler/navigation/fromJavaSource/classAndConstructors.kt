package testData.libraries

class SomeClassWithConstructors(private val arg: String)  {
    constructor(x: Int) : this("Hello")
    fun check() {
        TODO()
    }
}