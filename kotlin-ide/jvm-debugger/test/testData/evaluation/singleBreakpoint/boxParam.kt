package boxParam

fun main(args: Array<String>) {
    val nullableInt: Int? = 1
    val nullableByte: Byte? = 1

    // EXPRESSION: nullableInt?.plus(1)
    // RESULT: instance of java.lang.Integer(id=ID): Ljava/lang/Integer;

    // EXPRESSION: nullableByte?.plus(1)
    // RESULT: instance of java.lang.Integer(id=ID): Ljava/lang/Integer;
    //Breakpoint!
    val i = 1
}