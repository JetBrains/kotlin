class Identifier<T> {
    val name: T
    private var myHasDollar = false
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
        val i1: Identifier<*> = Identifier<String?>("name", false, true)
        val i2 = Identifier("name", false)
        val i3 = Identifier("name")
    }
}