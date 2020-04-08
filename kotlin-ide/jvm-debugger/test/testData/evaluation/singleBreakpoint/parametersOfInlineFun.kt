// Check that evaluate expression works inside inline function
package parametersOfInlineFun

fun main(args: Array<String>) {
    val a = A(1)
    a.foo { 1 + 1 }
}

inline fun A.foo(f: (i: Int) -> Unit) {
    val primitive = 1
    val array = arrayOf(1)
    val str = "str"
    val list = listOf("str")
    //Breakpoint!
    f(1)
}

class A(val prop: Int)

// EXPRESSION: primitive
// RESULT: 1: I

// EXPRESSION: array
// RESULT: instance of java.lang.Integer[1] (id=ID): [Ljava/lang/Integer;

// EXPRESSION: str
// RESULT: "str": Ljava/lang/String;

// EXPRESSION: list
// RESULT: instance of java.util.Collections$SingletonList(id=ID): Ljava/util/Collections$SingletonList;

// EXPRESSION: this.prop
// RESULT: 1: I