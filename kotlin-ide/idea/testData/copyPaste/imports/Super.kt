package a

open class A {
  open fun f(): Int = 3
}

class B(): A() {
  override fun f() = <selection>super.f() + 2</selection>
}