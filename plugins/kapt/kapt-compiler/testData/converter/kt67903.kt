value class Data(val value: Int)

fun Data.foo() {}

val Data.bar: Int
    get() = 0