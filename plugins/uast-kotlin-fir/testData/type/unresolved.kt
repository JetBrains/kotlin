fun foo(x: Unresolved?) {
    x?.prop
}

class A(
    val prop: Unresolved
) : UnresolvedBase(prop) {

   override fun bar() : UnresolvedBase {
   }

}
