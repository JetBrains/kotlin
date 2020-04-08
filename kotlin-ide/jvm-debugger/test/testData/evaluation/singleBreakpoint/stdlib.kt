package stdlib

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

// EXPRESSION: arrayOf(100, 101)
// RESULT: instance of java.lang.Integer[2] (id=ID): [Ljava/lang/Integer;

// EXPRESSION: arrayOf("a", "b", "c")
// RESULT: instance of java.lang.String[3] (id=ID): [Ljava/lang/String;

// EXPRESSION: intArrayOf(1, 2)
// RESULT: instance of int[2] (id=ID): [I

// EXPRESSION: String::class.java
// RESULT: instance of java.lang.Class(reflected class=java.lang.String, id=ID): Ljava/lang/Class;

// EXPRESSION: Int::class.java
// RESULT: instance of java.lang.Class(reflected class=int, id=ID): Ljava/lang/Class;

// EXPRESSION: 100.toInt()
// RESULT: 100: I

// EXPRESSION: 100.toLong()
// RESULT: 100: J

// EXPRESSION: args.sortedBy { it }.size
// RESULT: 0: I