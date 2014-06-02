public open class Identifier<T> private(private val myName: T?, private var myHasDollar: Boolean) {
    private var myNullable: Boolean = true

    public open fun getName(): T? {
        return myName
    }

    class object {

        public open fun <T> init(name: T?): Identifier<T> {
            val __ = Identifier(name, false)
            return __
        }

        public open fun <T> init(name: T?, isNullable: Boolean): Identifier<T> {
            val __ = Identifier(name, false)
            __.myNullable = isNullable
            return __
        }

        public open fun <T> init(name: T?, hasDollar: Boolean, isNullable: Boolean): Identifier<T> {
            val __ = Identifier(name, hasDollar)
            __.myNullable = isNullable
            return __
        }
    }
}

public open class User() {
    class object {
        public open fun main(args: Array<String?>?) {
            var i1: Identifier<*>? = Identifier.init<String?>("name", false, true)
            var i2: Identifier<*>? = Identifier.init<String?>("name", false)
            var i3: Identifier<*>? = Identifier.init<String?>("name")
        }
    }
}
fun main(args: Array<String>) = User.main(args as Array<String?>?)