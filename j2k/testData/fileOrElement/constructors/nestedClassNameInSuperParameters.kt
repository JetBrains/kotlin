internal open class Base internal constructor(nested: Base.Nested) {

    internal class Nested internal constructor(p: Int) {
        companion object {

            val FIELD: Int = 0
        }
    }
}

internal class Derived internal constructor() : Base(Base.Nested(Base.Nested.FIELD))