fun foo() {
    listOf(1).forEach {
        println(it<caret>)
    }
}

//INFO: <div class='definition'><pre>value-parameter <b>it</b>: Int</pre></div>
