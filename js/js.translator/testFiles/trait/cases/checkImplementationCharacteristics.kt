package foo

trait A {
  fun foo() {}
}
trait B : A {
  fun boo() {}
}

native fun eval(code: String): Any = noImpl
native val undefined: Any = noImpl
native class Function(vararg args: String)

val hasProp = Function("obj, prop", "return obj[prop] !== undefined") as ((Any, String)->Boolean)
val PREFIX = "Kotlin.modules.JS_TESTS.foo"

fun box(): String {
  val a = object: A {}
  val b = object: B {}

  if (!hasProp(a, "foo")) return "A hasn't foo"
  if (hasProp(a, "boo")) return "A has boo"

  if (!hasProp(b, "foo")) return "B hasn't foo"
  if (!hasProp(b, "boo")) return "B hasn't boo"

  if (eval("$PREFIX.A === $PREFIX.B") as Boolean) return "A and B refer to the same object"

  return "OK"
}