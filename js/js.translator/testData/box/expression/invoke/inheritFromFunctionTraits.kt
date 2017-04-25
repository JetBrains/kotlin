// EXPECTED_REACHABLE_NODES: 504
/**
 * NOTE: this test originally checked that values of classes inheriting from functions could be invoked as functions.
 * However, Function{n} / ExtensionFunction{n} classes were incompatible with JS functions our lambdas were compiled to.
 * This led to runtime errors (see KT-7692), so the test is temporarily disabled.
 *
 * TODO: support inheritance from function types and re-enable this test
 * NOTE: inheritance from extension function is forbidden now
 */

package foo

class Bar/* : Function0<String>*/ {
    operator fun invoke() = "Bar.invoke()"
}

class Baz/* : Function2<Int, Boolean, String>*/ {
    operator fun invoke(i: Int, b: Boolean) = "Baz.invoke($i, $b)"
}

class Mixed/* :
        Function1<Int, String>,
        Function2<Int, Boolean, String>*/
{
    operator fun invoke(i: Int) = "Mixed.invoke($i)"
    operator fun invoke(i: Int, b: Boolean) = "Mixed.invoke($i, $b)"
}

fun box(): String {
    val bar = Bar()
    val baz = Baz()
    val mixed = Mixed()

    assertEquals("Bar.invoke()", bar())
    assertEquals("Baz.invoke(2, false)", baz(2, false))

    assertEquals("Mixed.invoke(45)", mixed(45))
    assertEquals("Mixed.invoke(552, true)", mixed(552, true))

    return "OK"
}
