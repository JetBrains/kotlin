// IS_APPLICABLE: true
fun foo() {
      val z = <caret>bar { it * 2 }
}

fun <T> bar(a: (Int)->T): T = a(1)