// ERROR: Property must be initialized or be abstract
class Identifier<T> {
    val name: T
    private val myHasDollar: Boolean
    private var myNullable = true

    constructor(name: T) {
        this.name = name
    }

    constructor(name: T, isNullable: Boolean) {
        this.name = name
        myNullable = isNullable
    }

    constructor(name: T, hasDollar: Boolean, isNullable: Boolean) {
        this.name = name
        myHasDollar = hasDollar
        myNullable = isNullable
    }
}

object User {
    @JvmStatic
    fun main(args: Array<String>) {
        val i1 = Identifier("name", false, true)
        val i2 = Identifier("name", false)
        val i3 = Identifier("name")
    }
}