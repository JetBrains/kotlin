// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages

data class A(val <caret>n: Int, val s: String)

data class B(val x: Int, val y: Int)

fun condition(a: A) = true

fun x(a: A, b: Boolean, list: List<Int>) {
    val (x, y) = if (condition(a)) {
        print(A(1, "").toString())
        B(1, 2)
    }
    else {
        B(3, 4)
    }

    val (x1, y1) = if (b) {
        A(1, "").apply { val (x2, y2) = this }
    }
    else {
        return
    }

    if (list.any { it == 0 }) {
        A(1, "")
    }
}

fun y1(a: A) = condition(a)
fun y2(a: A): Boolean = condition(a)
fun y3(a: A) {
    condition(a)
}
