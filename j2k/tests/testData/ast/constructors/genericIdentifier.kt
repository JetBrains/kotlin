public fun <T> Identifier(name: T): Identifier<T> {
    return Identifier(name, false)
}

public fun <T> Identifier(name: T, isNullable: Boolean): Identifier<T> {
    val __ = Identifier(name, false)
    __.myNullable = isNullable
    return __
}

public fun <T> Identifier(name: T, hasDollar: Boolean, isNullable: Boolean): Identifier<T> {
    val __ = Identifier(name, hasDollar)
    __.myNullable = isNullable
    return __
}

public class Identifier<T>(private val myName: T, private val myHasDollar: Boolean) {
    private var myNullable = true

    public fun getName(): T {
        return myName
    }
}

public class User {
    class object {
        public fun main() {
            val i1 = Identifier("name", false, true)
            val i2 = Identifier<String>("name", false)
            val i3 = Identifier("name")
        }
    }
}