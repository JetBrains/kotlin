package fieldVariable

class Foo {
    val a: String? = "field"
        get() {
            //Breakpoint!
            val a = 5
            return "not a " + field
        }

    val b: String?
        get() {
            //Breakpoint!
            return "b"
        }
}

fun main() {
    Foo().a
    Foo().b
}

// EXPRESSION: field
// RESULT: "field": Ljava/lang/String;

// EXPRESSION: field
// RESULT: Cannot find the backing field 'b'