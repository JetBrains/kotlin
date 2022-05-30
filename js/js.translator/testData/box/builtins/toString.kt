// EXPECTED_REACHABLE_NODES: 1288
package foo

class A {
    override fun toString() = "42"
}

class B

// KT-52553
interface Parser
abstract class AbstractNamedParser(val name: String): Parser {
    override fun toString(): String = "$name ${super<Parser>.toString()}"
}
abstract class PredicateTokenParser(name: String): Parser, AbstractNamedParser(name) {
}
data class TokenEqualityParser(val expected: Int): PredicateTokenParser("$expected") {
    override fun toString(): String = super.toString()
}

fun box(): String {
    assertEquals(A().toString(), "42")
    assertEquals(B().toString(), "[object Object]")
    assertEquals(js("\"\"").toString(), "")
    assertEquals(js("123").toString(), "123")
    assertEquals(123.toString(), "123")
    assertEquals("123".toString(), "123")
    assertEquals((123 as Any).toString(), "123")
    assertEquals(null.toString(),  "null")
    assertEquals(TokenEqualityParser(2).toString(), "2 [object Object]")
    return "OK"
}
