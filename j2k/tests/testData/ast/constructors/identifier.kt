public open class Identifier private(private val myName: String?, private var myHasDollar: Boolean) {
    private var myNullable: Boolean = true

    public open fun getName(): String? {
        return myName
    }

    class object {

        public open fun init(name: String?): Identifier {
            val __ = Identifier(name, false)
            return __
        }

        public open fun init(name: String?, isNullable: Boolean): Identifier {
            val __ = Identifier(name, false)
            __.myNullable = isNullable
            return __
        }

        public open fun init(name: String?, hasDollar: Boolean, isNullable: Boolean): Identifier {
            val __ = Identifier(name, hasDollar)
            __.myNullable = isNullable
            return __
        }
    }
}

public open class User() {
    class object {
        public open fun main() {
            var i1: Identifier? = Identifier.init("name", false, true)
            var i2: Identifier? = Identifier.init("name", false)
            var i3: Identifier? = Identifier.init("name")
        }
    }
}