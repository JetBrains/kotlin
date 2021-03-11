package server

import some.TopLevelMethodFromJava_DataKt

class Testing {
    void test() {
        TopLevelMethodFromJava_DataKt.<caret>foo(12);
    }
}

//INFO: <div class='definition'><pre><a href="psi_element://some"><code>some</code></a> <font color="808080"><i>TopLevelMethodFromJava_Data.kt</i></font><br>public fun <b>foo</b>(
//INFO:     bar: Int
//INFO: ): Unit</pre></div><div class='content'><p>KDoc foo</p></div><table class='sections'></table>
