// ERROR: Property must be initialized or be abstract
class Identifier {
    val name: String?
    private var myHasDollar = false
    private var myNullable = true

    constructor(name: String?) {
        this.name = name
    }

    constructor(name: String?, isNullable: Boolean) {
        this.name = name
        myNullable = isNullable
    }

    constructor(name: String?, hasDollar: Boolean, isNullable: Boolean) {
        this.name = name
        myHasDollar = hasDollar
        myNullable = isNullable
    }
}

object User {
    fun main() {
        val i1 = Identifier("name", false, true)
        val i2 = Identifier("name", false)
        val i3 = Identifier("name")
    }
}