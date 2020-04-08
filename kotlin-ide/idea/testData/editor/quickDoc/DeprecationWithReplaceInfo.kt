@Deprecated("lol no more mainstream", replaceWith = ReplaceWith(expression = "kek()"))
fun <caret>lol() {
    println("lol")
}

//INFO: <div class='definition'><pre><font color="808080"><i>DeprecationWithReplaceInfo.kt</i></font><br>@<a href="psi_element://kotlin.Deprecated">Deprecated</a>(message = "lol no more mainstream", replaceWith = <a href="psi_element://kotlin.ReplaceWith">ReplaceWith</a>(expression = "kek()", imports = {}))
//INFO: public fun <b>lol</b>(): Unit</pre></div><table class='sections'><tr><td valign='top' class='section'><p>Deprecated:</td><td valign='top'>lol no more mainstream</td><tr><td valign='top' class='section'><p>Replace with:</td><td valign='top'><code>kek()</code></td></table></pre></div><table class='sections'><p></table>
