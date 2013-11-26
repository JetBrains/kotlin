public class Identifier<T>(_myName: T, _myHasDollar: Boolean) {
    private val myName: T
    private var myHasDollar: Boolean = false
    private var myNullable: Boolean = true
    public fun getName(): T {
        return myName
    }
    {
        myName = _myName
        myHasDollar = _myHasDollar
    }
    class object {
        public fun <T> init(name: T): Identifier<T> {
            val __ = Identifier(name, false)
            return __
        }
        public fun <T> init(name: T, isNullable: Boolean): Identifier<T> {
            val __ = Identifier(name, false)
            __.myNullable = isNullable
            return __
        }
        public fun <T> init(name: T, hasDollar: Boolean, isNullable: Boolean): Identifier<T> {
            val __ = Identifier(name, hasDollar)
            __.myNullable = isNullable
            return __
        }
    }
}
public class User() {
    class object {
        public fun main() {
            val i1 = Identifier.init<String>("name", false, true)
            val i2 = Identifier.init<String>("name", false)
            val i3 = Identifier.init<String>("name")
        }
    }
}