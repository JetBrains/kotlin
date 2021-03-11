package skipSimpleGetter

val top = 1

fun main(args: Array<String>) {
    val a = A(1)
    //Breakpoint!
    a.a1 + a.a2 + a.a3 + a.a4 + top
}

class A(val a4: Int) {
    // only init
    val a1 = 1

    // simple get, expression body
    val a2: Int = 1
        get() = field

    // simple get, block body
    val a3: Int = 1
        get() {
            return field
        }
}

// STEP_INTO: 3
// SKIP_GETTERS: true