fun <T> checkSubtype(t: T) = t

class A(val a:Int) {

      inner class B() {
        val x = checkSubtype<B>(this@B)
        val y = checkSubtype<A>(this@A)
        val z = checkSubtype<B>(this)
        val Int.xx : Int get() = checkSubtype<Int>(this)
        fun Byte.xx() : Double.() -> Unit {
          checkSubtype<Byte>(this)
          val <warning>a</warning>: Double.() -> Unit = { checkSubtype<Double>(this) + checkSubtype<Byte>(this@xx) }
          val <warning>b</warning>: Double.() -> Unit = a@{checkSubtype<Double>(this@a) + checkSubtype<Byte>(this@xx) }
          val <warning>c</warning> = a@{<error>this@a</error> <error>+</error> checkSubtype<Byte>(this@xx) }
          return (a@{checkSubtype<Double>(this@a) + checkSubtype<Byte>(this@xx) })
        }
      }
    }
