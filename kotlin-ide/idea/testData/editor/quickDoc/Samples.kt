package magic

object Samples {
    fun sampleMagic() {
        castTextSpell("[asd] [dse] [asz]")
    }
}

fun sampleScroll() {
    val reader = Scroll("[asd] [dse] [asz]").reader()
    castTextSpell(reader.readAll())
}

/**
 * @sample Samples.sampleMagic
 * @sample sampleScroll
 */
fun <caret>castTextSpell(spell: String) {
    throw SecurityException("Magic prohibited outside Hogwarts")
}

//INFO: <div class='definition'><pre><a href="psi_element://magic"><code>magic</code></a> <font color="808080"><i>Samples.kt</i></font><br>public fun <b>castTextSpell</b>(
//INFO:     spell: String
//INFO: ): Unit</pre></div><div class='content'></div><table class='sections'><tr><td valign='top' class='section'><p>Samples:</td><td valign='top'><p><a href="psi_element://Samples.sampleMagic"><code>Samples.sampleMagic</code></a><pre><code>
//INFO: castTextSpell("[asd] [dse] [asz]")
//INFO: </code></pre><p><a href="psi_element://sampleScroll"><code>sampleScroll</code></a><pre><code>
//INFO: val reader = Scroll("[asd] [dse] [asz]").reader()
//INFO: castTextSpell(reader.readAll())
//INFO: </code></pre></td></table>
