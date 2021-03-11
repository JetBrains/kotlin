// "Add function to supertype..." "true"
interface A {}
interface B {}
class C: A, B {
  <caret>override fun foo() {}
}