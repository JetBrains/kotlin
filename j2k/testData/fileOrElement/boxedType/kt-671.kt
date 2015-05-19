// ERROR: None of the following functions can be called with the arguments supplied:  public open fun valueOf(p0: kotlin.Short): kotlin.Short! defined in java.lang.Short public open fun valueOf(p0: kotlin.String!): kotlin.Short! defined in java.lang.Short public open fun valueOf(p0: kotlin.String!, p1: kotlin.Int): kotlin.Short! defined in java.lang.Short
package demo

class Test {
    fun test() {
        val i = Integer.valueOf(100)
        val s = 3
        val ss = java.lang.Short.valueOf(s)
    }
}