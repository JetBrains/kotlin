package foo

object A {
  val query = object {val status = "complete"}
}

object B {
  private val ov = "d"
  val query = object {val status = "complete" + ov}
}

class C {
  fun ov() = "d"
  val query = object {val status = "complete" + ov()}
}

fun box() = A.query.status == "complete" && B.query.status == "completed" && C().query.status == "completed"

