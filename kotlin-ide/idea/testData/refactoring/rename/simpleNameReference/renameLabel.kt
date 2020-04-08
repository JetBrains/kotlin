class AA {
    class B {
        fun Int.ba<caret>r() {
            val a = this@AA
            val b = this@B

            val c = this
            val c1 = this@bar
        }
    }
}