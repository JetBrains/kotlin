import test.ToBeImportedJava
import test.ToBeImportedJava.TO_BE_IMPORTED_CONST
import test.ToBeImportedJava.staticMethod
import test.ToBeImportedKotlin
import java.util.ArrayList
import java.util.HashMap


class Target {
    var listOfPlatformType: List<String> = ArrayList()

    var unresolved: UnresolvedInterface<UnresolvedGeneric?>? = UnresolvedImplementation() // Should not add import


    var hashMapOfNotImported: Map<ToBeImportedJava, ToBeImportedKotlin> = HashMap()

    internal fun acceptKotlinClass(tbi: ToBeImportedKotlin?) {}

    internal fun acceptJavaClass(tbi: ToBeImportedJava?) {}

    var ambiguousKotlin: IAmbiguousKotlin? = AmbiguousKotlin() // Should not add import in case of 2 declarations in Kotlin

    var ambiguous: IAmbiguous? = Ambiguous() // Should not add import in case of ambiguous declarations in Kotlin and in Java

    var ambiguousJava: IAmbiguousJava? = AmbiguousJava()  // Should not add import in case of 2 declarations in Java


    internal fun workWithStatics() {
        val a = TO_BE_IMPORTED_CONST
        staticMethod()
    }
}