class Outer {
    class Middle {
        class Inner(val outer: Outer) {
            constructor() : this(Outer())
        }
    }
}

class Middle {
    fun test() {
        val i = Outer.Middle.Inner<caret>(Outer())
        val b = Outer.Middle.Inner()
    }
}
