open class Base(nested: Base.Nested) {

    class Nested(p: Int) {
        default object {

            public val FIELD: Int = 0
        }
    }
}

class Derived : Base(Base.Nested(Base.Nested.FIELD))