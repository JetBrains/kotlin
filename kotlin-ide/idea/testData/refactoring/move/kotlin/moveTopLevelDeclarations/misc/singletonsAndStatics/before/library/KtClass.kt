package library

class KtClass {
    class Inner {
        companion object {
            fun foo(): Int = 1
        }
    }

    companion object {
        fun foo(): Int = 1
    }
}

object KtObject {
    class Inner {
        companion object {
            fun foo(): Int = 1
        }
    }

    fun foo(): Int = 1
}