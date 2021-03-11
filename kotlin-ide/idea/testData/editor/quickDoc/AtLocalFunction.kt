fun context() {
    fun local() {

    }

    <caret>local()
}

//INFO: <div class='definition'><pre>local final fun <b>local</b>(): Unit</pre></div>
