internal class C() {

    constructor(p: Int) : this() {
        println(staticField1 + C.staticField2)
    }

    companion object {
        private const val staticField1 = 0
        private const val staticField2 = 0
    }
}