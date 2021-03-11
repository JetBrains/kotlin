fun demo() {
  val abc = 1
  val a = ""
  val asd = 1
  val bar = 5
  fun map(<warning>f</warning> : () -> Any?) : Int  = 1
  fun buzz(<warning>f</warning> : () -> Any?) : Int  = 1
  val sdf = 1
  val foo = 3;
    use("$abc")
    use("$")
    use("$.$.asdf$\t")
    use("asd\$")
    use("asd$a<error>\x</error>")
    use("asd$a$asd$ $<error>xxx</error>")
    use("fosdfasdo${1 + bar + 100}}sdsdfgdsfsdf")
    use("foo${bar + map {foo}}sdfsdf")
    use("foo${bar + map { "foo" }}sdfsdf")
    use("foo${bar + map {
      "foo$sdf${ buzz{}}" }}sdfsdf")
}

fun use(<warning>s</warning>: String) {}