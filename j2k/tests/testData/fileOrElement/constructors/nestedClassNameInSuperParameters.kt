// ERROR: This type is final, so it cannot be inherited from
class Base(nested: Base.Nested) {

    class Nested(p: Int) {
        class object {

            public val FIELD: Int = 0
        }
    }
}

class Derived : Base(Base.Nested(Base.Nested.FIELD))