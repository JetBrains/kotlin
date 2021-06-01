fun foo(x: Unresolved) {
}

class A(
    val prop: Unresolved
) : UnresolvedBase(prop) {

   override fun bar() : UnresolvedBase {
   }

}
