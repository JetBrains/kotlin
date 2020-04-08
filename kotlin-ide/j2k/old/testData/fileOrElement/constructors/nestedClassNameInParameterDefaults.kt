internal class A @JvmOverloads constructor(nested: Nested = Nested(Nested.FIELD)) {

    internal class Nested(p: Int) {
        companion object {

            val FIELD = 0
        }
    }
}