import Outer.Middle.Inner as F

class Outer {
    class Middle {
        class Inner {
            companion object {
                const val SIZE = 1
            }
        }
    }
}

class Middle {
    fun test() {
        val i = Outer.Middle.Inner<caret>.SIZE
    }
}
