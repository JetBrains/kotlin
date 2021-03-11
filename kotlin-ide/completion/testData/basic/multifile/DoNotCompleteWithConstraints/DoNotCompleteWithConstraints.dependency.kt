package second

open class SomeOther() {}

class SomeTest<T> {
  fun testingMethod() {

  }
}

fun <U : SomeOther> SomeTest<U>.testingUnexpectedFunction() {
}

fun <W : Any> SomeTest<W>.testingExpectedFunction(i : Int) : String {
  return ""
}