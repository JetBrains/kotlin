internal open class Base(nested: Nested) {

    internal class Nested(p: Int) {
        companion object {

            val FIELD = 0
        }
    }
}

internal class Derived : Base(Base.Nested(Base.Nested.FIELD))