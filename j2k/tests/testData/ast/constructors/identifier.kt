public class Identifier private(private val myName: String, private val myHasDollar: Boolean) {
    private var myNullable = true

    public fun getName(): String {
        return myName
    }

    class object {

        public fun create(name: String): Identifier {
            return Identifier(name, false)
        }

        public fun create(name: String, isNullable: Boolean): Identifier {
            val __ = Identifier(name, false)
            __.myNullable = isNullable
            return __
        }

        public fun create(name: String, hasDollar: Boolean, isNullable: Boolean): Identifier {
            val __ = Identifier(name, hasDollar)
            __.myNullable = isNullable
            return __
        }
    }
}

public class User {
    class object {
        public fun main() {
            val i1 = Identifier.create("name", false, true)
            val i2 = Identifier.create("name", false)
            val i3 = Identifier.create("name")
        }
    }
}