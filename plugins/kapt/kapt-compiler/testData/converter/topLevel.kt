// WITH_STDLIB
// FULL_JDK
package test.another

annotation class Anno(val value: String)

fun topLevelFunction(): String? = null

fun <X : CharSequence, T : List<out X>> topLevelGenericFunction(): T? {
    return null!!
}

val topLevelProperty = 2

const val topLevelConstProperty = 2

val topLevelProperty2: String
    get() = ""

fun @receiver:Anno("rec") String.extensionFunction(@Anno("1") a: String, @Anno("2") b: String) {}

@Anno("extpr")
var <T: Any> @receiver:Anno("propRec") T.extensionProperty: String
    get() = ""
    set(@Anno("setparam") setParamName) {}
