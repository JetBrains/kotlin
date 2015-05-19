/**
 * NOTE: this test originally checked that values of classes inheriting from functions could be invoked as functions.
 * However, Function{n} / ExtensionFunction{n} classes were incompatible with JS functions our lambdas were compiled to.
 * This led to runtime errors (see KT-7692), so the test is temporarily disabled.
 *
 * TODO: support inheritance from function types and re-enable this test
 */

package foo

class Bar/* : Function0<String>*/ {
    fun invoke() = "Bar.invoke()"
}

class Baz/* : Function2<Int, Boolean, String>*/ {
    fun invoke(i: Int, b: Boolean) = "Baz.invoke($i, $b)"
}

class ExtBar/* : ExtensionFunction0<String, String>*/ {
    fun String.invoke() = "ExtBar.invoke($this)"
}

class ExtBaz/* : ExtensionFunction2<String, Int, Boolean, String>*/ {
    fun String.invoke(i: Int, b: Boolean) = "ExtBaz.invoke($this, $i, $b)"
}

class Mixed/* :
        Function1<Int, String>,
        Function2<Int, Boolean, String>,
        ExtensionFunction1<Int, Boolean, String>,
        ExtensionFunction2<Int, Int, Boolean, String>*/
{
    fun invoke(i: Int) = "Mixed.invoke($i)"
    fun invoke(i: Int, b: Boolean) = "Mixed.invoke($i, $b)"
    fun Int.invoke(b: Boolean) = "ext Mixed.invoke($this, $b)"
    fun Int.invoke(i: Int, b: Boolean) = "ext Mixed.invoke($this, $i, $b)"
}

fun box(): String {
    val bar = Bar()
    val baz = Baz()
    val extBar = ExtBar()
    val extBaz = ExtBaz()
    val mixed = Mixed()

    assertEquals("Bar.invoke()", bar())
    assertEquals("Baz.invoke(2, false)", baz(2, false))
    assertEquals("ExtBar.invoke(2e2)", "2e2".extBar())
    assertEquals("ExtBaz.invoke(29, 34, true)", "29".extBaz(34, true))

    assertEquals("Mixed.invoke(45)", mixed(45))
    assertEquals("Mixed.invoke(552, true)", mixed(552, true))
    assertEquals("ext Mixed.invoke(21, true)", 21.mixed(true))
    assertEquals("ext Mixed.invoke(29, 304, false)", 29.mixed(304, false))

    return "OK"
}
