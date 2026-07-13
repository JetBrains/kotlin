// ISSUE: KT-86985
// WITH_STDLIB

annotation class AllOpen

@AllOpen
class MyEntity(
    <!INAPPLICABLE_JVM_NAME!>@get:JvmName("getSuperA")<!>
    val a: String,

    @get:JvmName("getSuperB")
    final val b: String,
) {
    <!INAPPLICABLE_JVM_NAME!>@get:JvmName("getSuperC")<!>
    val c: String = "OK"

    @get:JvmName("getSuperD")
    final val d: String = "OK"
}
