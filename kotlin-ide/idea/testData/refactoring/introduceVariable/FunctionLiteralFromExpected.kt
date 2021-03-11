class A {
  fun foo() = 1
}
fun apply(x: (A) -> Int) = 2
fun test() {
  apply<selection>{it.foo()}</selection>
}