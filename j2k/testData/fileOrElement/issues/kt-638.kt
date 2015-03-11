// ERROR: 'public fun <T> Identifier(name: T, isNullable: kotlin.Boolean): Identifier<T>' is already defined in root package
// ERROR: 'public constructor Identifier<T>(name: T, myHasDollar: kotlin.Boolean)' is already defined in root package
// ERROR: Cannot choose among the following candidates without completing type inference:  public fun <T> Identifier(name: T, isNullable: kotlin.Boolean): Identifier<T> defined in root package public constructor Identifier<T>(name: T, myHasDollar: kotlin.Boolean) defined in Identifier
// ERROR: Cannot choose among the following candidates without completing type inference:  public fun <T> Identifier(name: T, isNullable: kotlin.Boolean): Identifier<T> defined in root package public constructor Identifier<T>(name: T, myHasDollar: kotlin.Boolean) defined in Identifier
// ERROR: Cannot choose among the following candidates without completing type inference:  public fun <T> Identifier(name: T, isNullable: kotlin.Boolean): Identifier<T> defined in root package public constructor Identifier<T>(name: T, myHasDollar: kotlin.Boolean) defined in Identifier
// ERROR: Overload resolution ambiguity:  public fun <T> Identifier(name: kotlin.String, isNullable: kotlin.Boolean): Identifier<kotlin.String> defined in root package public constructor Identifier<T>(name: kotlin.String, myHasDollar: kotlin.Boolean) defined in Identifier
public fun <T> Identifier(name: T): Identifier<T> {
    return Identifier(name, false)
}

public fun <T> Identifier(name: T, isNullable: Boolean): Identifier<T> {
    val __ = Identifier(name, false)
    __.myNullable = isNullable
    return __
}

public fun <T> Identifier(name: T, hasDollar: Boolean, isNullable: Boolean): Identifier<T> {
    val __ = Identifier(name, hasDollar)
    __.myNullable = isNullable
    return __
}

public class Identifier<T>(public val name: T, private val myHasDollar: Boolean) {
    private var myNullable = true
}

public class User {
    default object {
        public fun main(args: Array<String>) {
            val i1 = Identifier("name", false, true)
            val i2 = Identifier<String>("name", false)
            val i3 = Identifier("name")
        }
    }
}

fun main(args: Array<String>) = User.main(args)