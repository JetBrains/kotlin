fun testing() {
    SomeClassWithParen("param", 1<caret>)
}

//INFO: <div class='definition'><pre><a href="psi_element://SomeClassWithParen"><code>SomeClassWithParen</code></a><br><i>@<a href="psi_element://org.jetbrains.annotations.Contract"><code>Contract</code></a>(pure = true)</i>&nbsp;
//INFO: public&nbsp;<b>SomeClassWithParen</b>(<a href="psi_element://java.lang.String"><code>String</code></a>&nbsp;str,
//INFO:                           int&nbsp;num)</pre></div><table class='sections'><p><tr><td valign='top' class='section'><p><i>Inferred</i> annotations:</td><td valign='top'><p><i>@<a href="psi_element://org.jetbrains.annotations.Contract">org.jetbrains.annotations.Contract</a>(pure = true)</i></td></table>
