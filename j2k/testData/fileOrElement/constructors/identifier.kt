// ERROR: 'public fun Identifier(name: kotlin.String, isNullable: kotlin.Boolean): Identifier' is already defined in root package
// ERROR: 'public constructor Identifier(name: kotlin.String, myHasDollar: kotlin.Boolean)' is already defined in root package
// ERROR: Overload resolution ambiguity:  public fun Identifier(name: kotlin.String, isNullable: kotlin.Boolean): Identifier defined in root package public constructor Identifier(name: kotlin.String, myHasDollar: kotlin.Boolean) defined in Identifier
// ERROR: Overload resolution ambiguity:  public fun Identifier(name: kotlin.String, isNullable: kotlin.Boolean): Identifier defined in root package public constructor Identifier(name: kotlin.String, myHasDollar: kotlin.Boolean) defined in Identifier
// ERROR: Overload resolution ambiguity:  public fun Identifier(name: kotlin.String, isNullable: kotlin.Boolean): Identifier defined in root package public constructor Identifier(name: kotlin.String, myHasDollar: kotlin.Boolean) defined in Identifier
// ERROR: Overload resolution ambiguity:  public fun Identifier(name: kotlin.String, isNullable: kotlin.Boolean): Identifier defined in root package public constructor Identifier(name: kotlin.String, myHasDollar: kotlin.Boolean) defined in Identifier
public fun Identifier(name: String): Identifier {
    return Identifier(name, false)
}

public fun Identifier(name: String, isNullable: Boolean): Identifier {
    val __ = Identifier(name, false)
    __.myNullable = isNullable
    return __
}

public fun Identifier(name: String, hasDollar: Boolean, isNullable: Boolean): Identifier {
    val __ = Identifier(name, hasDollar)
    __.myNullable = isNullable
    return __
}

public class Identifier(public val name: String, private val myHasDollar: Boolean) {
    private var myNullable = true
}

public class User {
    default object {
        public fun main() {
            val i1 = Identifier("name", false, true)
            val i2 = Identifier("name", false)
            val i3 = Identifier("name")
        }
    }
}