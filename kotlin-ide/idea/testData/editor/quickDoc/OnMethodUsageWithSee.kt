/**
 * @see C
 * @see D
 * @see <a href="https://kotl.in">kotlin</a>
 */
fun testMethod() {

}

class C {
}

class D {
}

fun test() {
    <caret>testMethod(1, "value")
}

//INFO: <div class='definition'><pre><font color="808080"><i>OnMethodUsageWithSee.kt</i></font><br>public fun <b>testMethod</b>(): Unit</pre></div><div class='content'></div><table class='sections'><tr><td valign='top' class='section'><p>See Also:</td><td valign='top'><a href="psi_element://C"><code>C</code></a>, <a href="psi_element://D"><code>D</code></a>, <a href="https://kotl.in">kotlin</a></td></table>
