annotation class Ann(
    val b: Byte,
    val s: Short,
    val i: Int,
    val l: Long,
    val ss: String,
    val c: Char,
    val arr: IntArray,
    val some: SomeEnum
)

enum class SomeEnum {
    A, B
}

@Ann(1, 2, 3, 4, "hello", 'c', {5, 6, 7}, SomeEnum.A)
class Base
