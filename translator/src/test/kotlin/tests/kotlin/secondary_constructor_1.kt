class secondary_constructor_1_class(var field1: Int) {
    constructor(field1: Int, field2: Int) : this(field1) {
        this.field1 = field1 + field2
    }
}

fun secondary_constructor_1(x: Int): Int {
    val cls = secondary_constructor_1_class(12, x)
    return cls.field1
}

class secondary_constructor_1_class2() {
    var field1: IntArray

    init {
        field1 = IntArray(0)
    }

    constructor(x: Int) : this() {
        this.field1 = this.field1.plus(x)
    }
}

fun secondary_constructor_1_arrays(): Int {
    val cls = secondary_constructor_1_class2(56)
    println(cls.field1)
    return cls.field1.size + cls.field1[0]
}
