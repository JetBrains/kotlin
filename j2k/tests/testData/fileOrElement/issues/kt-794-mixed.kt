package demo

class Test {
    fun getInteger(i: Int?): Int? {
        return i
    }

    fun test() {
        val i = getInteger(10)!!
    }
}