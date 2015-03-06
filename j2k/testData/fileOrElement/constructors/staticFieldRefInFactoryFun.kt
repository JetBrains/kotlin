class C() {

    constructor(p: Int) : this() {
        System.out.println(staticField1 + C.staticField2)
    }

    default object {
        private val staticField1 = 0
        private val staticField2 = 0
    }
}
