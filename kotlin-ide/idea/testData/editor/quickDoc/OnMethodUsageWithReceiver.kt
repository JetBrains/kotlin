/**
Some documentation

 * @receiver Some int
 * @param b String
 * @return Return [a] and nothing else
 */
fun Int.testMethod(b: String) {

}

fun test() {
    1.<caret>testMethod("value")
}

//INFO: <div class='definition'><pre><font color="808080"><i>OnMethodUsageWithReceiver.kt</i></font><br>public fun Int.<b>testMethod</b>(
//INFO:     b: String
//INFO: ): Unit</pre></div><div class='content'><p>Some documentation</p></div><table class='sections'><tr><td valign='top' class='section'><p>Receiver:</td><td valign='top'>Some int</td><tr><td valign='top' class='section'><p>Params:</td><td valign='top'><p><code>b</code> - String</td><tr><td valign='top' class='section'><p>Returns:</td><td valign='top'>Return <a href="psi_element://a">a</a> and nothing else</td></table>
