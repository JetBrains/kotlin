@file:InternalFileAnnotation

@InternalFunctionAnnotation
fun test() {
  foo()
  internalBar()

  @InternalClassAnnotation
  class Local {
    @InternalFunctionAnnotation
    fun foo() {}
  }
}

@InternalClassAnnotation
class Class {
  @InternalFunctionAnnotation
  fun foo() {}

  @InternalClassAnnotation
  class Nested {
    @InternalFunctionAnnotation
    fun foo() {}
  }
}
