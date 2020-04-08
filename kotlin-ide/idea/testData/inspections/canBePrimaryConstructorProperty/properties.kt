class Correct(simple: String, withType: Int, otherName: Double) {

    val simple = simple

    val withType: Int = withType

    // Questionable case (due to possible named parameters), not allowed now
    val anotherName = otherName
}

// Inspection should not work here anywhere
class Incorrect(val property: String, withGetter: Double, withSetter: Int, differentType: String) {

    val another = property

    val fromProperty = another

    val withGetter = withGetter
        get() = 2 * field

    val withSetter = withSetter
        private set

    val differentType: String? = differentType

    constructor(param: Int): this("", 0.0, param, "") {
        val local = param
    }
}

// For data class inspection also should not work
data class Data(name: String) {
    val name = name
}

// Case from KT-12876: also should not work, property lives in different class here
interface Foo {
    val x : String
}

class Bar(x : String) {
    init {
        object : Foo {
            override val x = x // Property is assigned to parameter, can be declared in ctor
        }
    }
}

// KT-32561
open class X(property: String) {
    init {
        print(property)
    }

    open val property: String = property
}

abstract class X2(property: String) {
    init {
        print(property)
    }

    open val property: String = property
}

sealed class X3(property: String) {
    init {
        print(property)
    }

    open val property: String = property
}

open class X4(property: String) {
    init {
        print(property)
    }

    val property: String = property
}

class X5(property: String) {
    init {
        print(property)
    }

    open val property: String = property
}

open class X6(property: String) {
    open val property: String = property
}
