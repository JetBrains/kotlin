public class Identifier(_myName: String, _myHasDollar: Boolean) {
    private val myName: String
    private var myHasDollar: Boolean = false
    private var myNullable: Boolean = true

    public fun getName(): String {
        return myName
    }

    {
        myName = _myName
        myHasDollar = _myHasDollar
    }

    class object {

        public fun init(name: String): Identifier {
            val __ = Identifier(name, false)
            return __
        }

        public fun init(name: String, isNullable: Boolean): Identifier {
            val __ = Identifier(name, false)
            __.myNullable = isNullable
            return __
        }

        public fun init(name: String, hasDollar: Boolean, isNullable: Boolean): Identifier {
            val __ = Identifier(name, hasDollar)
            __.myNullable = isNullable
            return __
        }
    }
}

public class User() {
    class object {
        public fun main() {
            val i1 = Identifier.init("name", false, true)
            val i2 = Identifier.init("name", false)
            val i3 = Identifier.init("name")
        }
    }
}