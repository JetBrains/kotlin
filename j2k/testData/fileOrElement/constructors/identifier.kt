// ERROR: Property must be initialized or be abstract
public class Identifier {
    public val name: String
    private val myHasDollar: Boolean
    private var myNullable = true

    public constructor(name: String) {
        this.name = name
    }

    public constructor(name: String, isNullable: Boolean) {
        this.name = name
        myNullable = isNullable
    }

    public constructor(name: String, hasDollar: Boolean, isNullable: Boolean) {
        this.name = name
        myHasDollar = hasDollar
        myNullable = isNullable
    }
}

public class User {
    default object {
        public fun main() {
            val i1 = Identifier("name", false, true)
            val i2 = Identifier("name", false)
            val i3 = Identifier("name")
        }
    }
}