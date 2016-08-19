class secondary_constructor_1_class(var field1: Int) {
    constructor(field1: Int, field2: Int) : this(field1) {
        this.field1 = field1 + field2
    }
}

fun secondary_constructor_1(x: Int): Int {
    val cls = secondary_constructor_1_class(12, x)
    return cls.field1
}