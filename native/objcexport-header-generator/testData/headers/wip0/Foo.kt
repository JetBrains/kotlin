//class Foo {
//    companion object {
//        public inline val Int.days: Foo get() = error("")
//        public inline val Long.days: Foo get() = error("")
//        public inline val Double.days: Foo get() = error("")
//    }
//}

class Foo {
    fun String.days() = this
    fun Int.days() = this
    fun Boolean.days() = this
}