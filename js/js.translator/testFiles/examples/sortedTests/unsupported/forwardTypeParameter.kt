class Foo() { }
class Bar() { }

fun isInstance<T>(obj: Any?) = obj is T

fun isInstance2<T>(obj: Any?) = isInstance<T>(obj)

fun box(): String {
  if (! isInstance2<Foo>(Foo())) return "fail 1"
  if (isInstance2<Bar>(Foo())) return "fail 2"
  return "OK"
}
