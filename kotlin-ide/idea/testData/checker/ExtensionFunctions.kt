fun Int?.optint() : Unit {}
val Int?.optval : Unit get() = Unit

fun <T: Any, E> T.foo(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning> : E, y : A) : T   {
  y.plus(1)
  y plus 1
  y + 1.0

  this<warning descr="[UNNECESSARY_SAFE_CALL] Unnecessary safe call on a non-null receiver of type T">?.</warning>minus<T>(this)

  return this
}

class A

infix operator fun A.plus(<warning descr="[UNUSED_PARAMETER] Parameter 'a' is never used">a</warning> : Any) {

  1.foo()
  true.<error descr="[NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER] Not enough information to infer type variable E">foo</error>(<error descr="[NO_VALUE_FOR_PARAMETER] No value passed for parameter 'x'"><error descr="[NO_VALUE_FOR_PARAMETER] No value passed for parameter 'y'">)</error></error>

  <warning descr="[UNUSED_EXPRESSION] The expression is unused">1</warning>
}

infix operator fun A.plus(<warning descr="[UNUSED_PARAMETER] Parameter 'a' is never used">a</warning> : Int) {
  <warning descr="[UNUSED_EXPRESSION] The expression is unused">1</warning>
}

fun <T> T.minus(<warning descr="[UNUSED_PARAMETER] Parameter 't' is never used">t</warning> : T) : Int = 1

fun test() {
  val <warning descr="[UNUSED_VARIABLE] Variable 'y' is never used">y</warning> = 1.abs
}
val Int.abs : Int
  get() = if (this > 0) this else -this;

<error descr="[EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT] Extension property must have accessors or be abstract">val <T> T.foo : T</error>

fun Int.foo() = this

// FILE: b.kt
//package null_safety

        fun parse(<warning descr="[UNUSED_PARAMETER] Parameter 'cmd' is never used">cmd</warning>: String): Command? { return null  }
        class Command() {
        //  fun equals(other : Any?) : Boolean
          val foo : Int = 0
        }

        <error descr="[INAPPLICABLE_OPERATOR_MODIFIER] 'operator' modifier is inapplicable on this function: must be a member function">operator</error> fun Any.<warning descr="[EXTENSION_SHADOWED_BY_MEMBER] Extension is shadowed by a member: public open operator fun equals(other: Any?): Boolean">equals</warning>(<warning descr="[UNUSED_PARAMETER] Parameter 'other' is never used">other</warning> : Any?) : Boolean = true
        fun Any?.equals1(<warning descr="[UNUSED_PARAMETER] Parameter 'other' is never used">other</warning> : Any?) : Boolean = true
        fun Any.equals2(<warning descr="[UNUSED_PARAMETER] Parameter 'other' is never used">other</warning> : Any?) : Boolean = true

        fun main(<warning descr="[UNUSED_PARAMETER] Parameter 'args' is never used">args</warning>: Array<String>) {

            System.out.print(1)

            val command = parse("")

            command.foo

            command<error descr="[UNSAFE_CALL] Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Command?">.</error>equals(null)
            command?.equals(null)
            command.equals1(null)
            command?.equals1(null)

            val c = Command()
            c<warning descr="[UNNECESSARY_SAFE_CALL] Unnecessary safe call on a non-null receiver of type Command">?.</warning>equals2(null)

            if (command == null) <warning descr="[UNUSED_EXPRESSION] The expression is unused">1</warning>
        }
