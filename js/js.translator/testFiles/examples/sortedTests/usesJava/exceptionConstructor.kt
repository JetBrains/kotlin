class GameError(msg: String): Exception(msg) {
}

fun box(): String {
  val e = GameError("foo")
  return if (e.getMessage() == "foo") "OK" else "fail"
}
