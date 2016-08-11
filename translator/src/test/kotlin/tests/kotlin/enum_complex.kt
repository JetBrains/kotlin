
class Holder() {

    enum class nested(val id: Int) {
        FIRST(0),
        SECOND(1),
        THIRD(3);

        companion object {
            fun functor(ord: Int): Int {
                return nested.FIRST.id
            }
        }
    }
}

fun enum_complex_test1(): Int {
    val h = Holder()
    return 0
}
