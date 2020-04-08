// OUT_OF_CODE_BLOCK: FALSE

data class Test(val a : Int, val b : Int)

fun a() {
  val (test, <caret>other) = Test(11, 12)
}

