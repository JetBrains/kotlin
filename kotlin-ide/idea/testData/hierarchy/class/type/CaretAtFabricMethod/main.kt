fun foo() {
  A<caret>()
}

class A (x: Int)

fun A(): A = A(1)


