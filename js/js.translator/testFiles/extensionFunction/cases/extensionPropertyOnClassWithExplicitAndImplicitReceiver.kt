package foo

class Foo {

  fun blah(): Int {
    return 6
  }

}

val Foo.fooImp : String
  get() {
    return "implProp" + blah()
  }

val Foo.fooExp() : String
  get() {
    return "explProp" + this.blah()
  }

fun box() : Boolean {
    var a = Foo()
    if (a.fooImp != "implProp6") return false
    if (a.fooExp != "explProp6") return false
    return true;
}