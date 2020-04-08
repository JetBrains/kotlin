import java.io.File

interface I {
    fun abstractFun(): Int
    val abstractVal: Int
    fun nonAbstractFun(): Int = 0
}

fun I.extOnI(): Int = 0
val File.extOnFile: Int get() = 1

open class Base : File("") {
    open fun fromBase1(): Any = 1
    open fun fromBase2(): Int = 1
}

abstract class A : Base(), I {
    override fun abstractFun(): Int {
        return super.<caret>
    }

    override fun fromBase1(): Int = 0
}

// ABSENT: abstractFun
// ABSENT: abstractVal
// EXIST: { itemText: "nonAbstractFun", attributes: "bold" }
// EXIST: { itemText: "hashCode", attributes: "" }
// EXIST: { itemText: "compareTo", attributes: "" }

// ABSENT: fromBase1
// EXIST: { itemText: "fromBase2", attributes: "bold" }

// ABSENT: extOnI
// ABSENT: extOnFile

// NOTHING_ELSE