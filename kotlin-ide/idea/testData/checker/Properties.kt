 var x : Int = 1 + <error>x</error>
   get() : Int = 1
   set(value : <error>Long</error>) {
      field = value.toInt()
      field = <error>1.toLong()</error>
    }

 val xx : Int = <error>1 + x</error>
   get() : Int = 1
   <error>set(value : <error>Long</error>) {}</error>

  val p : Int = <error>1</error>
    get() = 1

class Test() {
    var a : Int = 111
    var b : Int get() = a; set(x) { a = x }

   init {

   }
   fun f() {

   }
}
