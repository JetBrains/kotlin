package foo

object A {
  object query {val status = "complete"}
}

object B {
  private val ov = "d"
  object query {val status = "complete" + ov}
}

class C {
  fun ov() = "d"
  object query {val status = "complete" + ov()}
}

fun box() = A.query.status == "complete" && B.query.status == "completed" && C().query.status == "completed"

