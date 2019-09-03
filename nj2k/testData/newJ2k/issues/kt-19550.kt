class TestToStringReturnsNullable {
    open class Base {
        var string: String? = null
    }

    open class Ctor(string: String) : Base() {
        init {
            this.string = string
        }
    }

    class Derived(string: String) : Ctor(string) {
        override fun toString(): String {
            return string!!
        }
    }
}