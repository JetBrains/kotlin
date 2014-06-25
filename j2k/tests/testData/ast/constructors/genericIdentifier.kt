public class Identifier<T> private(private val myName: T, private val myHasDollar: Boolean) {
    private var myNullable = true

    public fun getName(): T {
        return myName
    }

    class object {

        public fun <T> create(name: T): Identifier<T> {
            return Identifier(name, false)
        }

        public fun <T> create(name: T, isNullable: Boolean): Identifier<T> {
            val __ = Identifier(name, false)
            __.myNullable = isNullable
            return __
        }

        public fun <T> create(name: T, hasDollar: Boolean, isNullable: Boolean): Identifier<T> {
            val __ = Identifier(name, hasDollar)
            __.myNullable = isNullable
            return __
        }
    }
}

public class User {
    class object {
        public fun main() {
            val i1 = Identifier.create<String>("name", false, true)
            val i2 = Identifier.create<String>("name", false)
            val i3 = Identifier.create<String>("name")
        }
    }
}