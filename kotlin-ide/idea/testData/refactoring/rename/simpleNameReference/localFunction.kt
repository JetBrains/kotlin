class X {
    fun fn() : Iterator<String> = object : Iterator<String> {
        override fun next(): String {
            dddd()
            return ""
        }

        override fun hasNext(): Boolean {
            return false
        }

        fun dd<caret>dd() { // try renaming this function

        }
    }
}