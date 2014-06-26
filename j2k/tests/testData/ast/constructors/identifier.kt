public fun Identifier(name: String): Identifier {
    return Identifier(name, false)
}

public fun Identifier(name: String, isNullable: Boolean): Identifier {
    val __ = Identifier(name, false)
    __.myNullable = isNullable
    return __
}

public fun Identifier(name: String, hasDollar: Boolean, isNullable: Boolean): Identifier {
    val __ = Identifier(name, hasDollar)
    __.myNullable = isNullable
    return __
}

public class Identifier(private val myName: String, private val myHasDollar: Boolean) {
    private var myNullable = true

    public fun getName(): String {
        return myName
    }
}

public class User {
    class object {
        public fun main() {
            val i1 = Identifier("name", false, true)
            val i2 = Identifier("name", false)
            val i3 = Identifier("name")
        }
    }
}