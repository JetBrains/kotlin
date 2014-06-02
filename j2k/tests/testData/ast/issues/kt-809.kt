package demo

open class Container() {
    var myInt: Int = 1
}

open class One() {
    class object {
        var myContainer: Container? = Container()
    }
}

open class IntContainer(i: Int)

open class Test() {
    open fun putInt(i: Int) {
    }
    open fun test() {
        putInt(One.myContainer?.myInt!!)
        IntContainer(One.myContainer?.myInt!!)
    }
}