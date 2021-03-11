package collections

fun main(args: Array<String>) {
    val ar = intArrayOf(1, 2, 100, 200)
    //Breakpoint!
    args.size
}

// EXPRESSION: arrayListOf(1, 2).map { it.toString() }
// RESULT: instance of java.util.ArrayList(id=ID): Ljava/util/ArrayList;

// EXPRESSION: arrayListOf(1, 2, 101, 102).filter { it > 100 }
// RESULT: instance of java.util.ArrayList(id=ID): Ljava/util/ArrayList;

// EXPRESSION: arrayListOf(1, 2).max()
// RESULT: instance of java.lang.Integer(id=ID): Ljava/lang/Integer;

// EXPRESSION: arrayListOf(1, 2).count()
// RESULT: 2: I

// EXPRESSION: arrayListOf(1, 2).size
// RESULT: 2: I

// EXPRESSION: arrayListOf(1, 2, 3).drop(1)
// RESULT: instance of java.util.ArrayList(id=ID): Ljava/util/ArrayList;
